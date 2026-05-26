package com.tuc.distributed.worker.mapreduce;

import java.util.List;

public class GrepReducer implements Reducer {

    @Override
    public String reduce(String key, List<String> values) {
        return key;
    }
}
