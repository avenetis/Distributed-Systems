package com.tuc.distributed.worker.mapreduce;

import java.util.List;

public interface Reducer {
    String reduce(String key, List<String> values);
}
