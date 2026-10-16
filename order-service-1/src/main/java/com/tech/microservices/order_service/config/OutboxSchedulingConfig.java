package com.tech.microservices.order_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration( proxyBeanMethods = false)
public class OutboxSchedulingConfig {

    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("application-scheduler-");
        return scheduler;
    }

    @Bean(name = "outboxMonitoringScheduler")
    public ThreadPoolTaskScheduler outboxMonitoringScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("outbox-monitor-");
        return scheduler;
    }
}
