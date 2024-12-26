package com.lc.oj.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.oj.mapper.QuestionMapper;
import com.lc.oj.model.entity.Question;
import com.lc.oj.service.IQuestionService;
import org.springframework.stereotype.Service;

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

}
