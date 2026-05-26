package com.tuc.distributed.worker.mapreduce;

import java.util.ArrayList;
import java.util.List;

public class GrepMapper implements Mapper {

    private static final String PATTERN = "mapreduce";

    @Override
    public List<KeyValue> map(String line) {
        List<KeyValue> result = new ArrayList<>();
        if (line != null && line.toLowerCase().contains(PATTERN)) {
            result.add(new KeyValue(line.trim(), "1"));
        }
        return result;
    }
}


