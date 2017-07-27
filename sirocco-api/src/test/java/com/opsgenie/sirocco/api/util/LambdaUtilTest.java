package com.opsgenie.sirocco.api.util;

import com.opsgenie.core.util.ExceptionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;

/**
 * @author serkan
 */
public class LambdaUtilTest {

    private static final Map<String, String> theUnmodifiableEnvironment;
    private static final Field envField;

    static {
        try {
            Class processEnvClass = Class.forName("java.lang.ProcessEnvironment");
            envField = processEnvClass.getDeclaredField("theUnmodifiableEnvironment");
            envField.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(envField, envField.getModifiers() & ~Modifier.FINAL);
            theUnmodifiableEnvironment = (Map<String, String>) envField.get(null);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private static void setEnvironmentVariable(String key, String value) {
        try {
            Map<String, String> env = (Map<String, String>) envField.get(null);
            if (!(env instanceof ModifiableEnvironment)) {
                env = new ModifiableEnvironment();
                env.putAll(theUnmodifiableEnvironment);
                envField.set(null, env);
            }
            env.put(key, value);
        } catch (IllegalAccessException e) {
            ExceptionUtil.sneakyThrow(e);
        }
    }

    private static void resetEnvironmentVariables() {
        try {
            envField.set(null, theUnmodifiableEnvironment);
        } catch (IllegalAccessException e) {
            ExceptionUtil.sneakyThrow(e);
        }
    }

    private static class ModifiableEnvironment extends HashMap {
    }

    @Test
    public void shouldProvidePropertiesSuccessfully() {
        System.setProperty("opsgenie.profile", "test");
        setEnvironmentVariable("AWS_REGION", "local");
        setEnvironmentVariable("test-env-variable-name", "test-env-variable-value");
        try {
            Assert.assertThat(LambdaUtil.getProfile(), is("test"));
            Assert.assertThat(LambdaUtil.getRegion(), is("local"));
            Assert.assertThat(LambdaUtil.getEnvVar("test-env-variable-name"), is("test-env-variable-value"));
        } finally {
            resetEnvironmentVariables();
        }
    }

}
