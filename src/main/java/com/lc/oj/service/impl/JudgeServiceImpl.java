package com.lc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.judge.StrategyResponse;
import com.lc.oj.model.dto.question.JudgeConfig;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.model.enums.QuestionSubmitStatusEnum;
import com.lc.oj.service.IJudgeService;
import com.lc.oj.service.IQuestionService;
import com.lc.oj.service.IQuestionSubmitService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Objects;

@Service
public class JudgeServiceImpl implements IJudgeService {

    @Resource
    private IQuestionSubmitService questionSubmitService;

    @Resource
    private IQuestionService questionService;

    @Resource
    private StringRedisTemplate template;

    @Override
    @Transactional
    public void doJudge(long questionSubmitId) {
        // 1）传入题目的提交 id，获取到对应的题目、提交信息（包含代码、编程语言等）
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交信息不存在");
        }
        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }
        // 2）如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目状态不为等待判题");
        }
        // 3）更改判题（题目提交）的状态为 “判题中”，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
        // 4）TODO 根据题目来源选择不同的判题策略，获取判题结果
        StrategyRequest strategyRequest = new StrategyRequest();
        strategyRequest.setCode(questionSubmit.getCode());
        strategyRequest.setLanguage(questionSubmit.getLanguage());
        strategyRequest.setNum(question.getNum());
        strategyRequest.setJudgeConfig(JSONUtil.toBean(question.getJudgeConfig(), JudgeConfig.class));
        // -----执行判题策略，获取strategyResponse------
        StrategyResponse strategyResponse = new StrategyResponse();
        strategyResponse.setSuccess(true);
        strategyResponse.setCaseInfoList(new ArrayList<>());
        strategyResponse.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
        // ------------------------------------------
        // 5）修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setMaxTime(strategyResponse.getMaxTime());
        questionSubmitUpdate.setMaxMemory(strategyResponse.getMaxMemory());
        questionSubmitUpdate.setJudgeResult(strategyResponse.getJudgeResult());
        questionSubmitUpdate.setCaseInfoList(JSONUtil.toJsonStr(strategyResponse.getCaseInfoList()));
        if (strategyResponse.isSuccess()) {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
        } else {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        }
        update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }

        // 使用redis存储每个人通过的题目集合
        String acceptKey = RedisConstant.QUESTION_ACCEPT_KEY + questionSubmit.getUserId();
        String failKey = RedisConstant.QUESTION_FAIL_KEY + questionSubmit.getUserId();
        if (Objects.equals(strategyResponse.getJudgeResult(), JudgeResultEnum.ACCEPTED.getValue())) {
            question.setAcceptedNum(question.getAcceptedNum() + 1);
            questionService.updateById(question);
            // 设置这道题为通过
            template.opsForSet().add(acceptKey, questionId.toString());
        } else {
            if (Boolean.FALSE.equals(template.opsForSet().isMember(acceptKey, questionId.toString()))) {
                template.opsForSet().add(failKey, questionId.toString());
            }
        }
    }
}
