package com.tuc.distributed.worker.mapreduce;

import java.util.List;

public interface Mapper {
    List<KeyValue> map(String line);
}
