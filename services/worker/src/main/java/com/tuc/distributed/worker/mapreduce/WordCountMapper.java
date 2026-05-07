package com.tuc.distributed.worker.mapreduce;

import java.util.ArrayList;
import java.util.List;

public class WordCountMapper implements Mapper {

    @Override
    public List<KeyValue> map(String line) {
        List<KeyValue> output = new ArrayList<>();
        if (line == null || line.isBlank()) {
            return output;
        }

        String[] parts = line.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");

        for (String part : parts) {
            if (!part.isBlank()) {
                output.add(new KeyValue(part, "1"));
            }
        }
        return output;
    }
}
