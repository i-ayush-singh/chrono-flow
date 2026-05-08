package io.chronoflow.scheduler.service;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.cronutils.model.definition.CronDefinitionBuilder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleIndexService {

    private static final String ZSET_KEY = "chronoflow:scheduler:jobs";
    private static final String META_KEY_PREFIX = "chronoflow:scheduler:job:";

    private final StringRedisTemplate redisTemplate;

    public void upsertJob(Long jobId, String tenantId, String cronExpression, String targetUrl) {
        Instant nextRun = computeNextRun(cronExpression);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("jobId", String.valueOf(jobId));
        metadata.put("tenantId", tenantId);
        metadata.put("cronExpression", cronExpression);
        metadata.put("targetUrl", targetUrl);

        redisTemplate.opsForHash().putAll(metaKey(jobId), metadata);
        redisTemplate.opsForZSet().add(ZSET_KEY, String.valueOf(jobId), nextRun.toEpochMilli());
        log.info("Indexed jobId={} nextRun={}", jobId, nextRun);
    }

    public Set<String> getDueJobIds(Instant now) {
        return redisTemplate.opsForZSet().rangeByScore(ZSET_KEY, 0, now.toEpochMilli());
    }

    public Map<Object, Object> getJobMetadata(String jobId) {
        return redisTemplate.opsForHash().entries(metaKey(Long.parseLong(jobId)));
    }

    public void reschedule(String jobId, String cronExpression) {
        Instant nextRun = computeNextRun(cronExpression);
        redisTemplate.opsForZSet().add(ZSET_KEY, jobId, nextRun.toEpochMilli());
    }

    private String metaKey(Long jobId) {
        return META_KEY_PREFIX + jobId;
    }

    private Instant computeNextRun(String cronExpression) {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        Cron cron = parser.parse(cronExpression);
        cron.validate();

        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        return executionTime.nextExecution(now)
                .map(ZonedDateTime::toInstant)
                .orElseThrow(() -> new IllegalArgumentException("Cannot compute next run for cron: " + cronExpression));
    }
}
