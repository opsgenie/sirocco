# Sirocco Warmup

Warmup support for AWS Lambda functions to prevent cold starts as much as possible.

## How to Use

### WarmupHandler

`com.opsgenie.sirocco.warmup.WarmupHandler` is the AWS Lambda `RequestHandler` implementation which triggers warmup action through `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy`s for configured/discovered functions.

This handler needs some permissions to do its job.
* `lambda:InvokeFunction`: This permission is needed for invoking functions to warmup.
* `lambda:ListAliases`: This permission is needed when the alias discovery is used (enabled by default) for invoking functions by using alias as qualifier to warmup.
* `lambda:ListFunctions`: This permission is needed when any configuration discovery is used (enabled by default) for retrieving configurations of functions to warmup.

### WarmupStrategy

`com.opsgenie.sirocco.warmup.strategy.WarmupStrategy` is the interface for implementations which execute warmup action for the given AWS Lambda functions.

#### StandardWarmupStrategy

`com.opsgenie.sirocco.warmup.strategy.impl.StandardWarmupStrategy` is the standard `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy` implementation which warmup incrementally as randomized invocation counts for preventing full load on AWS Lambda all the warmup time to leave some AWS Lambda containers free/available for real requests and simulating real environment as much as possible.
 
#### StatAwareWarmupStrategy

`com.opsgenie.sirocco.warmup.strategy.impl.StatAwareWarmupStrategy` is the `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy` implementation which takes Lambda stats into consideration while warmup. If the target Lambda function is hot (invoked frequently), it is aimed to keep more instance of that Lambda function up by warmup it with more concurrent invocation.
 
#### StrategyAwareWarmupStrategy

`com.opsgenie.sirocco.warmup.strategy.impl.StrategyAwareWarmupStrategy` is the `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy` implementation which takes configured/specified `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy`s for functions into consideration while warmup. If there is no configured/specified `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy`s, uses given `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy` by default.

## Configuration

**Note:** Since AWS Lambda environment variable names cannot contain `.` character, `_` character can be used instead for the property names. `_` character is replaced with `.` internally.

### Configurations of WarmupHandler

- `sirocco.warmup.function`: `String` typed property prefix that declares warmup functions and their configurations.
- `sirocco.warmup.disableAllDiscoveries`: `Boolean` typed property that disables discovery mechanism for all configurations. Default value is `false`.
- `sirocco_warmup_warmupAware`: Name of the `Boolean` typed environment variable to be used for discovering Lambda functions to warmup. If a Lambda function wants to be warmed-up, it can publish itself by having this environment variable as enabled (`true`). Then, this handler will assume that this Lambda function want to be warmed-up and will add it to its function list to warmup.
- `sirocco.warmup.disableWarmupAwareDiscovery`: `Boolean` typed property that disables discovery mechanism for warmup aware functions specified by `com.opsgenie.sirocco.warmup.WarmupHandler#WARMUP_AWARE_ENV_VAR_NAME`. Default value is `false`.
- `sirocco_warmup_warmupGroupName`: `String` typed property that configures group name of this handler. If warmup group name is specified by this property for this handler, this handler only discovers and warms-up Lambda functions in the same warmup group (having same warmup group name specified by `com.opsgenie.sirocco.warmup.WarmupHandler#WARMUP_GROUP_NAME_ENV_VAR_NAME`).
- `sirocco.warmup.groupName`: `String` typed property that configures group name of this handler. If warmup group name is specified by this property for this handler, this handler only discovers and warms-up Lambda functions in the same warmup group (having same warmup group name specified by `com.opsgenie.sirocco.warmup.WarmupHandler#WARMUP_GROUP_NAME_ENV_VAR_NAME`).
- `sirocco.warmup.strategy`: `String` typed property that configures name of the `com.opsgenie.sirocco.warmup.strategy.WarmupStrategy` implementation to be used. Default value is the name of the `com.opsgenie.sirocco.warmup.strategy.impl.StrategyAwareWarmupStrategy`.
- `sirocco_warmup_warmupStrategy`: `String` typed environment variable to be used for discovering specific warmup strategy name configuration of Lambda functions to warmup.
- `sirocco.warmup.disableWarmupStrategyDiscovery`: `Boolean` typed property that disables discovery mechanism for warmup strategy name configurations specified by `com.opsgenie.sirocco.warmup.WarmupHandler#WARMUP_STRATEGY_ENV_VAR_NAME`. Default value is `false`.
- `sirocco.warmup.invocationData`: `String` typed property that configures invocation data to be used as invocation request while warmup. By default empty message is used.
- `sirocco_warmup_warmupInvocationData`: `String` typed environment variable to be used for discovering specific warmup invocation data configuration of Lambda functions to warmup.
- `sirocco.warmup.disableWarmupInvocationDataDiscovery`: `Boolean` typed property that disables discovery mechanism for warmup invocation data configurations specified by `com.opsgenie.sirocco.warmup.WarmupHandler#INVOCATION_DATA_ENV_VAR_NAME`. Default value is `false`.
- `sirocco_warmup_warmupInvocationCount`: `Integer` typed environment variable to be used for discovering specific warmup invocation count configuration of Lambda functions to warmup.
- `sirocco.warmup.disableWarmupInvocationCountDiscovery`: `Boolean` typed property that disables discovery mechanism for warmup invocation count configurations specified by `com.opsgenie.sirocco.warmup.WarmupHandler#INVOCATION_COUNT_ENV_VAR_NAME`.
- `sirocco.warmup.disableAliasDiscovery`: `Boolean` typed property that disables alias discovery mechanism to be used as qualifier while invoking Lambda functions to warmup. When alias discovery mechanism is active (active by default), alias with the latest version number is used as qualifier on invocation. Default value is `false`.

### Configurations of StandardWarmupStrategy

- `sirocco.warmup.invocationCount`: `Integer` typed property that configures the invocation count for each Lambda function to warmup. Note that if invocation counts are randomized, this value is used as upper limit of randomly generated invocation count. Default value is `8`.
- `sirocco.warmup.invocationResultConsumerCount`: `Integer` typed property that configures the count of consumers to get results of warmup invocations. The default value is two times of available CPU processors.
- `sirocco.warmup.iterationCount`: `Integer` typed property that configures the warmup iteration count. Default value is `2`.
- `sirocco.warmup.enableSplitIterations`: `Boolean` typed property that enables splitting iterations between multiple schedules of this handler and at each schedule call only one iteration is performed. Default value is `false`.
- `sirocco.warmup.randomizationBypassInterval`: `Long` typed property that configures the time interval in milliseconds to bypass randomization and directly use invocation count. Default value is `1.800.000` milliseconds (`30` minutes).
- `sirocco.warmup.disableRandomization`: `Boolean` typed property that disables randomized invocation count behaviour. Note that invocations counts are randomized for preventing full load on AWS Lambda all the warmup time to leave some AWS Lambda containers free/available for real requests and simulating real environment as much as possible. Default value is `false`.
- `sirocco.warmup.warmupFunctionAlias`: `String` typed property that configures alias to be used as qualifier while invoking Lambda functions to warmup.
- `sirocco.warmup.throwErrorOnFailure`: `Boolean` typed property that enables throwing error behaviour if the warmup invocation fails for some reason. Default value is `false`.
- `sirocco.warmup.dontWaitBetweenInvocationRounds`: `Boolean` typed property that disables waiting behaviour between each warmup invocation round. Default value is `false`.

### Configurations of StatAwareWarmupStrategy

- `sirocco.warmup.functionInstanceIdleTime`: `Long` typed property that configures the passed time in milliseconds to consider a Lambda function is idle. Default value is `1.800.000` milliseconds (`30` minutes).

- `sirocco.warmup.warmupScaleFactor`: `Float` typed property that configures scale factor to increase/decrease Lambda invocation count according to its stat (it is hot or not). Default value is `2.0`.


## Sample Usages

Installation steps:
- Deploy warmup handler as Lambda function with `com.opsgenie.sirocco.warmup.WarmupHandler` handler configuration.
- Add **CloudWatch** scheduled event as trigger to warmup handler (ex. every 5 minutes).
- Add `sirocco_warmup_warmupAware` environment variable with `true` value to the functions which will be warmed-up.

Here are the sample Lambda functions to be warmed-up:

### Java

``` java

```

### NodeJS

``` javascript
function checkAndHandleWarmupRequest(event, callback) {
    // Check whether it is empty request which is used as default warmup request
    if (Object.keys(event).length === 0) {
        console.log("Received warmup request as empty message. " + 
                    "Handling with 100 milliseconds delay ...");
        setTimeout(function() {
            callback(null);
        }, 100);
        return true;
    } else {
        var isString = (typeof event === 'string' || event instanceof String);
        if (isString) {
            // Check whether it is warmup request 
            if (event.startsWith('#warmup')) {
                var delayTime = 100;
                var args = event.substring('#warmup'.length).trim().split(/\s+/);
                // Iterate over all warmup arguments
                for (let arg of args) {
                    var argParts = arg.split('=');
                    // Check whether argument is in key=value format
                    if (argParts.length == 2) {
                        var argName = argParts[0];
                        var argValue = argParts[1];
                        // Check whether argument is "wait" argument 
                        // which specifies extra wait time before returning from request
                        if (argName === 'wait') {
                            var waitTime = parseInt(argValue);
                            delayTime += waitTime;
                        }
                    }
                }
                console.log("Received warmup request as warmup message. " + 
                            "Handling with " + delayTime + " milliseconds delay ...");
                setTimeout(function() {
                    callback(null);
                }, delayTime);
                return true;
            }
       } 
       return false;
    }   
}

exports.handler = (event, context, callback) => {
    // Check whether it is warmup request
    // Handle warmup request if it is warmup message
    if (!checkAndHandleWarmupRequest(event, callback)) {
        // TODO implement
        callback(null, 'Hello from Lambda');
    }      
};
```

### Python

``` python
import time

def isEmpty(obj):
    return (not obj)

def lambda_handler(event, context):
    if isEmpty(event):
        time.sleep(0.1)
        return ''
    else:    
        # TODO implement
        return 'Hello from Lambda'
```
