package com.lc.oj.model.vo;

import cn.hutool.json.JSONUtil;
import com.lc.oj.model.dto.judge.CaseInfo;
import com.lc.oj.model.entity.QuestionSubmit;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目提交封装类
 *
 * @TableName question
 */
@Data
public class QuestionSubmitVO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 编程语言
     */
    private String language;
    /**
     * 用户代码
     */
    private String code;
    /**
     * 判题结果（0 - 等待判题、1 - 通过题目、 2~7 - 未通过、 8 - 无测评数据）
     */
    private Integer judgeResult;
    /**
     * 最大耗时
     */
    private Long maxTime;
    /**
     * 最大内存
     */
    private Long maxMemory;
    /**
     * 每个点的判题信息
     */
    private List<CaseInfo> caseInfoList;
    /**
     * 题目id
     */
    private Long questionId;
    /**
     * 题号
     */
    private Long questionNum;
    /**
     * 题目标题
     */
    private String questionTitle;
    /**
     * 提交者
     */
    private String userName;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 包装类转对象
     *
     * @param questionSubmitVO
     * @return
     */
    public static QuestionSubmit voToObj(QuestionSubmitVO questionSubmitVO) {
        if (questionSubmitVO == null) {
            return null;
        }
        QuestionSubmit questionSubmit = new QuestionSubmit();
        BeanUtils.copyProperties(questionSubmitVO, questionSubmit);
        List<CaseInfo> caseInfoList = questionSubmitVO.getCaseInfoList();
        if (caseInfoList != null) {
            questionSubmit.setCaseInfoList(JSONUtil.toJsonStr(caseInfoList));
        }
        return questionSubmit;
    }

    /**
     * 对象转包装类
     *
     * @param questionSubmit
     * @return
     */
    public static QuestionSubmitVO objToVo(QuestionSubmit questionSubmit) {
        if (questionSubmit == null) {
            return null;
        }
        QuestionSubmitVO questionSubmitVO = new QuestionSubmitVO();
        BeanUtils.copyProperties(questionSubmit, questionSubmitVO);
        String caseInfoListStr = questionSubmit.getCaseInfoList();
        //CaseInfoList是一个bean的list，将字符串转为List<CaseInfo>
        questionSubmitVO.setCaseInfoList(JSONUtil.toList(JSONUtil.parseArray(caseInfoListStr), CaseInfo.class));
        return questionSubmitVO;
    }
}