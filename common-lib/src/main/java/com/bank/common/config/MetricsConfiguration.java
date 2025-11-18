package com.bank.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ComponentScan(basePackages = "com.bank.common.metrics")
public class MetricsConfiguration {
}
