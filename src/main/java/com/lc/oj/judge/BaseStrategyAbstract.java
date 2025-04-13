package com.lc.oj.judge;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lc.oj.common.ErrorCode;
import com.lc.oj.exception.BusinessException;
import com.lc.oj.model.dto.judge.CaseInfo;
import com.lc.oj.model.dto.judge.CodeSandboxRequest;
import com.lc.oj.model.dto.judge.StrategyRequest;
import com.lc.oj.model.dto.judge.StrategyResponse;
import com.lc.oj.model.enums.JudgeResultEnum;
import com.lc.oj.properties.JudgeProperties;
import com.lc.oj.utils.Base64Utils;
import com.lc.oj.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;

import java.io.File;
import java.util.*;

@Slf4j
public abstract class BaseStrategyAbstract implements JudgeStrategy {


    // 只显示前10个测试点的数据，数据长度不超过1000个字符
    private static final int MAX_CASE = 10;
    private static final int MAX_LENGTH = 1000;
    protected final Map<String, Integer> languageId = new HashMap<>(3);
    protected final Map<String, String> compilerOptions = new HashMap<>(3);
    private final String dataPath;
    private final String tempUrl;
    private final String rapidApiHost;
    private final List<String> xRapidapiKey;
    private String url;
    private Headers headers;

    // 构造函数
    public BaseStrategyAbstract(JudgeProperties judgeProperties) {
        this.dataPath = judgeProperties.getDataPath();
        if (judgeProperties.isRapidApi()) {
            this.url = judgeProperties.getApiUrl();
            this.xRapidapiKey = judgeProperties.getXRapidapiKey();
            this.rapidApiHost = judgeProperties.getXRapidapiHost();
            this.tempUrl = judgeProperties.getLocalUrl();
        } else {
            this.url = judgeProperties.getLocalUrl();
            this.xRapidapiKey = new ArrayList<>();
            this.rapidApiHost = "";
            this.tempUrl = "";
        }
        languageId.put("cpp", 54);
        languageId.put("java", 62);
        languageId.put("python", 71);
        compilerOptions.put("cpp", "-fPIC -DONLINE_JUDGE -Wall -fno-asm -lm -march=native");
        compilerOptions.put("java", "");
        compilerOptions.put("python", "");
    }

    // 整合判题结果
    private static StrategyResponse getStrategyResponse(List<CaseInfo> caseInfoList) {
        StrategyResponse strategyResponse = new StrategyResponse();
        strategyResponse.setCaseInfoList(caseInfoList);
        for (CaseInfo caseInfo : caseInfoList) {
            //log.info("caseInfo: {}", caseInfo);
            if (caseInfo.getTime() != null) {
                if (strategyResponse.getMaxTime() == null) {
                    strategyResponse.setMaxTime(caseInfo.getTime());
                } else {
                    strategyResponse.setMaxTime(Math.max(strategyResponse.getMaxTime(), caseInfo.getTime()));
                }
            }
            if (caseInfo.getMemory() != null) {
                if (strategyResponse.getMaxMemory() == null) {
                    strategyResponse.setMaxMemory(caseInfo.getMemory());
                } else {
                    strategyResponse.setMaxMemory(Math.max(strategyResponse.getMaxMemory(), caseInfo.getMemory()));
                }
            }
        }
        for (CaseInfo caseInfo : caseInfoList) {
            if (strategyResponse.getJudgeResult() == null
                    && !Objects.equals(caseInfo.getJudgeResult(), JudgeResultEnum.ACCEPTED.getValue())) {
                strategyResponse.setJudgeResult(caseInfo.getJudgeResult());
            }
        }
        if (strategyResponse.getJudgeResult() == null) {
            strategyResponse.setJudgeResult(JudgeResultEnum.ACCEPTED.getValue());
        }
        log.info("评测完毕，结果: {}", strategyResponse.getJudgeResult());
        return strategyResponse;
    }

    // 检查路径是否正确，并获取文件夹下的所有文件
    private File[] getFiles(String path) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹不存在: " + path);
        }
        File[] files = dir.listFiles();
        if (files == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹为空: " + path);
        }
        return files;
    }

    // 加载测试数据
    protected abstract void loadData(List<String> inputList, List<String> outputList, File[] files) throws Exception;

    // 对所有数据进行判题
    protected abstract List<CaseInfo> doJudgeAll(StrategyRequest strategyRequest, List<String> inputList, List<String> outputList) throws Exception;

    // 调用代码沙箱对某一组数据进行判题
    protected CaseInfo doJudgeOnce(CodeSandboxRequest codeSandboxRequest, int caseId, boolean needOutput) throws Exception {
        if (!Objects.equals(codeSandboxRequest.getLanguage_id(), languageId.get("cpp"))) {
            codeSandboxRequest.setCpu_time_limit(codeSandboxRequest.getCpu_time_limit() * 2);
            codeSandboxRequest.setMemory_limit(codeSandboxRequest.getMemory_limit() * 2);
        }
        String tokenStr = null;
        String key = null;
        for (String k : xRapidapiKey) {
            headers = new Headers.Builder()
                    .add("x-rapidapi-host", rapidApiHost)
                    .add("x-rapidapi-key", k)
                    .build();
            tokenStr = OkHttpUtils.post(url, JSONUtil.toJsonStr(codeSandboxRequest), headers);
            if (tokenStr != null) {
                key = k.substring(0, 3);
                break;
            }
        }
        if (tokenStr == null) {
            if (tempUrl != null && !tempUrl.isEmpty()) {
                url = tempUrl;
                key = "lcoj-judge";
                headers = new Headers.Builder()
                        .add("auth", key)
                        .build();
                tokenStr = OkHttpUtils.post(url, JSONUtil.toJsonStr(codeSandboxRequest), headers);
            } else throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无可用判题机");
        }
        JSONObject tokenObject = JSONUtil.parseObj(tokenStr);
        String token = tokenObject.getStr("token");
        log.info("提交一次判题，token: {}, caseId: {}, key: {}", token, caseId, key);
        JSONObject responseObject;
        String status;
        do {
            Thread.sleep(1000); // 每1000ms查询一次结果
            String resultURL = url + "/" + token + "?base64_encoded=true"; // 通过token获取结果
            String resultStr = OkHttpUtils.get(resultURL, headers);
            responseObject = JSONUtil.parseObj(resultStr);
            status = responseObject.getJSONObject("status").getStr("description");
        } while ("In Queue".equals(status)
                || "Processing".equals(status));
        CaseInfo caseInfo = new CaseInfo();
        String message = Base64Utils.decode(responseObject.getStr("message"));
        String compileOutput = Base64Utils.decode(responseObject.getStr("compile_output"));
        String stderr = Base64Utils.decode(responseObject.getStr("stderr"));
        String stdout = Base64Utils.decode(responseObject.getStr("stdout"));
        // 为JudgeResult赋值
        if (status.contains("Runtime Error")) {
            if (message.contains("137") || stderr.contains("OutOfMemoryError")) {
                status = "Memory Limit Exceeded";
            } else {
                status = "Runtime Error";
            }
        }
        caseInfo.setJudgeResult(JudgeResultEnum.getValueByText(status));
        log.info("获取到判题结果，token: {}，judgeResult: {}", token, caseInfo.getJudgeResult());
        if (caseInfo.getJudgeResult() == null) {
            caseInfo.setJudgeResult(JudgeResultEnum.SYSTEM_ERROR.getValue());
            caseInfo.setMessage("未知错误类型");
        } else if (caseInfo.getJudgeResult().equals(JudgeResultEnum.COMPILE_ERROR.getValue())) {
            caseInfo.setMessage(compileOutput);
        } else if (caseInfo.getJudgeResult().equals(JudgeResultEnum.RUNTIME_ERROR.getValue())) {
            caseInfo.setMessage(stderr);
        } else if (caseInfo.getJudgeResult().equals(JudgeResultEnum.SYSTEM_ERROR.getValue())) {
            caseInfo.setMessage(message);
        }
        if (needOutput) {
            // 造数据时为预期输出赋值
            if (caseInfo.getJudgeResult().equals(JudgeResultEnum.ACCEPTED.getValue())) {
                caseInfo.setExpectOutput(stdout);
            }
        } else {
            // 非造数据时，WA,TLE,MLE时为预期结果和实际结果赋值
            if (stdout != null
                    && caseId < MAX_CASE
                    && (caseInfo.getJudgeResult().equals(JudgeResultEnum.WRONG_ANSWER.getValue())
                    || caseInfo.getJudgeResult().equals(JudgeResultEnum.MEMORY_LIMIT_EXCEEDED.getValue())
                    || caseInfo.getJudgeResult().equals(JudgeResultEnum.TIME_LIMIT_EXCEEDED.getValue()))) {
                if (codeSandboxRequest.getStdin().length() < MAX_LENGTH
                        && codeSandboxRequest.getExpected_output().length() < MAX_LENGTH
                        && stdout.length() < MAX_LENGTH) {
                    caseInfo.setInput(codeSandboxRequest.getStdin());
                    caseInfo.setExpectOutput(codeSandboxRequest.getExpected_output());
                    caseInfo.setWrongOutput(stdout);
                } else {
                    caseInfo.setMessage("数据过大，无法显示");
                }
            }
        }
        // 为时间和内存赋值
        // timeStr单位为秒，是小数字符串，将时间转换为Long类型的毫秒
        if (responseObject.getStr("time") != null) {
            caseInfo.setTime((long) (Double.parseDouble(responseObject.getStr("time")) * 1000));
        }
        if (responseObject.getStr("memory") != null) {
            caseInfo.setMemory(responseObject.getLong("memory"));
        }
        if (caseInfo.getMessage() == null) {
            caseInfo.setMessage("无");
        }
        caseInfo.setCaseId(caseId);
        return caseInfo;
    }

    @Override
    public StrategyResponse doJudgeWithStrategy(StrategyRequest strategyRequest) {
        //建立list集合，用于存放测试数据
        List<String> inputList = new ArrayList<>();
        List<String> outputList = new ArrayList<>();

        //1. 调用loadData方法，将测试数据加载到inputList和outputList中
        try {
            File[] files = getFiles(dataPath + strategyRequest.getNum());
            loadData(inputList, outputList, files);
        } catch (Exception e) {
            log.info("测试数据读取异常", e);
            StrategyResponse response = new StrategyResponse();
            response.setJudgeResult(JudgeResultEnum.DATA_NOT_FOUND.getValue());
            return response;
        }

        //2. 判题
        List<CaseInfo> caseInfoList = null;
        try {
            caseInfoList = doJudgeAll(strategyRequest, inputList, outputList);
        } catch (Exception e) {
            log.info("判题服务异常", e);
            StrategyResponse response = new StrategyResponse();
            response.setJudgeResult(JudgeResultEnum.SYSTEM_ERROR.getValue());
            return response;
        }

        //3. 整合判题结果
        return getStrategyResponse(caseInfoList);
    }
}
