package com.tech.microservices.order_service.repository;
import com.tech.microservices.order_service.model.OutboxEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface OutboxEventRepository extends JpaRepository<OutboxEvents, UUID> ,JpaSpecificationExecutor<OutboxEvents> {

    List<OutboxEvents> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status, java.time.LocalDateTime nextAttemptAt);

    /**
     * 查询 Outbox 发布监控快照，将多条记录汇总为一行。
     *
     * <p>返回指标：
     * <ul>
     *   <li>failedCount：当前 status = FAILED 的消息数量。</li>
     *   <li>pendingCount：当前 status = PENDING 的消息数量。</li>
     *   <li>oldestPendingAgeSeconds：最老 PENDING 消息的等待时长，单位为秒。</li>
     * </ul>
     *
     * <p>数量统计规则：
     * CASE 匹配状态时返回 1，不匹配时默认返回 NULL。
     * COUNT(表达式) 只统计非 NULL 值，因此只计算符合条件的记录。
     * 不要添加 ELSE 0，因为 COUNT 也会把 0 统计进去。
     *
     * <p>最老待发布消息的等待时间，按以下顺序计算：
     * <ol>
     *   <li>CASE：只保留 PENDING 记录的 created_at，其他记录返回 NULL。</li>
     *   <li>MIN：忽略 NULL，找到最早的待发布消息创建时间。</li>
     *   <li>TIMESTAMPDIFF：计算数据库当前时间减去创建时间，单位为秒。</li>
     *   <li>GREATEST(结果, 0)：避免返回负数。</li>
     *   <li>COALESCE(结果, 0)：没有 PENDING 时，将 NULL 转换为 0。</li>
     * </ol>
     *
     * <p>注意：
     * FAILED 表示消息发布重试耗尽，不表示库存释放等业务操作失败。
     * 等待时长从 created_at 开始计算，不会因为重试而重新计时。
     * SQL 列别名与 MonitoringSnapshot 中对应的 getter 属性名保持一致。
     *
     * @return 当前发布失败数、待发布数及最老待发布消息的等待秒数
     */
    @Query(value = """
    SELECT
        COUNT(CASE WHEN status = 'FAILED' THEN 1 END) AS failedCount,
        COUNT(CASE WHEN status = 'PENDING' THEN 1 END) AS pendingCount,
        COALESCE(
            GREATEST(
                TIMESTAMPDIFF(
                    SECOND,
                    MIN(CASE WHEN status = 'PENDING' THEN created_at END),
                    CURRENT_TIMESTAMP
                ),
                0
            ),
            0
        ) AS oldestPendingAgeSeconds
    FROM outbox_events
    WHERE status IN ('FAILED', 'PENDING')
    """, nativeQuery = true)
    @QueryHints(@QueryHint(name = "jakarta.persistence.query.timeout", value = "3000"))
    MonitoringSnapshot loadMonitoringSnapshot();

    /**
     * Outbox 监控统计查询的只读接口投影 ， 是和上边的loadMonitoringSnapshot（）返回值对应的接口，这种方式也叫嵌套借口，不需要单独声明
     * getter 属性名与 SQL 返回列的别名对应。
     */
    interface MonitoringSnapshot {
        long getFailedCount();
        long getPendingCount();
        long getOldestPendingAgeSeconds();
    }
}
