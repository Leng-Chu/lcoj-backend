package com.lc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.CommonConstant;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.mapper.QuestionSubmitMapper;
import com.lc.oj.message.MessageProducer;
import com.lc.oj.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.lc.oj.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.entity.User;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.model.enums.QuestionSubmitLanguageEnum;
import com.lc.oj.model.vo.QuestionSubmitCountVO;
import com.lc.oj.model.vo.QuestionSubmitVO;
import com.lc.oj.service.IQuestionService;
import com.lc.oj.service.IQuestionSubmitService;
import com.lc.oj.service.IUserService;
import com.lc.oj.utils.CacheUtils;
import com.lc.oj.utils.SqlUtils;
import com.lc.oj.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 题目提交 服务实现类
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@Service
@Slf4j
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit> implements IQuestionSubmitService {
    @Resource
    private IQuestionService questionService;
    @Resource
    private MessageProducer messageProducer;
    @Resource
    private IUserService userService;
    @Resource
    private WebSocketServer webSocketServer;
    @Resource
    private QuestionSubmitMapper questionSubmitMapper;
    @Resource
    private StringRedisTemplate template;
    @Resource
    private CacheUtils cacheUtils;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验该用户是否有正在评测的题目
        String value = template.opsForValue().get(RedisConstant.SUBMIT_LOCK_KEY + loginUser.getId());
        if (StringUtils.isNotBlank(value)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "您有正在评测的题目，请勿重复提交");
        }
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = cacheUtils.query(RedisConstant.QUESTION_CACHE_KEY, questionId, Question.class, x -> questionService.getById(x));
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 更新题目提交数，然后删除缓存
        question.setSubmitNum(question.getSubmitNum() + 1);
        questionService.updateById(question);
        template.delete(RedisConstant.QUESTION_CACHE_KEY + questionId);
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(loginUser.getId());
        questionSubmit.setUserName(loginUser.getUserName());
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setQuestionTitle(question.getTitle());
        questionSubmit.setQuestionNum(question.getNum());
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        questionSubmit.setCaseInfoList("[]");
        boolean save = this.save(questionSubmit);
        if (!save) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        LocalDateTime createTime = LocalDateTime.now();
        Instant instant = createTime.atZone(ZoneId.of("UTC")).toInstant();
        long time = instant.getEpochSecond();
        template.opsForZSet().add(RedisConstant.SUBMIT_LIST_KEY, String.valueOf(questionSubmit.getId()), time);
        webSocketServer.sendToAllClient("添加提交记录: " + questionSubmitId);
        messageProducer.sendJudgeMessage(questionSubmitId);
        // 设置提交锁的有效期为 60 秒
        template.opsForValue().set(RedisConstant.SUBMIT_LOCK_KEY + loginUser.getId(), String.valueOf(questionSubmitId), 60, TimeUnit.SECONDS);
        return questionSubmitId;
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return null;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer judgeResult = questionSubmitQueryRequest.getJudgeResult();
        Long questionNum = questionSubmitQueryRequest.getQuestionNum();
        String questionTitle = questionSubmitQueryRequest.getQuestionTitle();
        String userName = questionSubmitQueryRequest.getUserName();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();
        if (StringUtils.isAllBlank(language, questionTitle, userName) && ObjectUtils.isEmpty(judgeResult) && ObjectUtils.isEmpty(questionNum)) {
            return null;
        }
        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StringUtils.isNotBlank(questionTitle), "questionTitle", questionTitle);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionNum), "questionNum", questionNum);
        queryWrapper.eq(JudgeResultEnum.getEnumByValue(judgeResult) != null, "judgeResult", judgeResult);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码和提交记录信息
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userService.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
            questionSubmitVO.setCaseInfoList(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (questionSubmitList == null || questionSubmitList.isEmpty()) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }

    /**
     * 统计用户提交情况
     *
     * @param userName
     * @return
     */
    @Override
    public QuestionSubmitCountVO countQuestionSubmissions(String userName) {
        return questionSubmitMapper.countQuestionSubmissions(userName);
    }

    /**
     * 获取题目提交分页（缓存）
     *
     * @param current
     * @param size
     * @return
     */
    @Override
    public Page<QuestionSubmit> getPageByCache(long current, long size) {
        Page<QuestionSubmit> questionSubmitPage = new Page<>(current, size);
        int x = (int) ((current - 1) * size);
        int y = (int) (current * size - 1);
        List<String> idList = new ArrayList<>(template.opsForZSet().reverseRange(RedisConstant.SUBMIT_LIST_KEY, x, y));
        List<String> keyList = new ArrayList<>(idList);
        keyList.replaceAll(s -> RedisConstant.SUBMIT_CACHE_KEY + s);
        List<String> jsonList = template.opsForValue().multiGet(keyList);
        List<QuestionSubmit> questionSubmitList = new ArrayList<>();
        int cnt = 0;
        for (int i = 0; i < jsonList.size(); i++) {
            QuestionSubmit questionSubmit;
            if (jsonList.get(i) == null) {
                Long id = Long.parseLong(idList.get(i));
                questionSubmit =
                        cacheUtils.query(RedisConstant.SUBMIT_CACHE_KEY, id, QuestionSubmit.class, this::getById);
            } else {
                questionSubmit = JSONUtil.toBean(jsonList.get(i), QuestionSubmit.class);
                cnt++;
            }
            questionSubmitList.add(questionSubmit);
        }
        if (cnt != 0) {
            log.info("缓存批量查询成功，cnt={}", cnt);
        }
        questionSubmitPage.setRecords(questionSubmitList);
        questionSubmitPage.setTotal(template.opsForZSet().zCard(RedisConstant.SUBMIT_LIST_KEY));
        return questionSubmitPage;
    }
}
