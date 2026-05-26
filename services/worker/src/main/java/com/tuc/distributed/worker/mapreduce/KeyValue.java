package com.tuc.distributed.worker.mapreduce;

public record KeyValue(String key, String value) {
}


/*ublic class KeyValue {
    private final String key;
    private final String value;

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }
}*/