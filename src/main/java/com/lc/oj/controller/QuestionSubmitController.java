package com.lc.oj.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.oj.common.BaseResponse;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.common.ResultUtils;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.lc.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.entity.User;
import com.lc.oj.model.vo.QuestionSubmitVO;
import com.lc.oj.service.IQuestionSubmitService;
import com.lc.oj.service.IUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 题目提交 前端控制器
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@RestController
@RequestMapping("/question-submit")
public class QuestionSubmitController {

    @Resource
    private IUserService userService;
    @Resource
    private IQuestionSubmitService questionSubmitService;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的 id
     */
    @PostMapping("/question_submit/do")
    public BaseResponse<Long> doQuestionSubmit(@RequestBody QuestionSubmitAddRequest questionSubmitAddRequest,
                                               HttpServletRequest request) {
        if (questionSubmitAddRequest == null || questionSubmitAddRequest.getQuestionId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        final User loginUser = userService.getLoginUser(request);
        long questionSubmitId = questionSubmitService.doQuestionSubmit(questionSubmitAddRequest, loginUser);
        return ResultUtils.success(questionSubmitId);
    }

    /**
     * 分页获取题目提交列表（除了管理员外，普通用户只能看到非答案、提交代码等公开信息）
     *
     * @param questionSubmitQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/question_submit/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        // 从数据库中查询原始的题目提交分页信息
        Page<QuestionSubmit> questionSubmitPage = questionSubmitService.page(new Page<>(current, size),
                questionSubmitService.getQueryWrapper(questionSubmitQueryRequest));
        final User loginUser = userService.getLoginUser(request);
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }
}
