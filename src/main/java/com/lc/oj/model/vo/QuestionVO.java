package com.lc.oj.model.vo;

import cn.hutool.json.JSONUtil;
import com.lc.oj.model.dto.question.JudgeConfig;
import com.lc.oj.model.dto.question.SampleCase;
import com.lc.oj.model.entity.Question;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目封装类
 *
 * @TableName question
 */
@Data
public class QuestionVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 题号
     */
    private Long num;
    /**
     * 标题
     */
    private String title;
    /**
     * 内容
     */
    private String content;
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 判题配置（json 对象）
     */
    private JudgeConfig judgeConfig;
    /**
     * 样例
     */
    private List<SampleCase> sampleCase;
    /**
     * 当前用户是否通过
     */
    private Integer status;

    /**
     * 包装类转对象
     *
     * @param questionVO
     * @return
     */
    public static Question voToObj(QuestionVO questionVO) {
        if (questionVO == null) {
            return null;
        }
        Question question = new Question();
        BeanUtils.copyProperties(questionVO, question);
        List<String> tagList = questionVO.getTags();
        if (tagList != null) {
            question.setTags(JSONUtil.toJsonStr(tagList));
        }
        JudgeConfig voJudgeConfig = questionVO.getJudgeConfig();
        if (voJudgeConfig != null) {
            question.setJudgeConfig(JSONUtil.toJsonStr(voJudgeConfig));
        }
        List<SampleCase> sampleCase = questionVO.getSampleCase();
        if (sampleCase != null) {
            question.setSampleCase(JSONUtil.toJsonStr(sampleCase));
        }
        return question;
    }

    /**
     * 对象转包装类
     *
     * @param question
     * @return
     */
    public static QuestionVO objToVo(Question question) {
        if (question == null) {
            return null;
        }
        QuestionVO questionVO = new QuestionVO();
        BeanUtils.copyProperties(question, questionVO);
        List<String> tagList = JSONUtil.toList(question.getTags(), String.class);
        questionVO.setTags(tagList);
        String judgeConfigStr = question.getJudgeConfig();
        questionVO.setJudgeConfig(JSONUtil.toBean(judgeConfigStr, JudgeConfig.class));
        List<String> sampleCaseStr = JSONUtil.toList(question.getSampleCase(), String.class);
        List<SampleCase> sampleCase = new ArrayList<>();
        for (String s : sampleCaseStr) {
            sampleCase.add(JSONUtil.toBean(s, SampleCase.class));
        }
        questionVO.setSampleCase(sampleCase);
        return questionVO;
    }
}