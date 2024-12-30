package com.lc.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lc.oj.model.dto.question.QuestionQueryRequest;
import com.lc.oj.model.entity.Question;
import com.lc.oj.model.vo.QuestionListVO;

/**
 * <p>
 * 题目 服务类
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
public interface IQuestionService extends IService<Question> {
    /**
     * 校验
     *
     * @param question
     * @param add
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @return
     */
    Page<QuestionListVO> getQuestionVOPage(Page<Question> questionPage);

    /**
     * 获取question表中递增字段num的下一个值
     *
     * @return
     */
    Long getNextNum();
}
