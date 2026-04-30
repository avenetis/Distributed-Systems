package com.mapreduce.manager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.mapreduce.manager.entity.Worker;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.entity.WorkerStatus;

@Repository
public interface  WorkerRepository extends  JpaRepository<Worker, String> {
    List<Worker> findByStatus(WorkerStatus status);

    @Query("SELECT w FROM Worker w WHERE w.status IN :statuses AND w.lastHeartbeat < :timeout")
        List<Worker> findStaleWorkers(@Param("statuses") List<WorkerStatus> statuses, @Param("timeout") LocalDateTime timeout);

        @Modifying
    @Transactional
    @Query("UPDATE Worker w SET w.status = :status, w.currentTaskId = :taskId, w.lastHeartbeat = CURRENT_TIMESTAMP WHERE w.id = :workerId")
    int updateWorkerStatus(@Param("workerId") String workerId, 
                           @Param("status") WorkerStatus status, 
                           @Param("taskId") String taskId);
    
    @Modifying
    @Transactional
    @Query("UPDATE Worker w SET w.lastHeartbeat = CURRENT_TIMESTAMP, w.status = :status WHERE w.id = :workerId")
    void updateHeartbeat(@Param("workerId") String workerId, @Param("status") WorkerStatus status);
    
    Optional<Worker> findFirstByStatusOrderByLastHeartbeatAsc(WorkerStatus status);

}
