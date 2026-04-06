package com.mapreduce.manager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.entity.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByJobId(String jobId);
    List<Task> findByJobIdAndType(String jobId, TaskType type);
    List<Task> findByStatus(TaskStatus status);

    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.type = :type")
    List<Task> findPendindTasksByType(@Param("status") TaskStatus status, @Param("type") TaskType type);

    @Query("SELECT t FROM Task t WHERE t.workerId = :workerId AND t.status IN :statuses")
    List<Task> findTasksByWorkerAndStatuses(@Param("workerId") String workerId, @Param("statuses") List<TaskStatus> statuses);

    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.assignedAt < :timeout")
    List<Task> findStaleAssignedTasks(@Param("status") TaskStatus status, @Param("timeout") LocalDateTime timeout);

    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = :status, t.startedAt = :startedAt WHERE t.id = :taskId")
    void startTask(@Param("taskId") String taskId, @Param("status") TaskStatus status, @Param("startedAt") LocalDateTime startedAt);

    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = :status, t.completedAt = :completedAt, t.outputLocation = :outputLocation WHERE t.id = :taskId")
    void updateTask(@Param("taskId") String taskId, @Param("status") TaskStatus status, @Param("completedAt") LocalDateTime completedAt, @Param("outputLocation") String outputLocation);

    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.status = :status, t.errorMessage = :errorMessage, t.retryCount = t.retryCount + 1 WHERE t.id = :taskId")
    void failTask(@Param("taskId") String taskId, @Param("status") TaskStatus status, @Param("errorMessage") String errorMessage);

    Optional<Task> findFirstByJobIdAndTypeOrderByPartitionIndexAsc(String jobId, TaskType type);
    
    long countByJobIdAndTypeAndStatus(String jobId, TaskType type, TaskStatus status);

}
