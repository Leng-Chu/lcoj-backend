package com.lc.oj.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.CommonConstant;
import com.lc.oj.constant.RedisConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.mapper.QuestionMapper;
import com.lc.oj.model.dto.question.QuestionQueryRequest;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.vo.QuestionListVO;
import com.lc.oj.model.vo.QuestionManageVO;
import com.lc.oj.service.IQuestionService;
import com.lc.oj.utils.CacheUtils;
import com.lc.oj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 题目 服务实现类
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
@Service
@Slf4j
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    @Resource
    private StringRedisTemplate template;
    @Resource
    private CacheUtils cacheUtils;

    /**
     * 校验题目是否合法
     *
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long num = question.getNum();
        String title = question.getTitle();
        String content = question.getContent();
        String answer = question.getAnswer();
        String sampleCase = question.getSampleCase();
        // 参数不能为空
        if (StringUtils.isAnyBlank(title, content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题和题目描述不能为空");
        }
        if (num == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题号不能为空");
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目描述过长");
        }
        if (StringUtils.isNotBlank(answer) && answer.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标程过长");
        }
        if (StringUtils.isNotBlank(sampleCase) && sampleCase.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "样例过长");
        }
        //如果num不为空，判断是否重复
        if (num <= 0 || num > 100000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题号应在1~100000范围内");
        }
        //判断题号是否重复，已逻辑删除的题目不算
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id").eq("num", num);
        if (!add) {
            queryWrapper.ne("id", question.getId());
        }
        List<Question> questions = this.list(queryWrapper);
        if (questions != null && !questions.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题号重复");
        }
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return null;
        }
        Long num = questionQueryRequest.getNum();
        String title = questionQueryRequest.getTitle();
        List<String> tags = questionQueryRequest.getTags();
        String userName = questionQueryRequest.getUserName();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();
        if (StringUtils.isAllBlank(title, userName) && ObjectUtils.isEmpty(num) && ObjectUtils.isEmpty(userName) && tags.isEmpty()) {
            return null;
        }
        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(num), "num", num);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<QuestionListVO> getQuestionVOPage(Page<Question> questionPage) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionListVO> questionListVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (questionList == null || questionList.isEmpty()) {
            return questionListVOPage;
        }
        // 填充信息
        List<QuestionListVO> questionVOList = questionList.stream().map(QuestionListVO::objToVo).collect(Collectors.toList());
        questionListVOPage.setRecords(questionVOList);
        return questionListVOPage;
    }

    @Override
    public Page<QuestionManageVO> getQuestionManageVOPage(Page<Question> questionPage) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionManageVO> questionManageVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (questionList == null || questionList.isEmpty()) {
            return questionManageVOPage;
        }
        // 填充信息
        List<QuestionManageVO> questionVOList = questionList.stream().map(QuestionManageVO::objToVo).collect(Collectors.toList());
        questionManageVOPage.setRecords(questionVOList);
        return questionManageVOPage;
    }

    @Override
    public Long getNextNum() {
        long nextNum = 1L;
        Set<ZSetOperations.TypedTuple<String>> highestScoredElements =
                template.opsForZSet().reverseRangeWithScores(RedisConstant.QUESTION_LIST_KEY, 0, 0);
        if (highestScoredElements != null && !highestScoredElements.isEmpty()) {
            ZSetOperations.TypedTuple<String> highestScoredElement = highestScoredElements.iterator().next();
            nextNum = (long) (highestScoredElement.getScore() + 1);
        }
        return nextNum;
    }

    /**
     * 从Redis中获取某个用户通过的题目或没通过的题目
     *
     * @param key
     * @return
     */
    @Override
    public List<Question> getQuestionList(String key) {
        List<Question> questionList = new ArrayList<>();
        Set<String> strIds = template.opsForSet().members(key);
        if (strIds == null || strIds.isEmpty()) {
            return questionList;
        }
        for (String strId : strIds) {
            Long id = Long.valueOf(strId);
            Double num = template.opsForZSet().score(RedisConstant.QUESTION_LIST_KEY, strId);
            Question question = new Question();
            question.setId(id);
            if (num != null) {
                question.setNum(num.longValue());
            }
            questionList.add(question);
        }
        questionList.sort(Comparator.comparing(Question::getNum));
        return questionList;
    }

    /**
     * 获取题目分页（缓存）
     *
     * @param current
     * @param size
     */
    @Override
    public Page<Question> getPageByCache(long current, long size) {
        Page<Question> questionPage = new Page<>(current, size);
        int x = (int) ((current - 1) * size);
        int y = (int) (current * size - 1);
        List<String> idList = new ArrayList<>(template.opsForZSet().range(RedisConstant.QUESTION_LIST_KEY, x, y));
        List<String> keyList = new ArrayList<>(idList);
        keyList.replaceAll(s -> RedisConstant.QUESTION_CACHE_KEY + s);
        List<String> jsonList = template.opsForValue().multiGet(keyList);
        List<Question> questionList = new ArrayList<>();
        int cnt = 0;
        for (int i = 0; i < jsonList.size(); i++) {
            Question question;
            if (jsonList.get(i) == null) {
                Long id = Long.parseLong(idList.get(i));
                question = cacheUtils.query(RedisConstant.QUESTION_CACHE_KEY, id, Question.class, this::getById);
            } else {
                question = JSONUtil.toBean(jsonList.get(i), Question.class);
                cnt++;
            }
            questionList.add(question);
        }
        if (cnt != 0) {
            log.info("缓存批量查询成功，cnt={}", cnt);
        }
        questionPage.setRecords(questionList);
        questionPage.setTotal(template.opsForZSet().zCard(RedisConstant.QUESTION_LIST_KEY));
        return questionPage;
    }
}
