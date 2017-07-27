package com.opsgenie.sirocco.warmup.strategy.impl;

import com.opsgenie.core.instance.InstanceDiscovery;
import com.opsgenie.sirocco.warmup.strategy.WarmupStrategy;
import com.opsgenie.sirocco.warmup.strategy.WarmupStrategyProvider;

import java.util.List;

/**
 * Standard/default {@link WarmupStrategyProvider} implementation
 * which provides {@link WarmupStrategy}s by
 * <b>instance discovery</b> (through {@link InstanceDiscovery}) mechanism .
 *
 * @author serkan
 */
public class StandardWarmupStrategyProvider implements WarmupStrategyProvider {

    private static final List<WarmupStrategy> WARMUP_STRATEGIES =
            InstanceDiscovery.instancesOf(WarmupStrategy.class);

    @Override
    public WarmupStrategy getWarmupStrategy(String warmupStrategyName) {
        for (WarmupStrategy ws : WARMUP_STRATEGIES) {
            if (ws.getName().equals(warmupStrategyName)) {
                return ws;
            }
        }
        return null;
    }

}
