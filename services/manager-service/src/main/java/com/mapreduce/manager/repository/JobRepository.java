package com.mapreduce.manager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.JobStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    List<Job> findByUserId(String userId);
    List<Job> findByStatus(JobStatus status);
    List<Job> findByStatusIn(List<JobStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.createdAt < :timeout")
    List<Job> findStaleJobsByStatus(@Param("status") JobStatus status, @Param("timeout") LocalDateTime timeout);

    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.status = :status, j.errorMessage = :error WHERE j.id = :jobId")
    void updateJobStatus(@Param("jobId") String jobId, @Param("status") JobStatus status, @Param("error") String error);

    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.completedMappers = :completed, j.id = :jobId")
    void incrementCompletedMappers(@Param("jobId") String jobId, @Param("completed") Integer completed);

    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.completedReducers = :completed, j.id = :jobId")
    void incrementCompletedReducers(@Param("jobId") String jobId, @Param("completed") Integer completed);
}