package com.opsgenie.sirocco.warmup.strategy.impl;

import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.util.StringUtils;
import com.opsgenie.sirocco.warmup.LambdaService;
import com.opsgenie.sirocco.warmup.WarmupFunctionInfo;
import com.opsgenie.sirocco.warmup.WarmupHandler;
import com.opsgenie.sirocco.warmup.WarmupPropertyProvider;
import com.opsgenie.sirocco.warmup.strategy.WarmupStrategy;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Standard {@link WarmupStrategy} implementation which
 * warmup incrementally as randomized invocation counts
 * for preventing full load on AWS Lambda all the warmup time to
 * leave some AWS Lambda containers free/available for real requests and
 * simulating real environment as much as possible.
 *
 * @author serkan
 */
public class StandardWarmupStrategy implements WarmupStrategy {

    public static final String NAME = "standard";

    /**
     * Name of the <code>integer</code> typed property
     * which configures the invocation count for each Lambda function to warmup.
     * Note that if invocation counts are randomized,
     * this value is used as upper limit of randomly generated invocation count.
     */
    public static final String INVOCATION_COUNT_PROP_NAME =
            "sirocco.warmup.invocationCount";
    /**
     * Default value for {@link #INVOCATION_COUNT_PROP_NAME} property.
     * The default value is <code>8</code>.
     */
    public static final int DEFAULT_INVOCATION_COUNT = 8;

    /**
     * Name of the <code>integer</code> typed property name
     * which configures the count of consumers
     * to get results of warmup invocations.
     */
    public static final String INVOCATION_RESULT_CONSUMER_COUNT_PROP_NAME =
            "sirocco.warmup.invocationResultConsumerCount";
    /**
     * Default value for {@link #DEFAULT_INVOCATION_RESULT_CONSUMER_COUNT} property.
     * The default value is two times of available CPU processors.
     */
    public static final int DEFAULT_INVOCATION_RESULT_CONSUMER_COUNT =
            2 * Runtime.getRuntime().availableProcessors();

    /**
     * Name of the <code>integer</code> typed property
     * which configures the warmup iteration count.
     */
    public static final String ITERATION_COUNT_PROP_NAME =
            "sirocco.warmup.iterationCount";
    /**
     * Default value for {@link #ITERATION_COUNT_PROP_NAME} property.
     * The default value is <code>2</code>.
     */
    public static final int DEFAULT_ITERATION_COUNT = 2;

    /**
     * Name of the <code>boolean</code> typed property
     * which enables splitting iterations between multiple schedules of this handler
     * and at each schedule call only one iteration is performed.
     */
    public static final String ENABLE_SPLIT_ITERATIONS_PROP_NAME =
            "sirocco.warmup.enableSplitIterations";

    /**
     * Name of the <code>long</code> typed property
     * which configures the time interval in milliseconds
     * to bypass randomization and directly use invocation count.
     */
    public static final String RANDOMIZATION_BYPASS_INTERVAL_MILLIS_PROP_NAME =
            "sirocco.warmup.randomizationBypassInterval";
    /**
     * Default value for {@link #RANDOMIZATION_BYPASS_INTERVAL_MILLIS_PROP_NAME} property.
     * The default value is <code>30 minutes</code>.
     */
    public static final long DEFAULT_RANDOMIZATION_TIMEOUT_MILLIS = 30 * 60 * 1000; // 30 min

    /**
     * Name of the <code>boolean</code> typed property
     * which disables randomized invocation count behaviour.
     * Note that invocations counts are randomized
     * for preventing full load on AWS Lambda all the warmup time to
     * leave some AWS Lambda containers free/available for real requests and
     * simulating real environment as much as possible.
     */
    public static final String DISABLE_RANDOMIZATION_PROP_NAME =
            "sirocco.warmup.disableRandomization";

    /**
     * Name of the <code>string</code> typed property
     * which configures alias to be used as qualifier
     * while invoking Lambda functions to warmup.
     */
    public static final String WARMUP_FUNCTION_ALIAS_PROP_NAME =
            "sirocco.warmup.warmupFunctionAlias";

    /**
     * Name of the <code>boolean</code> typed property
     * which enables throwing error behaviour
     * if the warmup invocation fails for some reason.
     */
    public static final String THROW_ERROR_ON_FAILURE_PROP_NAME =
            "sirocco.warmup.throwErrorOnFailure";

    /**
     * Name of the <code>boolean</code> typed property
     * which enables waiting behaviour between each warmup invocation round.
     * This property is active by default.
     */
    public static final String WAIT_BETWEEN_INVOCATION_ROUNDS =
            "sirocco.warmup.waitBetweenInvocationRounds";

    protected final Logger logger = Logger.getLogger(getClass());

    protected final int invocationCount;
    protected final int invocationResultConsumerCount;
    protected final int iterationCount;
    protected final boolean splitIterations;
    protected int currentIterationCount = 0;
    protected final long randomizationBypassIntervalMillis;
    protected final boolean disableRandomization;
    protected final String warmupFunctionAlias;
    protected final boolean throwErrorOnFailure;
    protected final boolean waitBetweenInvocationRounds;

    protected final Map<String, Long> functionCallTimes = new HashMap<String, Long>();
    protected final ExecutorService executorService;

    public StandardWarmupStrategy() {
        this(WarmupHandler.DEFAULT_WARMUP_PROPERTY_PROVIDER);
    }

    public StandardWarmupStrategy(WarmupPropertyProvider warmupPropertyProvider) {
        this.invocationCount =
                warmupPropertyProvider.getInteger(
                        INVOCATION_COUNT_PROP_NAME,
                        DEFAULT_INVOCATION_COUNT);
        this.invocationResultConsumerCount =
                warmupPropertyProvider.getInteger(
                        INVOCATION_RESULT_CONSUMER_COUNT_PROP_NAME,
                        DEFAULT_INVOCATION_RESULT_CONSUMER_COUNT);
        this.iterationCount =
                warmupPropertyProvider.getInteger(
                        ITERATION_COUNT_PROP_NAME,
                        DEFAULT_ITERATION_COUNT);
        this.splitIterations =
                warmupPropertyProvider.getBoolean(ENABLE_SPLIT_ITERATIONS_PROP_NAME);
        this.randomizationBypassIntervalMillis =
                warmupPropertyProvider.getLong(
                        RANDOMIZATION_BYPASS_INTERVAL_MILLIS_PROP_NAME,
                        DEFAULT_RANDOMIZATION_TIMEOUT_MILLIS);
        this.disableRandomization =
                warmupPropertyProvider.getBoolean(DISABLE_RANDOMIZATION_PROP_NAME);
        this.warmupFunctionAlias =
                warmupPropertyProvider.getString(WARMUP_FUNCTION_ALIAS_PROP_NAME);
        this.throwErrorOnFailure =
                warmupPropertyProvider.getBoolean(THROW_ERROR_ON_FAILURE_PROP_NAME);
        this.waitBetweenInvocationRounds =
                warmupPropertyProvider.getBoolean(WAIT_BETWEEN_INVOCATION_ROUNDS, true);
        this.executorService =
                Executors.newFixedThreadPool(invocationResultConsumerCount);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void warmup(Context context,
                       LambdaService lambdaService,
                       Map<String, WarmupFunctionInfo> functionsToWarmup) {
        int defaultInvocationCount = getDefaultInvocationCount();

        logger.info("Default invocation count per function: " + defaultInvocationCount);

        long remainingMillis = context.getRemainingTimeInMillis();
        long iterationDurationMillis = remainingMillis / iterationCount;
        int invocationCountPerIteration = defaultInvocationCount / iterationCount;
        int remainingInvocationCountAtFinalRound =
                defaultInvocationCount - (invocationCountPerIteration * iterationCount);

        logger.info("Iteration count: " + iterationCount);

        ///////////////////////////////////////////////////////////////////////////////

        AtomicLong invocationResultCounter = new AtomicLong(0L);
        LinkedBlockingQueue<InvokeResultInfo> invocationResultFutures = new LinkedBlockingQueue<>();
        List<InvokeResultError> errors = new CopyOnWriteArrayList<>();
        AtomicBoolean stopFlag = new AtomicBoolean(false);
        List<Future> futures = new ArrayList<>(invocationResultConsumerCount);

        Random random = !disableRandomization ? new Random() : null;

        try {
            for (int i = 0; i < invocationResultConsumerCount; i++) {
                InvocationResultConsumer invocationResultConsumer =
                        new InvocationResultConsumer(
                                invocationResultCounter,
                                invocationResultFutures,
                                errors,
                                stopFlag);
                Future future = executorService.submit(invocationResultConsumer);
                futures.add(future);
            }

            ///////////////////////////////////////////////////////////////////////////////

            Map<String, List<InvokeResultInfo>> invokeResultInfosMap = new HashMap<String, List<InvokeResultInfo>>();

            logger.info("Starting iterations to warmup ...");

            int invokeCount = (currentIterationCount + 1) * invocationCountPerIteration;
            for (int i = currentIterationCount; i < iterationCount; i++) {
                long startTime = System.currentTimeMillis();

                logger.info(String.format("Iteration round %d ...", (i + 1)));
                for (Map.Entry<String, WarmupFunctionInfo> entry : functionsToWarmup.entrySet()) {
                    String functionToBeWarmup = entry.getKey();
                    WarmupFunctionInfo functionInfo = entry.getValue();

                    if (i + 1 == iterationCount) {
                        invokeCount += remainingInvocationCountAtFinalRound;
                    }

                    int actualInvocationCount = invokeCount;
                    boolean randomize = !disableRandomization;
                    Long callTime = functionCallTimes.get(functionToBeWarmup);
                    if (    callTime == null
                            ||
                            (System.currentTimeMillis() - callTime) > randomizationBypassIntervalMillis) {
                        functionCallTimes.remove(functionToBeWarmup);
                        randomize = false;
                    }
                    if (randomize) {
                        actualInvocationCount =
                                actualInvocationCount - invocationCountPerIteration +
                                (random.nextInt(invocationCountPerIteration));
                    }

                    int functionInvocationCount =
                            getInvocationCount(
                                    functionToBeWarmup,
                                    defaultInvocationCount,
                                    functionInfo.getInvocationCount(),
                                    functionInfo);
                    if (functionInvocationCount > 0) {
                        actualInvocationCount =
                                (int) (((double) (functionInvocationCount * actualInvocationCount)) / defaultInvocationCount);
                    }

                    if (actualInvocationCount == 0) {
                        actualInvocationCount = 1;
                    }

                    String alias = null;
                    if (StringUtils.hasValue(warmupFunctionAlias)) {
                        alias = warmupFunctionAlias;
                    }
                    if (StringUtils.hasValue(functionInfo.getAlias())) {
                        alias = functionInfo.getAlias();
                    }

                    if (alias != null) {
                        logger.info(String.format(
                                "Invoking function %s with alias '%s' to warmup for %d times ...",
                                functionToBeWarmup, alias, actualInvocationCount));
                    } else {
                        logger.info(String.format(
                                "Invoking function %s to warmup for %d times ...",
                                functionToBeWarmup, actualInvocationCount));
                    }

                    for (int j = 0; j < actualInvocationCount; j++) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format("Invocation round %d ...", (j + 1)));
                        }
                        InvokeRequest invokeRequest =
                                createInvokeRequest(functionInfo, functionToBeWarmup, alias, actualInvocationCount);
                        Future<InvokeResult> invokeResultFuture = lambdaService.invokeAsync(invokeRequest);
                        invocationResultCounter.incrementAndGet();
                        InvokeResultInfo invokeResultInfo =
                                new InvokeResultInfo(
                                        (i + 1), (j + 1),
                                        functionToBeWarmup, invokeResultFuture);
                        List<InvokeResultInfo> invokeResultInfos = invokeResultInfosMap.get(functionToBeWarmup);
                        if (invokeResultInfos == null) {
                            invokeResultInfos = new ArrayList<InvokeResultInfo>();
                            invokeResultInfosMap.put(functionToBeWarmup, invokeResultInfos);
                        }
                        invokeResultInfos.add(invokeResultInfo);
                        invocationResultFutures.offer(invokeResultInfo);
                    }

                    functionCallTimes.putIfAbsent(functionToBeWarmup, System.currentTimeMillis());
                }

                invokeCount += invocationCountPerIteration;
                invokeCount = Math.min(invokeCount, defaultInvocationCount);

                if (splitIterations) {
                    break;
                }

                // No need to sleep at last round
                if (i < iterationCount - 1) {
                    long passedTime = System.currentTimeMillis() - startTime;
                    long iterationRemainingMillis = iterationDurationMillis - passedTime;
                    try {
                        logger.info(String.format(
                                "Sleeping %d millis for next iteration ...", iterationRemainingMillis));
                        Thread.sleep(iterationRemainingMillis);
                    } catch (InterruptedException e) {
                    }
                }
            }

            logger.info("Finished iterations to warmup");

            ///////////////////////////////////////////////////////////////////////////////

            logger.info("Started waiting for invocations results ...");

            try {
                // We don't wait by timeout but wait infinite on purpose.
                // Because while waiting, if there is a timeout for this warmup handler function,
                // we should be aware of it
                while (invocationResultCounter.get() > 0) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
            }

            stopFlag.set(true);

            Iterator<Future> iter = futures.iterator();
            while (iter.hasNext()) {
                Future future = iter.next();
                future.cancel(true);
                iter.remove();
            }

            ///////////////////////////////////////////////////////////////////////////////

            handleInvokeResultInfos(invokeResultInfosMap);

            if (!errors.isEmpty()) {
                handleErrors(errors);
            }

            logger.info("Finished waiting for invocations results");
        } finally {
            if (splitIterations) {
                currentIterationCount = (currentIterationCount + 1) % iterationCount;
            }
            for (Future future : futures) {
                future.cancel(true);
            }
        }
    }

    protected int getDefaultInvocationCount() {
        return invocationCount;
    }

    protected int getInvocationCount(String functionName, int defaultInvocationCount, int configuredInvocationCount,
                                     WarmupFunctionInfo functionInfo) {
        if (configuredInvocationCount > 0) {
            return configuredInvocationCount;
        }
        return defaultInvocationCount;
    }

    protected InvokeRequest createInvokeRequest(WarmupFunctionInfo functionInfo, String functionName,
                                                String alias, int actualInvocationCount) {
        InvokeRequest invokeRequest =
            new InvokeRequest().
                    withFunctionName(functionName).
                    withPayload(
                            ByteBuffer.wrap(
                                    createInvokeRequestPayload(functionInfo, functionName, actualInvocationCount)));
        if (alias != null) {
            invokeRequest.withQualifier(alias);
        }
        return invokeRequest;
    }

    protected byte[] createInvokeRequestPayload(WarmupFunctionInfo functionInfo, String functionName,
                                                int actualInvocationCount) {
        String invocationData = functionInfo.getInvocationData();
        if (StringUtils.isNullOrEmpty(invocationData)) {
            return new byte[0];
        } else {
            return invocationData.getBytes();
        }
    }

    protected void handleInvokeResultInfos(Map<String, List<InvokeResultInfo>> invokeResultInfosMap) {
    }

    protected void handleErrors(List<InvokeResultError> errors) {
        StringBuilder errorMessageBuilder = new StringBuilder("[ERRORS]\n");
        int errorCount = 1;
        for (InvokeResultError error : errors) {
            errorMessageBuilder.append("\t- Error [").append(errorCount++).append("]\n");
            errorMessageBuilder.append("\t\t- Iteration  No: ").append(error.iterationNo).append("\n");
            errorMessageBuilder.append("\t\t- Invocation No: ").append(error.invocationNo).append("\n");
            errorMessageBuilder.append("\t\t- Function Name: ").append(error.functionName).append("\n");
            errorMessageBuilder.append("\t\t- Error        : ").append(error.error.getMessage()).append("\n");
        }
        if (throwErrorOnFailure) {
            logger.error(errorMessageBuilder.toString());
        } else {
            throw new RuntimeException(errorMessageBuilder.toString());
        }
    }

    protected static class InvokeResultInfo {

        protected final int iterationNo;
        protected final int invocationNo;
        protected final String functionName;
        protected final Future<InvokeResult> invokeResultFuture;
        protected volatile InvokeResult invokeResult;

        protected InvokeResultInfo(int iterationNo, int invocationNo,
                                   String functionName, Future<InvokeResult> invokeResultFuture) {
            this.iterationNo = iterationNo;
            this.invocationNo = invocationNo;
            this.functionName = functionName;
            this.invokeResultFuture = invokeResultFuture;
        }

    }

    protected static class InvokeResultError {

        protected final int iterationNo;
        protected final int invocationNo;
        protected final String functionName;
        protected final Throwable error;

        protected InvokeResultError(int iterationNo, int invocationNo,
                                    String functionName, Throwable error) {
            this.iterationNo = iterationNo;
            this.invocationNo = invocationNo;
            this.functionName = functionName;
            this.error = error;
        }

    }

    protected class InvocationResultConsumer implements Runnable {

        protected final AtomicLong invocationResultCounter;
        protected final LinkedBlockingQueue<InvokeResultInfo> invocationResultFutures;
        protected final List<InvokeResultError> errors;
        protected final AtomicBoolean stopFlag;

        protected InvocationResultConsumer(AtomicLong invocationResultCounter,
                                           LinkedBlockingQueue<InvokeResultInfo> invocationResultFutures,
                                           List<InvokeResultError> errors,
                                           AtomicBoolean stopFlag) {
            this.invocationResultCounter = invocationResultCounter;
            this.invocationResultFutures = invocationResultFutures;
            this.errors = errors;
            this.stopFlag = stopFlag;
        }

        @Override
        public void run() {
            while (!stopFlag.get()) {
                InvokeResultInfo invokeResultInfo = null;
                try {
                    invokeResultInfo = invocationResultFutures.take();
                    invokeResultInfo.invokeResult = invokeResultInfo.invokeResultFuture.get();
                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format(
                                "Invocation result has been successfully retrieved at iteration %d and invocation %d for function %s",
                                invokeResultInfo.iterationNo, invokeResultInfo.invocationNo, invokeResultInfo.functionName));
                    }
                } catch (Throwable t) {
                    if (t instanceof InterruptedException) {
                        return;
                    }
                    if (invokeResultInfo != null) {
                        logger.error(String.format(
                                "Retrieving invocation result has failed at iteration %d and invocation %d for function %s!",
                                invokeResultInfo.iterationNo, invokeResultInfo.invocationNo, invokeResultInfo.functionName),
                                t);
                        errors.add(new InvokeResultError(
                                invokeResultInfo.iterationNo, invokeResultInfo.invocationNo,
                                invokeResultInfo.functionName, t));
                    } else {
                        logger.error("Error occurred while retrieving invocation result!", t);
                    }
                } finally {
                    invocationResultCounter.decrementAndGet();
                }
            }
        }

    }

}
