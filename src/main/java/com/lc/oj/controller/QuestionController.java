package com.lc.oj.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.lc.oj.annotation.AuthCheck;
import com.lc.oj.common.BaseResponse;
import com.lc.oj.common.DeleteRequest;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.common.ResultUtils;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.constant.UserConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.exception.ThrowUtils;
import com.lc.oj.model.dto.question.*;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.entity.User;
import com.lc.oj.model.enums.QuestionAcceptEnum;
import com.lc.oj.model.vo.QuestionListVO;
import com.lc.oj.model.vo.QuestionManageVO;
import com.lc.oj.model.vo.QuestionVO;
import com.lc.oj.service.IJudgeService;
import com.lc.oj.service.IQuestionService;
import com.lc.oj.service.IQuestionSubmitService;
import com.lc.oj.service.IUserService;
import com.lc.oj.utils.FileUtils;
import com.lc.oj.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 题目 前端控制器
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@RestController
@RequestMapping("/question")
@Slf4j
public class QuestionController {
    private final static Gson GSON = new Gson();
    @Resource
    private IQuestionService questionService;
    @Resource
    private IQuestionSubmitService questionSubmitService;
    @Resource
    private IUserService userService;
    @Resource
    private IJudgeService judgeService;
    @Resource
    private StringRedisTemplate template;
    @Resource
    private WebSocketServer webSocketServer;
    @Value("${lcoj.judge.data-path}")
    private String dataPath;


    /**
     * 创建（仅管理员）
     *
     * @param questionAdminAddRequest
     * @return
     */
    @PostMapping("/admin/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> adminAddQuestion(@RequestBody QuestionAdminAddRequest questionAdminAddRequest) {
        Question question = new Question();
        BeanUtils.copyProperties(questionAdminAddRequest, question);
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "插入数据库失败");
        long newQuestionId = question.getId();
        return ResultUtils.success(newQuestionId);
    }


    /**
     * 创建
     *
     * @param questionAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addQuestion(@RequestBody QuestionAddRequest questionAddRequest, HttpServletRequest request) {
        if (questionAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionAddRequest, question);
        List<String> tags = questionAddRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<SampleCase> sampleCase = questionAddRequest.getSampleCase();
        if (sampleCase != null) {
            question.setSampleCase(GSON.toJson(sampleCase));
        }
        JudgeConfig judgeConfig = questionAddRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        questionService.validQuestion(question, true);
        User loginUser = userService.getLoginUser(request);
        question.setUserId(loginUser.getId());
        question.setUserName(loginUser.getUserName());
        boolean result = questionService.save(question);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "插入数据库失败");
        long newQuestionId = question.getId();
        File dir = new File(dataPath + question.getNum());
        // 创建文件夹
        if (!dir.exists()) {
            boolean flag = dir.mkdirs();
            if (!flag) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        }
        return ResultUtils.success(newQuestionId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @Transactional
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuestion(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestion.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 删除题目
        boolean b = questionService.removeById(id);
        // 删除提交记录
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("questionId", id);
        questionSubmitService.remove(queryWrapper);
        webSocketServer.sendToAllClient("删除题目: " + id);
        // 删除数据文件夹
        File dir = new File(dataPath + oldQuestion.getNum());
        FileUtils.deleteDirectory(dir);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param questionUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Transactional
    public BaseResponse<Boolean> updateQuestion(@RequestBody QuestionUpdateRequest questionUpdateRequest) {
        if (questionUpdateRequest == null || questionUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionUpdateRequest, question);
        List<String> tags = questionUpdateRequest.getTags();
        if (tags != null) {
            question.setTags(GSON.toJson(tags));
        }
        List<SampleCase> sampleCase = questionUpdateRequest.getSampleCase();
        if (sampleCase != null) {
            question.setSampleCase(GSON.toJson(sampleCase));
        }
        JudgeConfig judgeConfig = questionUpdateRequest.getJudgeConfig();
        if (judgeConfig != null) {
            question.setJudgeConfig(GSON.toJson(judgeConfig));
        }
        // 参数校验
        questionService.validQuestion(question, false);
        long id = questionUpdateRequest.getId();
        // 判断是否存在
        Question oldQuestion = questionService.getById(id);
        ThrowUtils.throwIf(oldQuestion == null, ErrorCode.NOT_FOUND_ERROR);
        // 改题目表
        boolean result = questionService.updateById(question);
        // 在questionsubmit表里查询questionid相同的字段，如果num或title改了，同步更新
        if (!Objects.equals(oldQuestion.getNum(), question.getNum()) || !Objects.equals(oldQuestion.getTitle(), question.getTitle())) {
            //根据questionId查询questionSubmit表中的数据
            QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("questionId", id);
            List<QuestionSubmit> questionSubmitList = questionSubmitService.list(queryWrapper);
            for (QuestionSubmit questionSubmit : questionSubmitList) {
                questionSubmit.setQuestionNum(question.getNum());
                questionSubmit.setQuestionTitle(question.getTitle());
                questionSubmitService.updateById(questionSubmit);
                webSocketServer.sendToAllClient("更新提交记录: " + questionSubmit.getId());
            }
        }
        //将旧文件夹改名为新文件夹
        if (!Objects.equals(oldQuestion.getNum(), question.getNum())) {
            File oldDir = new File(dataPath + oldQuestion.getNum());
            File newDir = new File(dataPath + question.getNum());
            if (oldDir.exists()) {
                boolean flag = oldDir.renameTo(newDir);
                if (!flag) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR);
                }
            }
        }
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Question> getQuestionById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 不是本人或管理员，不能直接获取所有信息
        if (!question.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return ResultUtils.success(question);
    }

    /**
     * 根据 id 获取题目信息
     *
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<QuestionVO> getQuestionVOById(Long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Question question = questionService.getById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        QuestionVO questionVO = QuestionVO.objToVo(question);
        final User loginUser = userService.getLoginUser(request);
        String acceptKey = RedisConstant.QUESTION_ACCEPT_KEY + loginUser.getUserName();
        String failKey = RedisConstant.QUESTION_FAIL_KEY + loginUser.getUserName();
        if (Boolean.TRUE.equals(template.opsForSet().isMember(acceptKey, id.toString()))) {
            questionVO.setStatus(QuestionAcceptEnum.ACCEPT.getValue());
        } else if (Boolean.TRUE.equals(template.opsForSet().isMember(failKey, id.toString()))) {
            questionVO.setStatus(QuestionAcceptEnum.UNACCEPT.getValue());
        } else {
            questionVO.setStatus(QuestionAcceptEnum.NEVER.getValue());
        }
        return ResultUtils.success(questionVO);
    }

    /**
     * 分页获取题目列表（做题端）
     *
     * @param questionQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<QuestionListVO>> listQuestionVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest, HttpServletRequest request) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        Page<QuestionListVO> questionVOPage = questionService.getQuestionVOPage(questionPage);
        final User loginUser = userService.getLoginUserPermitNull(request);
        if (loginUser != null) {
            String acceptKey = RedisConstant.QUESTION_ACCEPT_KEY + loginUser.getUserName();
            String failKey = RedisConstant.QUESTION_FAIL_KEY + loginUser.getUserName();
            questionVOPage.getRecords().forEach(questionVO -> {
                if (Boolean.TRUE.equals(template.opsForSet().isMember(acceptKey, questionVO.getId().toString()))) {
                    questionVO.setStatus(QuestionAcceptEnum.ACCEPT.getValue());
                } else if (Boolean.TRUE.equals(template.opsForSet().isMember(failKey, questionVO.getId().toString()))) {
                    questionVO.setStatus(QuestionAcceptEnum.UNACCEPT.getValue());
                } else {
                    questionVO.setStatus(QuestionAcceptEnum.NEVER.getValue());
                }
            });
        } else {
            questionVOPage.getRecords().forEach(questionVO -> {
                questionVO.setStatus(QuestionAcceptEnum.NEVER.getValue());
            });
        }
        return ResultUtils.success(questionVOPage);
    }

    /**
     * 分页获取题目列表（管理端）
     *
     * @param questionQueryRequest
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/manage/list/page/vo")
    public BaseResponse<Page<QuestionManageVO>> listQuestionManageVOByPage(@RequestBody QuestionQueryRequest questionQueryRequest) {
        long current = questionQueryRequest.getCurrent();
        long size = questionQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Question> questionPage = questionService.page(new Page<>(current, size),
                questionService.getQueryWrapper(questionQueryRequest));
        return ResultUtils.success(questionService.getQuestionManageVOPage(questionPage));
    }

    /**
     * 获取question表中递增字段num的下一个值
     *
     * @return
     */
    @GetMapping("/next-num")
    public BaseResponse<Long> getNextNum() {
        Long nextNum = questionService.getNextNum();
        return ResultUtils.success(nextNum);
    }

    /**
     * 造某道题的输出数据
     *
     * @return
     */
    @PostMapping("/create-output")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> createOutputFiles(@RequestParam Long num) {
        if (num == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Thread thread = new Thread(() -> judgeService.createOutput(num));
        thread.start();
        return ResultUtils.success(true);
    }

    /**
     * 获取某用户通过的题目
     *
     * @param userName
     * @return
     */
    @GetMapping("/accept")
    public BaseResponse<List<Question>> getAcceptQuestion(String userName) {
        String acceptKey = RedisConstant.QUESTION_ACCEPT_KEY + userName;
        Set<String> strIds = template.opsForSet().members(acceptKey);
        if (strIds == null || strIds.isEmpty()) {
            return ResultUtils.success(new ArrayList<>());
        } else {
            Set<Long> ids = strIds.stream().map(Long::valueOf).collect(Collectors.toSet());
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("id", "num").in("id", ids).orderByAsc("num");
            List<Question> questionList = questionService.list(queryWrapper);
            return ResultUtils.success(questionList);
        }
    }

    /**
     * 获取某用户未通过的题目
     *
     * @param userName
     * @return
     */
    @GetMapping("/fail")
    public BaseResponse<List<Question>> getFailQuestion(String userName) {
        String failKey = RedisConstant.QUESTION_FAIL_KEY + userName;
        Set<String> strIds = template.opsForSet().members(failKey);
        if (strIds == null || strIds.isEmpty()) {
            return ResultUtils.success(new ArrayList<>());
        } else {
            Set<Long> ids = strIds.stream().map(Long::valueOf).collect(Collectors.toSet());
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("id", "num").in("id", ids).orderByAsc("num");
            List<Question> questionList = questionService.list(queryWrapper);
            return ResultUtils.success(questionList);
        }
    }
}
