package com.tuc.distributed.worker.mapreduce;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MapperRegistry {

    private final Map<String, Mapper> mappers = Map.of(
            "WordCountMapper", new WordCountMapper()
    );

    public Mapper get(String name) {
        Mapper mapper = mappers.get(name);
        if (mapper == null) {
            throw new IllegalArgumentException("Unsupported mapper: " + name);
        }
        return mapper;
    }
}
