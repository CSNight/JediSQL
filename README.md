# JediSQL
Support SQL on redis with redisql module,this library extend redisql api on jedis source code,add all redisql command as java method 

This library provides all the features of jedis and redisql, and supports the use of redisql in cluster and pipeline environments

**Based on [Jedis](https://github.com/xetorthio/jedis), [jedis-sql](https://github.com/xiao321/jedis-sql) and [JRediSQL](https://github.com/RedBeardLab/JRediSQL)**

```
package com.supermap.context;

import com.csnight.jedisql.JediSQL;
import com.csnight.jedisql.Pipeline;
import com.csnight.jedisql.Response;
import com.supermap.redis.MultiRedisConnPoolST;
import com.supermap.utils.GUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class test {
    public static void main(String[] args) {
        MultiRedisConnPoolST st = MultiRedisConnPoolST.getInstance();
        st.setSt_pool_type("sin");
        st.setPort(6379);
        st.setServerIp("140.1.25.30");
        st.BuildJedisPool();
        for (int i = 0; i < 10; i++) {
            tests();
        }
    }

    public static void tests() {
        //Get a Redis connection from pool
        MultiRedisConnPoolST st = MultiRedisConnPoolST.getInstance();
        String guid = GUID.getUUID();
        JediSQL jediSQL = st.getJedis(guid);
        //clear db
        jediSQL.flushAll();
        String table1 = "TRAINS";
        //use pipeline
        Pipeline pipe = jediSQL.pipelined();
        //create a db by redisql
        Response<String> rs1 = pipe.create_db("DB");
        pipe.sync();
        System.out.println(rs1.get());
        //create a table by redisql
        Response<List<Object>> rs2 = pipe.exec("DB", "CREATE TABLE " + table1 + "(key TEXT, DSDS TEXT)");
        pipe.sync();
        System.out.println(rs2.get());
        long start = System.currentTimeMillis();
        pipe.create_statement_now("DB", "st", "INSERT INTO " + table1 + " VALUES(?1,?2)");
        pipe.sync();
        // insert 8000 hash table into redis table name such as "cats:132131"
        for (int i = 0; i < 1000000; i++) {
            pipe.exec_statement_now("DB", "st", "cats:" + i, "AAA");
        }
        pipe.sync();
        pipe.delete_statement("DB", "st");
        pipe.sync();
        long sync_insert = System.currentTimeMillis();
        System.out.println("Use insert command:" + (sync_insert - start) + "ms");
        start = System.currentTimeMillis();
        pipe.create_statement_now("DB", "std", "SELECT * FROM " + table1);
        pipe.sync();
        // insert 8000 hash table into redis table name such as "cats:132131"

        Response<List<Object>> res = pipe.exec_statement_now("DB", "std");

        pipe.sync();
        pipe.delete_statement("DB", "std");
        pipe.sync();
        List<Object> ss=res.get();
        sync_insert = System.currentTimeMillis();
        System.out.println("Use select command:" + (sync_insert - start) + "ms");
        st.close(guid);
    }
}

```

```
//the print result may like blow
OK
[DONE]
Use insert command:4923ms
Use select command:856ms
OK
[DONE]
Use insert command:4918ms
Use select command:901ms
OK
[DONE]
Use insert command:4891ms
Use select command:714ms
...
```
It takes short time than not use pipeline to insert when you insert a lot of records
