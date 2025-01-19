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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseStrategyAbstract implements JudgeStrategy {


    // 只显示前10个测试点的数据，数据长度不超过1000个字符
    protected static final int MAX_CASE = 10;
    protected static final int MAX_LENGTH = 1000;
    protected final Map<String, Integer> languageId = new HashMap<>(3);
    protected final Map<String, String> compilerOptions = new HashMap<>(3);
    private final String dataPath;
    private final Headers headers;
    private final String url;

    public BaseStrategyAbstract(JudgeProperties judgeProperties) {
        this.dataPath = judgeProperties.getDataPath();
        if (judgeProperties.isRapidApi()) {
            this.url = judgeProperties.getApiUrl();
            this.headers = new Headers.Builder()
                    .add("x-rapidapi-host", judgeProperties.getXRapidapiHost())
                    .add("x-rapidapi-key", judgeProperties.getXRapidapiKey())
                    .build();
        } else {
            this.url = judgeProperties.getLocalUrl();
            this.headers = new Headers.Builder().build();
        }
        languageId.put("cpp", 54);
        languageId.put("java", 62);
        languageId.put("python", 71);
        compilerOptions.put("cpp", "-fPIC -DONLINE_JUDGE -Wall -fno-asm -lm -march=native");
        compilerOptions.put("java", "");
        compilerOptions.put("python", "");
    }

    private void loadData(List<String> inputList, List<String> outputList, String path) throws Exception {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹不存在: " + path);
        }
        File[] files = dir.listFiles();
        if (files == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件夹为空: " + path);
        }
        //遍历dir文件夹中的所有.in文件，并找到同名的.out或.ans文件，如果.out和.ans都存在，优先使用.out文件
        for (File inputFile : files) {
            if (!inputFile.isFile() || !inputFile.getName().endsWith(".in")) continue;
            String name = inputFile.getName();
            String prefix = name.substring(0, name.length() - 3);
            File outFile = new File(path + File.separator + prefix + ".out");
            // 优先使用.out文件
            if (!outFile.exists()) {
                outFile = new File(path + File.separator + prefix + ".ans");
                if (!outFile.exists()) continue;
            }
            try {
                inputList.add(new String(Files.readAllBytes(Paths.get(inputFile.getPath()))));
                outputList.add(new String(Files.readAllBytes(Paths.get(outFile.getPath()))));
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败: " + e);
            }
        }
        if (inputList.isEmpty() || outputList.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "无测试数据");
        }
    }

    // 调用代码沙箱对一组数据进行判题
    protected CaseInfo doJudgeOnce(CodeSandboxRequest codeSandboxRequest, int caseId) throws Exception {

        String tokenStr = OkHttpUtils.post(url, JSONUtil.toJsonStr(codeSandboxRequest), headers);
        JSONObject tokenObject = JSONUtil.parseObj(tokenStr);
        String token = tokenObject.getStr("token");
        log.info("提交一次判题，token: {}, caseId: {}", token, caseId);
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
            if (message.contains("137")) {
                // 内存超限错误码为137
                status = "Memory Limit Exceeded";
            } else {
                status = "Runtime Error";
            }
        }
        caseInfo.setJudgeResult(JudgeResultEnum.getValueByText(status));
        if (caseInfo.getJudgeResult() == null) {
            caseInfo.setJudgeResult(JudgeResultEnum.SYSTEM_ERROR.getValue());
            caseInfo.setMessage("未知错误类型");
        } else if (caseInfo.getJudgeResult().equals(JudgeResultEnum.COMPILE_ERROR.getValue())) {
            caseInfo.setMessage(compileOutput);
        } else if (caseInfo.getJudgeResult().equals(JudgeResultEnum.RUNTIME_ERROR.getValue())) {
            caseInfo.setMessage(stderr);
        }
        // WA,TLE,MLE时为预期结果和实际结果赋值
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

    // 对所有数据进行判题
    protected abstract StrategyResponse doJudgeAll(StrategyRequest strategyRequest, List<String> inputList, List<String> outputList) throws Exception;

    @Override
    public StrategyResponse doJudgeWithStrategy(StrategyRequest strategyRequest) {
        //建立list集合，用于存放测试数据
        List<String> inputList = new ArrayList<>();
        List<String> outputList = new ArrayList<>();

        //1. 调用loadData方法，将测试数据加载到inputList和outputList中
        try {
            loadData(inputList, outputList, dataPath + strategyRequest.getNum());
        } catch (Exception e) {
            log.info("测试数据读取异常", e);
            StrategyResponse response = new StrategyResponse();
            response.setJudgeResult(JudgeResultEnum.DATA_NOT_FOUND.getValue());
            return response;
        }

        //2. 判题
        try {
            return doJudgeAll(strategyRequest, inputList, outputList);
        } catch (Exception e) {
            log.info("判题服务异常", e);
            StrategyResponse response = new StrategyResponse();
            response.setJudgeResult(JudgeResultEnum.SYSTEM_ERROR.getValue());
            return response;
        }
    }
}
