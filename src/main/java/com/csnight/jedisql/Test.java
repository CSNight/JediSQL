package com.csnight.jedisql;

import java.util.List;

public class Test {
    public static void main(String[] args) {
        JediSQL j = new JediSQL("140.1.25.4", 6379);
        j.connect();
        List<byte[]> list = j.command();
    }
}
