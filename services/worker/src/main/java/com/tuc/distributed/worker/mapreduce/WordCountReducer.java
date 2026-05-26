package com.tuc.distributed.worker.mapreduce;

import java.util.List;

public class WordCountReducer implements Reducer {

    @Override
    public String reduce(String key, List<String> values) {
        int sum = values.stream()//Παίρνει τη λίστα τιμών:["1", "1", "1"]
                .mapToInt(Integer::parseInt)//Οι τιμές είναι strings, επειδή το KeyValue έχει: String value Άρα μετατρέπονται από:"1", "1", "1" σε integers: 1, 1, 1
                .sum();//Αθροίζει τους αριθμούς:1 + 1 + 1 = 3
        return key + "\t" + sum;
    }
}
