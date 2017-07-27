package com.opsgenie.sirocco.api.control;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

/**
 * @author serkan
 */
public class ControlRequestBuilderTest {

    @Test
    public void shouldBuildControlRequestSuccessfully() {
        String contolRequest =
                new ControlRequestBuilder().
                        controlRequestType("test-control-request").
                        controlRequestArgument("arg1", "value1").
                        controlRequestArgument("arg2", "value2").
                        controlRequestProperty("prop3", "value3").
                        controlRequestProperty("prop4", "value4").
                    build();

        Assert.assertThat(
                contolRequest,
                is("\"#test-control-request arg1=value1 arg2=value2 -prop3=value3 -prop4=value4\""));
    }

}
