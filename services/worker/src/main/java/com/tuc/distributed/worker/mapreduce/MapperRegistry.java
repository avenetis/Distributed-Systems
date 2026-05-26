package com.tuc.distributed.worker.mapreduce;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class MapperRegistry {

    private final Map<String, Mapper> mappers = new HashMap<>();

    public MapperRegistry() {
        mappers.put("WordCountMapper", new WordCountMapper());
        mappers.put("GrepMapper", new GrepMapper());
    }

    public Mapper get(String name) {
        Mapper mapper = mappers.get(name);
        if (mapper == null) {
            throw new IllegalArgumentException("Unsupported mapper: " + name);
        }
        return mapper;
    }
}
