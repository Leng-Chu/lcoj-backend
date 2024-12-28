package com.lc.oj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.constant.CommonConstant;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.mapper.QuestionMapper;
import com.lc.oj.model.dto.question.QuestionQueryRequest;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.vo.QuestionListVO;
import com.lc.oj.service.IQuestionService;
import com.lc.oj.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
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
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements IQuestionService {

    /**
     * 校验题目是否合法
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
        if (!add && num == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新题目时题号不能为空");
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
        if (num != null) {
            //题号必须>0
            if (num <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题号必须大于0");
            }
            QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("num", num);
            Question questionInDB = getOne(queryWrapper);
            if (questionInDB != null && !questionInDB.getId().equals(question.getId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "题号重复");
            }
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
            return queryWrapper;
        }
        Long num = questionQueryRequest.getNum();
        String title = questionQueryRequest.getTitle();
        List<String> tags = questionQueryRequest.getTags();
        String userName = questionQueryRequest.getUserName();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(num), "num", num);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public Page<QuestionListVO> getQuestionVOPage(Page<Question> questionPage) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionListVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (questionList==null || questionList.isEmpty()) {
            return questionVOPage;
        }
        // 填充信息
        List<QuestionListVO> questionVOList = questionList.stream().map(QuestionListVO::objToVo).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }
}
