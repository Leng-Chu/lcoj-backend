package com.lc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.judge.JudgeStrategy;
import com.lc.oj.judge.NormalStrategy;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.judge.StrategyResponse;
import com.lc.oj.model.dto.question.JudgeConfig;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.properties.JudgeProperties;
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
    private JudgeProperties judgeProperties;

    @Resource
    private WebSocketServer webSocketServer;

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
        JudgeStrategy judgeStrategy = new NormalStrategy(judgeProperties);
        StrategyResponse strategyResponse = judgeStrategy.doJudgeWithStrategy(strategyRequest);
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
        webSocketServer.sendToAllClient("更新提交记录: " + questionSubmit.getId());
        // 4）使用redis存储每个人通过的题目集合
        String acceptKey = RedisConstant.QUESTION_ACCEPT_KEY + questionSubmit.getUserId();
        String failKey = RedisConstant.QUESTION_FAIL_KEY + questionSubmit.getUserId();
        if (Objects.equals(strategyResponse.getJudgeResult(), JudgeResultEnum.ACCEPTED.getValue())) {
            question.setAcceptedNum(question.getAcceptedNum() + 1);
            questionService.updateById(question);
            // 设置这道题为通过
            template.opsForSet().add(acceptKey, question.getId().toString());
        } else {
            if (Boolean.FALSE.equals(template.opsForSet().isMember(acceptKey, question.getId().toString()))) {
                template.opsForSet().add(failKey, question.getId().toString());
            }
        }
    }

    @Override
    public void checkSubmit(long questionSubmitId) {
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
    }
}
