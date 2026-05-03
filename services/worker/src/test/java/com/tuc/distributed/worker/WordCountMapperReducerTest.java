package com.tuc.distributed.worker;

import com.tuc.distributed.worker.mapreduce.KeyValue;
import com.tuc.distributed.worker.mapreduce.WordCountMapper;
import com.tuc.distributed.worker.mapreduce.WordCountReducer;
import com.tuc.distributed.worker.service.PartitionService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordCountMapperReducerTest {

    @Test
    void mapperShouldTokenizeAndEmitOnes() {
        WordCountMapper mapper = new WordCountMapper();
        List<KeyValue> result = mapper.map("Cat dog, cat!");

        assertEquals(3, result.size());
        assertEquals(new KeyValue("cat", "1"), result.get(0));
        assertEquals(new KeyValue("dog", "1"), result.get(1));
        assertEquals(new KeyValue("cat", "1"), result.get(2));
    }

    @Test
    void reducerShouldSumValues() {
        WordCountReducer reducer = new WordCountReducer();
        String result = reducer.reduce("cat", List.of("1", "1", "1"));
        assertEquals("cat\t3", result);
    }

    @Test
    void partitionShouldStayWithinBounds() {
        PartitionService partitionService = new PartitionService();
        int partition = partitionService.partitionOf("cat", 4);
        assertTrue(partition >= 0 && partition < 4);
    }
}
