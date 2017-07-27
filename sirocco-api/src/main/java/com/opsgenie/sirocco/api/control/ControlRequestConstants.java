package com.opsgenie.sirocco.api.control;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Interface for defining and providing control request related constants.
 *
 * @author serkan
 */
public interface ControlRequestConstants {

    /**
     * Character to be added in front of control request messages as prefix.
     */
    char CONTROL_REQUEST_PREFIX = '#';
    /**
     * Character to be added in front of properties in the control request message as prefix.
     */
    char PROPERTY_PREFIX = '-';

    /**
     * Property name to define <b>wait</b> argument in the control request.
     * <b>Wait</b> argument is used to wait/sleep before the control request message is handled.
     */
    String WAIT_ARGUMENT = "wait";
    /**
     * Property name to define <b>instance id</b> argument in the control request.
     * <b>Instance id</b> argument is used by the target Lambda handler to check
     * whether this message is for itself.
     */
    String INSTANCE_ID_ARGUMENT = "instanceId";

    /**
     * {@link DateFormat Date format} to be used for formatting {@link java.util.Date} data
     * in the control response.
     */
    DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

}
