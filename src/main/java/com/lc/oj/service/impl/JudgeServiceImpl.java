package com.lc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.judge.JudgeInfo;
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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;

@Service
public class JudgeServiceImpl implements IJudgeService {

    @Resource
    private IQuestionSubmitService questionSubmitService;

    @Resource
    private IQuestionService questionService;

    @Override
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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
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
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setMemory(100L);
        judgeInfo.setTime(200L);
        judgeInfo.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
        strategyResponse.setJudgeInfo(judgeInfo);
        strategyResponse.setCaseInfoList(new ArrayList<>());
        // ------------------------------------------
        // 5）修改数据库中的判题结果
        questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        if (strategyResponse.isSuccess()) {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
            questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(strategyResponse.getJudgeInfo()));
            questionSubmitUpdate.setCaseInfoList(JSONUtil.toJsonStr(strategyResponse.getCaseInfoList()));
        } else {
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
        }
        update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "题目状态更新错误");
        }
    }
}
