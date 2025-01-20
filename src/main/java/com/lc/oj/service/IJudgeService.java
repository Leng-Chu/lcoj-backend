package com.lc.oj.service;

/**
 * 判题服务
 */
public interface IJudgeService {

    /**
     * 判题
     *
     * @param questionSubmitId
     */
    void doJudge(long questionSubmitId);

    void checkSubmit(long questionSubmitId) throws Exception;
}
