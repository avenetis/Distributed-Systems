package com.mapreduce.manager.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.entity.TaskStatus;
import com.mapreduce.manager.repository.TaskRepository;
import com.mapreduce.manager.repository.TaskType;

import org.springframework.transaction.annotation.Transactional;

@Service
public class InputtSplitterService {
    private static final Logger log = LoggerFactory.getLogger(InputtSplitterService.class);
    private static final int DEFAULT_SPLIT_SIZE = 64 * 1024 * 1024; // 64mb
    private final TaskRepository taskRepository;


    public InputtSplitterService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public List<SplitInfo> splitInput(Job job) {
        log.info("Splitting input for job: {}, numMappers: {}", job.getId(), job.getNumMappers());

        List<SplitInfo> splits = new ArrayList<>();
        // In a real implementation, we would read the input file metadata to determine actual splits based on file size and format.
        // For this example, we will create dummy splits based on the number of mappers requested.
        for (int i = 0; i < job.getNumMappers(); i++) {
            SplitInfo split = new SplitInfo();
            split.setPartitionIndex(i);
            split.setInputPath(job.getInputPath() + "/split_" + i);
            split.setStartOffset(i * DEFAULT_SPLIT_SIZE);
            split.setEndOffset((i + 1) * DEFAULT_SPLIT_SIZE);
            splits.add(split);
            //create a mapper task for this split
            createMapperTask(job, split);
        }
        log.info("Created {} map tasks for job {}", splits.size(), job.getId());
        return splits;
    }

    // helper method to create a mapper task for a given split
    private void createMapperTask(Job job, SplitInfo split) {
        Task task = new Task();
        task.setJobId(job.getId());
        task.setType(TaskType.MAP);
        task.setStatus(TaskStatus.PENDING);
        task.setPartitionIndex(split.getPartitionIndex());
        task.setInputPath(split.getInputPath());
        task.setOutputLocation(job.getOutputPath() + "/map_output_" + split.getPartitionIndex());
        task.setRetryCount(0);
        taskRepository.save(task);

        log.debug("Created map task {} for partition {}", task.getId(), split.getPartitionIndex());
    }

    // simple dto to represent input split information 
    public static class SplitInfo {
        private int partitionIndex;
        private String inputPath;
        private long startOffset;
        private long endOffset;

        public int getPartitionIndex() {
            return partitionIndex;
        }

        public void setPartitionIndex(int partitionIndex) {
            this.partitionIndex = partitionIndex;
        }

        public String getInputPath() {
            return inputPath;
        }

        public void setInputPath(String inputPath) {
            this.inputPath = inputPath;
        }

        public long getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        public long getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(long endOffset) {
            this.endOffset = endOffset;
        }
    } 

}
