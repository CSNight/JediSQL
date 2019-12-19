package com.csnight.jedisql;

import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        JediSQL jedis = new JediSQL("39.97.255.100", 6779);
        jedis.connect();
        JedisMonitor jm = new JedisMonitor() {
            @Override
            public void onCommand(String command) {
                System.out.println(command);
            }
        };
        new Thread(() -> {
            jedis.monitor(jm);
        }).start();
        Scanner lll = new Scanner(System.in);
        System.out.println("请输入第一串字符：");
        String firStr = lll.next();
        if (firStr.equals("b")) {
            jedis.unmonitor(jm);

        }
    }
}
