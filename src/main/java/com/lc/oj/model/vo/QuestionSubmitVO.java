package com.lc.oj.model.vo;

import cn.hutool.json.JSONUtil;
import com.lc.oj.model.dto.judge.CaseInfo;
import com.lc.oj.model.dto.judge.JudgeInfo;
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
     * 每个点的判题信息
     */
    private List<CaseInfo> caseInfoList;
    /**
     * 总的判题信息
     */
    private JudgeInfo judgeInfo;
    /**
     * 判题状态（0 - 待判题、1 - 判题中、2 - 成功、3 - 失败）
     */
    private Integer status;
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
     * 更新时间
     */
    private LocalDateTime updateTime;

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
        JudgeInfo judgeInfoObj = questionSubmitVO.getJudgeInfo();
        if (judgeInfoObj != null) {
            questionSubmit.setJudgeInfo(JSONUtil.toJsonStr(judgeInfoObj));
        }
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
        String judgeInfoStr = questionSubmit.getJudgeInfo();
        questionSubmitVO.setJudgeInfo(JSONUtil.toBean(judgeInfoStr, JudgeInfo.class));
        String caseInfoListStr = questionSubmit.getCaseInfoList();
        //CaseInfoList是一个bean的list，将字符串转为List<CaseInfo>
        questionSubmitVO.setCaseInfoList(JSONUtil.toList(JSONUtil.parseArray(caseInfoListStr), CaseInfo.class));
        return questionSubmitVO;
    }
}