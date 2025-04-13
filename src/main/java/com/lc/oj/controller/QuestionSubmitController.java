package com.lc.oj.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lc.oj.annotation.AuthCheck;
import com.lc.oj.common.BaseResponse;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.common.ResultUtils;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.constant.UserConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.message.MessageProducer;
import com.lc.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.lc.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.entity.User;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.model.vo.QuestionSubmitCountVO;
import com.lc.oj.model.vo.QuestionSubmitVO;
import com.lc.oj.service.IQuestionSubmitService;
import com.lc.oj.service.IUserService;
import com.lc.oj.websocket.WebSocketServer;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    @Resource
    private MessageProducer messageProducer;
    @Resource
    private WebSocketServer webSocketServer;
    @Resource
    private StringRedisTemplate template;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param request
     * @return 提交记录的 id
     */
    @PostMapping("/do")
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
    @PostMapping("/list/page")
    public BaseResponse<Page<QuestionSubmitVO>> listQuestionSubmitByPage(@RequestBody QuestionSubmitQueryRequest questionSubmitQueryRequest,
                                                                         HttpServletRequest request) {
        long current = questionSubmitQueryRequest.getCurrent();
        long size = questionSubmitQueryRequest.getPageSize();
        QueryWrapper<QuestionSubmit> queryWrapper = questionSubmitService.getQueryWrapper(questionSubmitQueryRequest);
        Page<QuestionSubmit> questionSubmitPage;
        if (queryWrapper == null) {
            // 如果返回null，证明没有查询条件，可以走缓存
            questionSubmitPage = questionSubmitService.getPageByCache(current, size);
        } else {
            // 从数据库中查询原始的题目提交分页信息
            queryWrapper.select("id", "language", "code", "judgeResult", "maxTime", "maxMemory", "caseInfoList", "questionId", "questionNum", "questionTitle", "userId", "userName", "createTime");
            questionSubmitPage = questionSubmitService.page(new Page<>(current, size), queryWrapper);
        }
        final User loginUser = userService.getLoginUser(request);
        // 返回脱敏信息
        return ResultUtils.success(questionSubmitService.getQuestionSubmitVOPage(questionSubmitPage, loginUser));
    }

    /**
     * 重判某个提交记录
     *
     * @param questionSubmitId
     * @return
     */
    @PostMapping("/rejudge")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> rejudge(Long questionSubmitId) {
        if (questionSubmitId == null || questionSubmitId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        template.opsForValue().set(RedisConstant.SUBMIT_REJUDGE_KEY + questionSubmitId, String.valueOf(questionSubmit.getJudgeResult()));
        UpdateWrapper<QuestionSubmit> updateWrapper = new UpdateWrapper<>();
        updateWrapper.set("judgeResult", JudgeResultEnum.WAITING.getValue())
                .set("maxTime", null)
                .set("maxMemory", null)
                .set("caseInfoList", "[]")
                .eq("id", questionSubmitId);
        boolean b = questionSubmitService.update(updateWrapper);
        if (b) {
            template.delete(RedisConstant.SUBMIT_CACHE_KEY + questionSubmitId);
            webSocketServer.sendToAllClient("更新提交记录: " + questionSubmitId);
            messageProducer.sendJudgeMessage(questionSubmitId);
        }
        return ResultUtils.success(b);
    }

    /**
     * 获取某人提交记录计数
     *
     * @param userName
     * @return
     */
    @PostMapping("/count")
    public BaseResponse<QuestionSubmitCountVO> countQuestionSubmit(String userName) {
        if (userName == null || userName.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QuestionSubmitCountVO countVO = questionSubmitService.countQuestionSubmissions(userName);
        return ResultUtils.success(countVO);
    }
}
