# JediSQL
Support SQL on redis with redisql module,this library extend redisql api on jedis source code,add all redisql command as java method 

**Based on [Jedis](https://github.com/xetorthio/jedis), [jedis-sql](https://github.com/xiao321/jedis-sql) and [JRediSQL](https://github.com/RedBeardLab/JRediSQL)**

```
        //use pipeline
        Pipeline pipe = jediSQL.pipelined();
        //create a db by redisql
        Response<String> rs1 = pipe.create_db("DB");
        pipe.sync();
        System.out.println(rs1.get());
        //create a table by redisql
        Response<List<Object>> rs2 = pipe.exec("DB", "CREATE TABLE cats(key TEXT, DSDS TEXT)");
        pipe.sync();
        System.out.println(rs2.get());
        long start = System.currentTimeMillis();
        List<Response<List<Object>>> responses = new ArrayList<>();
        // insert 8000 hash table into redis table name such as "cats:132131"
        for (int i = 0; i < 8000; i++) {
            Response<List<Object>> rs = pipe.exec("DB", "INSERT INTO cats VALUES('cats:" + i + "','AAA')");
            responses.add(rs);
        }
        pipe.sync();
        Response<List<Object>> rss = pipe.exec("DB", "SELECT * FROM cats");
        pipe.sync();
        System.out.println(rss.get());
        
```

```
//the print result may like blow
OK
[DONE]
[cats:0, AAA]
[cats:1, AAA]
[cats:2, AAA]
[cats:3, AAA]
```
It takes short time than not use pipeline to insert when you insert a lot of records
