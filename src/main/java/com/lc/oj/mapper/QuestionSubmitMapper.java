package com.lc.oj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lc.oj.model.entity.QuestionSubmit;
import com.lc.oj.model.vo.QuestionSubmitCountVO;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 题目提交 Mapper 接口
 * </p>
 *
 * @author lc
 * @since 2024-12-27
 */
public interface QuestionSubmitMapper extends BaseMapper<QuestionSubmit> {
    QuestionSubmitCountVO countQuestionSubmissions(@Param("userName") String userName);
}
