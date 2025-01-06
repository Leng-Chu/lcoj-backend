package com.lc.oj.service;

/**
 * 判题服务
 */
public interface IJudgeService {

    /**
     * 判题
     *
     * @param questionSubmitId
     * @return
     */
    boolean doJudge(long questionSubmitId);

    void checkSubmit(long questionSubmitId);
}
