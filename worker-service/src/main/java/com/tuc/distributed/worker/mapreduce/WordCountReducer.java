package com.tuc.distributed.worker.mapreduce;

import java.util.List;

public class WordCountReducer implements Reducer {

    @Override
    public String reduce(String key, List<String> values) {
        int sum = values.stream()
                .mapToInt(Integer::parseInt)
                .sum();
        return key + "\t" + sum;
    }
}
