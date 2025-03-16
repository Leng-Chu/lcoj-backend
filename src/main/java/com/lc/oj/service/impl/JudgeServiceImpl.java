package com.lc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.constant.StrategyConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.judge.JudgeStrategy;
import com.lc.oj.judge.JudgeStrategySelector;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.judge.StrategyResponse;
import com.lc.oj.model.dto.question.JudgeConfig;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.service.IJudgeService;
import com.lc.oj.service.IQuestionService;
import com.lc.oj.service.IQuestionSubmitService;
import com.lc.oj.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Slf4j
@Service
public class JudgeServiceImpl implements IJudgeService {

    @Resource
    private IQuestionSubmitService questionSubmitService;

    @Resource
    private IQuestionService questionService;

    @Resource
    private StringRedisTemplate template;

    @Resource
    private WebSocketServer webSocketServer;

    @Resource
    private JudgeStrategySelector judgeStrategySelector;

    @Override
    public void doJudge(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        Question question = questionService.getById(questionSubmit.getQuestionId());
        // 1）根据题目来源选择不同的判题策略，获取判题结果
        StrategyRequest strategyRequest = StrategyRequest.builder()
                .code(questionSubmit.getCode())
                .language(questionSubmit.getLanguage())
                .num(question.getNum())
                .judgeConfig(JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class))
                .build();
        // 2）执行判题，普通判题策略
        StrategyResponse strategyResponse;
        JudgeStrategy judgeStrategy = judgeStrategySelector.select(StrategyConstant.NORMAL);
        if (judgeStrategy == null) {
            strategyResponse = new StrategyResponse();
            strategyResponse.setJudgeResult(JudgeResultEnum.SYSTEM_ERROR.getValue());
        } else {
            strategyResponse = judgeStrategy.doJudgeWithStrategy(strategyRequest);
        }
        if (Objects.equals(strategyResponse.getJudgeResult(), JudgeResultEnum.SYSTEM_ERROR.getValue())) {
            log.info("判题系统错误，questionSubmitId:{}", questionSubmitId);
        }

        // 3）修改数据库中的判题结果
        questionSubmit.setJudgeResult(strategyResponse.getJudgeResult());
        questionSubmit.setMaxTime(strategyResponse.getMaxTime());
        questionSubmit.setMaxMemory(strategyResponse.getMaxMemory());
        questionSubmit.setCaseInfoList(JSONUtil.toJsonStr(strategyResponse.getCaseInfoList()));
        boolean update = questionSubmitService.updateById(questionSubmit);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题结果更新失败");
        }
        // 更新后删除缓存
        template.delete(RedisConstant.SUBMIT_CACHE_KEY + questionSubmit.getId());
        webSocketServer.sendToAllClient("更新提交记录: " + questionSubmit.getId());
        // 4）使用redis存储每个人通过的题目集合，删掉redis中的提交记录
        template.delete(RedisConstant.SUBMIT_LOCK_KEY + questionSubmit.getUserId());
        String acceptKey = RedisConstant.QUESTION_ACCEPT_KEY + questionSubmit.getUserName();
        String failKey = RedisConstant.QUESTION_FAIL_KEY + questionSubmit.getUserName();
        String rejudge = template.opsForValue().get(RedisConstant.SUBMIT_REJUDGE_KEY + questionSubmit.getId());
        Integer oldResult = null;
        if (rejudge != null) {
            oldResult = Integer.parseInt(rejudge);
            template.delete(RedisConstant.SUBMIT_REJUDGE_KEY + questionSubmit.getId());
        }
        if (Objects.equals(strategyResponse.getJudgeResult(), JudgeResultEnum.ACCEPTED.getValue())) {
            if (oldResult == null || !oldResult.equals(JudgeResultEnum.ACCEPTED.getValue())) {
                // 如果是第一次提交，通过数+1；重判时，如果之前没有通过，那么通过数+1
                question.setAcceptedNum(question.getAcceptedNum() + 1);
            }
            questionService.updateById(question);
            template.delete(RedisConstant.QUESTION_CACHE_KEY + question.getId());
            // 设置这道题为通过，并从失败集合中删除
            template.opsForSet().add(acceptKey, question.getId().toString());
            template.opsForSet().remove(failKey, question.getId().toString());
        } else {
            if (oldResult != null && oldResult.equals(JudgeResultEnum.ACCEPTED.getValue())) {
                // 重判时，如果之前通过了，现在没通过，那么通过数-1
                question.setAcceptedNum(question.getAcceptedNum() - 1);
                questionService.updateById(question);
                template.delete(RedisConstant.QUESTION_CACHE_KEY + question.getId());
            }
            // 如果此人之前通过了这道题，那么忽略；否则，添加到失败集合
            if (Boolean.FALSE.equals(template.opsForSet().isMember(acceptKey, question.getId().toString()))) {
                template.opsForSet().add(failKey, question.getId().toString());
            }
        }
        Long size = template.opsForSet().size(acceptKey);
        template.opsForZSet().add(RedisConstant.USER_RANK_KEY, questionSubmit.getUserName(), size == null ? 0 : size);
    }

    @Override
    public void checkSubmit(long questionSubmitId) throws Exception {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            //休眠一段时间，然后重试
            Thread.sleep(1000);
            questionSubmit = questionSubmitService.getById(questionSubmitId);
        }
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            //休眠一段时间，然后重试
            Thread.sleep(1000);
            question = questionService.getById(questionId);
        }
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
    }

    @Override
    public void createOutput(long questionNum) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("num", questionNum);
        Question question = questionService.getOne(queryWrapper);
        String strNum = ":" + questionNum;
        if (question == null) {
            webSocketServer.sendToSpecificClients("生成输出数据失败：题目不存在", strNum);
            return;
        }
        String answer = question.getAnswer();
        if (answer == null || answer.isEmpty()) {
            webSocketServer.sendToSpecificClients("生成输出数据失败：标程不能为空", strNum);
            return;
        }
        String language = question.getLanguage();
        if (!"cpp".equals(language) && !"java".equals(language) && !"python".equals(language)) {
            webSocketServer.sendToSpecificClients("生成输出数据失败：编程语言不合法", strNum);
            return;
        }
        StrategyRequest strategyRequest = StrategyRequest.builder()
                .code(answer)
                .language(language)
                .num(questionNum)
                .judgeConfig(JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class))
                .build();
        webSocketServer.sendToSpecificClients("等待生成输出数据", strNum);
        JudgeStrategy judgeStrategy = judgeStrategySelector.select(StrategyConstant.CREATE_OUTPUT);
        if (judgeStrategy == null) {
            webSocketServer.sendToSpecificClients("系统错误，生成输出数据失败", strNum);
        } else {
            StrategyResponse strategyResponse = judgeStrategy.doJudgeWithStrategy(strategyRequest);
            Integer result = strategyResponse.getJudgeResult();
            if (!Objects.equals(result, JudgeResultEnum.ACCEPTED.getValue())) {
                webSocketServer.sendToSpecificClients("生成输出数据失败：" + JudgeResultEnum.getEnumByValue(result).getText(), strNum);
            } else {
                webSocketServer.sendToSpecificClients("生成输出数据成功", strNum);
            }
        }
    }
}
