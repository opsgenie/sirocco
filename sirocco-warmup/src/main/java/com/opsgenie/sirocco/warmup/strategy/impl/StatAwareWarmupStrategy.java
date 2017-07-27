package com.opsgenie.sirocco.warmup.strategy.impl;

import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opsgenie.core.util.ExceptionUtil;
import com.opsgenie.sirocco.api.control.ControlRequestBuilder;
import com.opsgenie.sirocco.api.control.ControlRequestConstants;
import com.opsgenie.sirocco.warmup.WarmupFunctionInfo;
import com.opsgenie.sirocco.warmup.WarmupHandler;
import com.opsgenie.sirocco.warmup.WarmupPropertyProvider;
import com.opsgenie.sirocco.warmup.strategy.WarmupStrategy;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * {@link WarmupStrategy} implementation which
 * takes Lambda stats into consideration while warmup.
 * If the target Lambda function is hot (invoked frequently),
 * it is aimed to keep more instance of that Lambda function up
 * by warmup it with more concurrent invocation.
 *
 * @author serkan
 */
public class StatAwareWarmupStrategy extends StandardWarmupStrategy {

    /**
     * Name of the {@link StatAwareWarmupStrategy}.
     */
    public static final String NAME = "stat-aware";

    /**
     * Name of the <code>long</code> typed property
     * which configures the passed time in milliseconds to
     * consider a Lambda function is idle.
     */
    public static final String FUNCTION_INSTANCE_IDLE_TIME_PROP_NAME =
            "sirocco.warmup.functionInstanceIdleTime";
    /**
     * Default value for {@link #FUNCTION_INSTANCE_IDLE_TIME_PROP_NAME} property.
     * The default value is <code>30 minutes</code>.
     */
    public static final long DEFAULT_FUNCTION_INSTANCE_IDLE_TIME = 30 * 60 * 1000; // 30 min

    /**
     * Name of the <code>float</code> typed property
     * which configures scale factor to increase/decrease
     * Lambda invocation count according to its stat (it is hot or not).
     */
    public static final String WARMUP_SCALE_FACTOR_PROP_NAME =
            "sirocco.warmup.warmupScaleFactor";
    /**
     * Default value for {@link #WARMUP_SCALE_FACTOR_PROP_NAME} property.
     * The default value is <code>2.0</code>.
     */
    public static final float DEFAULT_WARMUP_SCALE_FACTOR = 2.0F;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Map<String, Date>> functionLatestRequestTimeMap =
            new HashMap<String, Map<String, Date>>();
    private final long functionInstanceIdleTime;
    private final float warmupScaleFactor;

    public StatAwareWarmupStrategy() {
        this(WarmupHandler.DEFAULT_WARMUP_PROPERTY_PROVIDER);
    }

    public StatAwareWarmupStrategy(WarmupPropertyProvider warmupPropertyProvider) {
        this.functionInstanceIdleTime =
                warmupPropertyProvider.getLong(
                        FUNCTION_INSTANCE_IDLE_TIME_PROP_NAME,
                        DEFAULT_FUNCTION_INSTANCE_IDLE_TIME);
        this.warmupScaleFactor =
                warmupPropertyProvider.getFloat(
                        WARMUP_SCALE_FACTOR_PROP_NAME,
                        DEFAULT_WARMUP_SCALE_FACTOR);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected byte[] createInvokeRequestPayload(WarmupFunctionInfo functionInfo, String functionName,
                                                int actualInvocationCount) {
        String controlRequest =
                new ControlRequestBuilder().
                        controlRequestType("warmup").
                        controlRequestArgument(
                            ControlRequestConstants.WAIT_ARGUMENT,
                            100 * (actualInvocationCount / 10)). // Additional wait time to default one (100 ms)
                        build();
        return controlRequest.getBytes();
    }

    protected boolean isFunctionInstanceExpired(long currentTime, long latestRequestTime) {
        return currentTime > latestRequestTime + functionInstanceIdleTime;
    }

    @Override
    protected int getInvocationCount(String functionName, int defaultInvocationCount, int configuredInvocationCount,
                                     WarmupFunctionInfo functionInfo) {
        int invocationCount;
        Map<String, Date> latestRequestTimeMap = functionLatestRequestTimeMap.get(functionName);
        if (latestRequestTimeMap != null) {
            long currentTime = System.currentTimeMillis();
            int activeInstanceCount = 0;
            Iterator<Date> iter = latestRequestTimeMap.values().iterator();
            while (iter.hasNext()) {
                Long latestRequestTime = iter.next().getTime();
                if (isFunctionInstanceExpired(currentTime, latestRequestTime)) {
                    iter.remove();
                } else {
                    activeInstanceCount++;
                }
            }
            logger.info("Detected active instance count for function " + functionName + ": " + activeInstanceCount);
            invocationCount =
                    Math.max(
                        (int) (activeInstanceCount * warmupScaleFactor),
                        super.getInvocationCount(functionName, defaultInvocationCount, configuredInvocationCount, functionInfo));

        } else {
            invocationCount =
                    super.getInvocationCount(functionName, defaultInvocationCount, configuredInvocationCount, functionInfo);
        }
        logger.info("Calculated invocation count for function " + functionName + ": " + invocationCount);
        return invocationCount;
    }

    @Override
    protected void handleInvokeResultInfos(Map<String, List<InvokeResultInfo>> invokeResultInfosMap) {
        for (Map.Entry<String, List<InvokeResultInfo>> entry : invokeResultInfosMap.entrySet()) {
            String functionName = entry.getKey();
            List<InvokeResultInfo> invokeResultInfos = entry.getValue();
            for (InvokeResultInfo invokeResultInfo : invokeResultInfos) {
                InvokeResult invokeResult = invokeResultInfo.invokeResult;
                String functionError = invokeResult.getFunctionError();
                if (StringUtils.hasValue(functionError)) {
                    JSONObject invokeResultJsonObj =
                            new JSONObject(new String(invokeResult.getPayload().array()));
                    String errorMessage;
                    if (invokeResultJsonObj.has("errorMessage")) {
                        errorMessage = invokeResultJsonObj.getString("errorMessage");
                    } else {
                        errorMessage = functionError;
                    }
                    logger.error("Warmup invocation for function " + functionName +
                                 " has returned with error: " + errorMessage);
                } else {
                    String response = new String(invokeResult.getPayload().array());
                    Map<String, Object> responseValues = null;
                    try {
                        responseValues = objectMapper.readValue(response, Map.class);
                    } catch (IOException e) {
                        ExceptionUtil.sneakyThrow(e);
                    }
                    String instanceId = (String) responseValues.get("instanceId");
                    String latestRequestTimeStr = (String) responseValues.get("latestRequestTime");
                    if (latestRequestTimeStr != null) {
                        Date latestRequestTime = null;
                        try {
                            latestRequestTime = ControlRequestConstants.DATE_FORMAT.parse(latestRequestTimeStr);
                        } catch (ParseException e) {
                            ExceptionUtil.sneakyThrow(e);
                        }
                        if (latestRequestTime.getTime() > 0) {
                            Map<String, Date> latestRequestTimeMap = functionLatestRequestTimeMap.get(functionName);
                            if (latestRequestTimeMap == null) {
                                latestRequestTimeMap = new HashMap<String, Date>();
                                functionLatestRequestTimeMap.put(functionName, latestRequestTimeMap);
                            }
                            latestRequestTimeMap.put(instanceId, latestRequestTime);
                        }
                    }
                }
            }
        }

        logger.info("Latest requests times of functions: " + functionLatestRequestTimeMap);

        evictExpiredLatestRequestTimes();
    }

    private void evictExpiredLatestRequestTimes() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, Map<String, Date>> entry : functionLatestRequestTimeMap.entrySet()) {
            Map<String, Date> latestRequestTimeMap = entry.getValue();
            Iterator<Date> iter = latestRequestTimeMap.values().iterator();
            while (iter.hasNext()) {
                Long latestRequestTime = iter.next().getTime();
                if (isFunctionInstanceExpired(currentTime, latestRequestTime)) {
                    iter.remove();
                }
            }
        }
    }

}
