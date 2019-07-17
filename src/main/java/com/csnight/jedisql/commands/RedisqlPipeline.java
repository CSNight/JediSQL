package com.csnight.jedisql.commands;

import com.csnight.jedisql.Response;

import java.util.List;

public interface RedisqlPipeline {
    Response<String> create_db(String db);

    Response<String> create_db(String db, String path);

    Response<List<Object>> exec(String db, String sql);

    Response<List<Object>> exec_now(String db, String query);

    Response<List<Object>> query(String db, String query);

    Response<List<Object>> query_now(String db, String query);

    Response<List<Object>> query_into(String stream, String db, String query);

    Response<List<Object>> query_into_now(String stream, String db, String query);

    Response<String> create_statement(String db, String stmt_name, String stmt_query);

    Response<String> create_statement_now(String db, String stmt_name, String stmt_query);

    Response<List<Object>> exec_statement(String... args);

    Response<List<Object>> exec_statement_now(String... args);

    Response<List<Object>> query_statement(String... args);

    Response<List<Object>> query_statement_now(String... args);

    Response<List<Object>> query_statement_into(String... args);

    Response<List<Object>> query_statement_into_now(String... args);

    Response<String> delete_statement(String db, String stmt_name);

    Response<String> delete_statement_now(String db, String stmt_name);

    Response<String> update_statement(String db, String stmt_name, String stmt_query);

    Response<String> update_statement_now(String db, String stmt_name, String stmt_query);

    Response<String> copy(String db1, String db2);

    Response<String> copy_now(String db1, String db2);

    Response<List<Object>> statistics();

    Response<String> version();
}
