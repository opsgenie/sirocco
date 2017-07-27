package com.opsgenie.sirocco.api.util;

/**
 * Utility class for Lambda related common stuffs.
 *
 * @author serkan
 */
public final class LambdaUtil {

    private LambdaUtil() {
    }

    /**
     * Gets the underlying region.
     *
     * @return the underlying region
     */
    public static String getRegion() {
        return System.getenv("AWS_REGION");
    }

    /**
     * Gets the application profile name.
     *
     * @return the application profile name
     */
    public static String getProfile() {
        return System.getProperty("opsgenie.profile", "default");
    }

    /**
     * Gets the environment variable value for Lambda environment.
     *
     * @param name name of the environment variable
     * @return the requested environment variable value
     */
    public static String getEnvVar(String name) {
        return System.getenv(name);
    }

}
