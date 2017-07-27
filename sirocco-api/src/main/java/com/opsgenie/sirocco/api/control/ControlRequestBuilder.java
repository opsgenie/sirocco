package com.opsgenie.sirocco.api.control;

import com.opsgenie.core.util.ExceptionUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds control request by using <b>Builder pattern</b> approach.
 *
 * @author serkan
 */
public final class ControlRequestBuilder {

    private String controlRequestType;
    private Map<String, String> controlRequestArguments;
    private Map<String, String> controlRequestProperties;

    public ControlRequestBuilder() {
    }

    public ControlRequestBuilder controlRequestType(String controlRequestType) {
        this.controlRequestType = controlRequestType;
        return this;
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ExceptionUtil.sneakyThrow(e);
            return value;
        }
    }

    /**
     * Adds the given property to the control request.
     * <p>
     *     Properties are specific to control requests and
     *     they are interpreted by the target control request handler specifically.
     * </p>
     *
     * @param propName  name of the property
     * @param propValue value of the property
     * @return this {@link ControlRequestBuilder builder}
     */
    public ControlRequestBuilder controlRequestProperty(String propName, Object propValue) {
        if (propName.charAt(0) == ControlRequestConstants.PROPERTY_PREFIX) {
            throw new IllegalArgumentException(
                    String.format("Only control request property can start with '%c'", ControlRequestConstants.PROPERTY_PREFIX));
        }
        propName = ControlRequestConstants.PROPERTY_PREFIX + propName;
        if (propName.matches("\\s+")) {
            throw new IllegalArgumentException("Property name cannot contain any white space character");
        }
        if (controlRequestProperties == null) {
            controlRequestProperties = new LinkedHashMap<String, String>();
        }
        String encodedPropValue = encodeValue(propValue.toString());
        controlRequestProperties.put(propName, encodedPropValue);
        return this;
    }

    /**
     * Adds the given argument to the control request.
     * <p>
     *     Arguments are not specific to control requests.
     *     They are used in general independently from control request type.
     * </p>
     *
     * @param argName  name of the argument
     * @param argValue value of the argument
     * @return this {@link ControlRequestBuilder builder}
     */
    public ControlRequestBuilder controlRequestArgument(String argName, Object argValue) {
        if (argName.charAt(0) == ControlRequestConstants.PROPERTY_PREFIX) {
            throw new IllegalArgumentException(
                    String.format("Only control request argument can start with '%c'", ControlRequestConstants.PROPERTY_PREFIX));
        }
        if (argName.matches("\\s+")) {
            throw new IllegalArgumentException("Argument name cannot contain any white space character");
        }
        if (controlRequestArguments == null) {
            controlRequestArguments = new LinkedHashMap<String, String>();
        }
        String encodedArgValue = encodeValue(argValue.toString());
        controlRequestArguments.put(argName, encodedArgValue);
        return this;
    }

    /**
     * Builds control request.
     *
     * @return the built control request
     */
    public String build() {
        StringBuilder request = new StringBuilder(ControlRequestConstants.CONTROL_REQUEST_PREFIX + controlRequestType);
        if (controlRequestArguments != null) {
            for (Map.Entry<String, String> entry : controlRequestArguments.entrySet()) {
                String argName = entry.getKey();
                String argValue = entry.getValue().toString();
                request.append(" ").append(argName).append("=").append(argValue);
            }
        }
        if (controlRequestProperties != null) {
            for (Map.Entry<String, String> entry : controlRequestProperties.entrySet()) {
                String propName = entry.getKey();
                String propValue = entry.getValue().toString();
                request.append(" ").append(propName).append("=").append(propValue);
            }
        }
        return "\"" + request.toString().trim() + "\"";
    }

}
