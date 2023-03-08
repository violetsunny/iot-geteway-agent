package top.iot.gateway.component.common.micrometer;

import io.micrometer.core.instrument.MeterRegistry;

public interface MeterRegistrySupplier {

    MeterRegistry getMeterRegistry(String metric, String... tagKeys);

}
