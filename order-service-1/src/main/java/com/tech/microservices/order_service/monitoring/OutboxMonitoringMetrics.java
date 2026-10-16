package com.tech.microservices.order_service.monitoring;


import com.tech.microservices.order_service.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

@Component
@ConditionalOnProperty(
        name = "outbox.monitor.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxMonitoringMetrics {

    private static final Logger log = LoggerFactory.getLogger(OutboxMonitoringMetrics.class);

    private final OutboxEventRepository repository;
    private volatile Snapshot snapshot = new Snapshot(Double.NaN, Double.NaN, Double.NaN, 0);
    private volatile double collectionSuccess;
    private boolean refreshFailed;

    /*
     * 注册“当前未解决的 Outbox 发布失败数量”指标。
     *
     * builder 的三个参数：
     * 1. outbox.failed.count：指标名称，不是数据库字段或配置项。
     * 2. this：被观察的 OutboxMonitoringMetrics 对象。
     * 3. Lambda：指标被读取时，从该对象的最新 snapshot 中取得失败数量。
     *
     * 数据更新与读取分离：
     * - refresh() 定时查询数据库并更新 snapshot。
     * - Gauge 读取内存快照，不在取值时执行 SQL。
     * - 观察 this 而不是直接观察 snapshot，是因为 refresh() 会替换快照对象。
     *
     * description：指标说明，不是告警规则。
     * register：组件创建时注册到 Micrometer，不代表立即推送到 Prometheus。
     * Prometheus 抓取指标时，通过取值函数读取当时的快照数值。
     */
    public OutboxMonitoringMetrics(OutboxEventRepository repository, MeterRegistry registry) {
        this.repository = repository;
        Gauge.builder("outbox.failed.count", this, m -> m.snapshot.failedCount())
                .description("Current unresolved outbox publishing failures")
                .register(registry);
        Gauge.builder("outbox.pending.count", this, m -> m.snapshot.pendingCount())
                .description("Current pending outbox messages")
                .register(registry);
        Gauge.builder("outbox.pending.oldest.age.seconds", this,
                        m -> m.snapshot.oldestPendingAgeSeconds())
                .description("Age in seconds of the oldest pending message")
                .register(registry);
        Gauge.builder("outbox.monitor.collection.success", this, m -> m.collectionSuccess)
                .description("Whether the latest database collection succeeded")
                .register(registry);
        Gauge.builder("outbox.monitor.last.success.timestamp.seconds", this,
                        m -> m.snapshot.lastSuccessTimestampSeconds())
                .description("Unix timestamp in seconds of the last successful collection")
                .register(registry);
    }

    @Scheduled(
            // 对应config/OutboxSchedulingConfig.java 中的 outboxMonitoringScheduler
            scheduler = "outboxMonitoringScheduler",
            fixedDelayString = "${outbox.monitor.fixed-delay-ms:15000}",
            initialDelayString = "${outbox.monitor.initial-delay-ms:1000}"
    )
    public void refresh() {
        try {
            var result = repository.loadMonitoringSnapshot();
            snapshot = new Snapshot(
                    result.getFailedCount(), result.getPendingCount(),
                    result.getOldestPendingAgeSeconds(), Instant.now().getEpochSecond()
            );
            collectionSuccess = 1;
            if (refreshFailed) {
                log.info("OUTBOX_MONITOR_REFRESH_RECOVERED");
            }
            refreshFailed = false;
        } catch (RuntimeException error) {
            collectionSuccess = 0;
            if (!refreshFailed) {
                log.error("OUTBOX_MONITOR_REFRESH_FAILED", error);
            }
            refreshFailed = true;
        }
    }

    private record Snapshot(
            double failedCount,
            double pendingCount,
            double oldestPendingAgeSeconds,
            double lastSuccessTimestampSeconds
    ) {}
}
