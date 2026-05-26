package com.tuc.distributed.worker.mapreduce;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ReducerRegistry {

    private final Map<String, Reducer> reducers = new HashMap<>();

    public ReducerRegistry() {
        reducers.put("WordCountReducer", new WordCountReducer());
        reducers.put("GrepReducer", new GrepReducer());
    }

    public Reducer get(String name) {
        Reducer reducer = reducers.get(name);
        if (reducer == null) {
            throw new IllegalArgumentException("Unsupported reducer: " + name);
        }
        return reducer;
    }
}
