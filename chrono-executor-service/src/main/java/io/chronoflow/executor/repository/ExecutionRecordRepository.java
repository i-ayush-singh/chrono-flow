package io.chronoflow.executor.repository;

import io.chronoflow.executor.entity.ExecutionRecord;
import io.chronoflow.executor.entity.ExecutionStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, String> {
    List<ExecutionRecord> findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            ExecutionStatus status,
            Instant now
    );
}
