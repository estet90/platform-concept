package ru.craftysoft.schemaregistry.util;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.SqlClient;
import io.vertx.mutiny.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import org.jooq.EnumType;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.conf.ParamType;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ru.craftysoft.schemaregistry.util.DbLoggerHelper.*;

@RequiredArgsConstructor
public class DbClient {

    private final PgPool pgPool;

    public <T> Uni<List<T>> executeBatch(Logger log, String point, String sql, List<Tuple> args, Function<Row, T> mapper) {
        return executeBatch(pgPool, log, point, sql, args, mapper);
    }

    public static <T> Uni<List<T>> executeBatch(SqlClient sqlClient, Logger log, String point, String sql, List<Tuple> args, Function<Row, T> mapper) {
        var queryId = UUID.randomUUID().toString();
        logIn(log, point, queryId, sql, args);
        return sqlClient.preparedQuery(sql).executeBatch(args)
                .onFailure().invoke(e -> logError(log, point, queryId, e))
                .map(rows -> {
                    var resultList = new ArrayList<T>();
                    var rowsSet = rows.value();
                    while (rowsSet != null) {
                        rowsSet.iterator().forEachRemaining(x -> resultList.add(mapper.apply(x)));
                        rowsSet = rowsSet.next();
                    }
                    logOutCount(log, point, queryId, resultList.size());
                    return resultList;
                });
    }

    public Uni<Integer> executeBatch(Logger log, String point, String sql, List<Tuple> args) {
        return executeBatch(pgPool, log, point, sql, args);
    }

    public static Uni<Integer> executeBatch(SqlClient sqlClient, Logger log, String point, String sql, List<Tuple> args) {
        var queryId = UUID.randomUUID().toString();
        logIn(log, point, queryId, sql, args);
        return sqlClient.preparedQuery(sql).executeBatch(args)
                .onFailure().invoke(e -> logError(log, point, queryId, e))
                .map(rows -> {
                    int count = 0;
                    var rowsSet = rows.value();
                    while (rowsSet != null) {
                        count++;
                        rowsSet = rowsSet.next();
                    }
                    logOutCount(log, point, queryId, count);
                    return count;
                });
    }

    public Uni<Integer> execute(Logger log, String point, Query query) {
        return execute(pgPool, log, point, query);
    }

    public static Uni<Integer> execute(SqlClient sqlClient, Logger log, String point, Query query) {
        var sql = extractSql(query);
        var args = extractArgs(query);
        return execute(sqlClient, log, point, sql, args);
    }

    public Uni<Integer> execute(Logger log, String point, String sql, Tuple args) {
        return execute(pgPool, log, point, sql, args);
    }

    public static Uni<Integer> execute(SqlClient sqlClient, Logger log, String point, String sql, Tuple args) {
        var queryId = UUID.randomUUID().toString();
        logIn(log, point, queryId, sql, args);
        return sqlClient.preparedQuery(sql).execute(args)
                .onFailure().invoke(e -> logError(log, point, queryId, e))
                .map(rows -> {
                    logOutCount(log, point, queryId, rows);
                    return rows.rowCount();
                });
    }

    public <T> Multi<T> toMulti(Logger log, String point, Query query, Function<Row, T> mapper) {
        return toMulti(pgPool, log, point, query, mapper);
    }

    public static <T> Multi<T> toMulti(SqlClient sqlClient, Logger log, String point, Query query, Function<Row, T> mapper) {
        var sql = extractSql(query);
        var args = extractArgs(query);
        return toMulti(sqlClient, log, point, sql, args, mapper);
    }

    public <T> Multi<T> toMulti(Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        return toMulti(pgPool, log, point, sql, args, mapper);
    }

    public static <T> Multi<T> toMulti(SqlClient sqlClient, Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        var queryId = UUID.randomUUID().toString();
        logIn(log, point, queryId, sql, args);
        return sqlClient.preparedQuery(sql).execute(args)
                .onFailure().invoke(e -> logError(log, point, queryId, e))
                .invoke(rows -> {
                    if (log.isDebugEnabled()) {
                        var result = new ArrayList<>();
                        rows.iterator().forEachRemaining(row -> result.add(mapper.apply(row)));
                        logOut(log, point, queryId, rows, result);
                    }
                })
                .toMulti()
                .flatMap(rows -> Multi.createFrom().iterable(rows))
                .map(mapper);
    }

    public <T> Uni<T> toUni(Logger log, String point, Query query, Function<Row, T> mapper) {
        return toUni(pgPool, log, point, query, mapper);
    }

    public static <T> Uni<T> toUni(SqlClient sqlClient, Logger log, String point, Query query, Function<Row, T> mapper) {
        var sql = extractSql(query);
        var args = extractArgs(query);
        return toUni(sqlClient, log, point, sql, args, mapper);
    }

    public <T> Uni<T> toUni(Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        return toUni(pgPool, log, point, sql, args, mapper);
    }

    public static <T> Uni<T> toUni(SqlClient sqlClient, Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        var queryId = UUID.randomUUID().toString();
        logIn(log, point, queryId, sql, args);
        return sqlClient.preparedQuery(sql).execute(args)
                .onFailure().invoke(e -> logError(log, point, queryId, e))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        logOut(log, point, queryId, rows, null);
                        return null;
                    }
                    var row = rows.iterator().next();
                    var result = mapper.apply(row);
                    logOut(log, point, queryId, rows, result);
                    return result;
                });
    }

    public <T> Uni<List<T>> toUniOfList(Logger log, String point, Query query, Function<Row, T> mapper) {
        return toUniOfList(pgPool, log, point, query, mapper);
    }

    public static <T> Uni<List<T>> toUniOfList(SqlClient sqlClient, Logger log, String point, Query query, Function<Row, T> mapper) {
        var sql = extractSql(query);
        var args = extractArgs(query);
        return toUniOfList(sqlClient, log, point, sql, args, mapper);
    }

    public <T> Uni<List<T>> toUniOfList(Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        return toUniOfList(pgPool, log, point, sql, args, mapper);
    }

    public static <T> Uni<List<T>> toUniOfList(SqlClient sqlClient, Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        return toUniOfCollection(sqlClient, log, point, sql, args, mapper, List::of, ArrayList::new);
    }

    public <T> Uni<Set<T>> toUniOfSet(Logger log, String point, Query query, Function<Row, T> mapper) {
        return toUniOfSet(pgPool, log, point, query, mapper);
    }

    public static <T> Uni<Set<T>> toUniOfSet(SqlClient sqlClient, Logger log, String point, Query query, Function<Row, T> mapper) {
        var sql = extractSql(query);
        var args = extractArgs(query);
        return toUniOfSet(sqlClient, log, point, sql, args, mapper);
    }

    public <T> Uni<Set<T>> toUniOfSet(Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        return toUniOfSet(pgPool, log, point, sql, args, mapper);
    }

    public static <T> Uni<Set<T>> toUniOfSet(SqlClient sqlClient, Logger log, String point, String sql, Tuple args, Function<Row, T> mapper) {
        return toUniOfCollection(sqlClient, log, point, sql, args, mapper, Set::of, HashSet::new);
    }

    public static <COLLECTION extends Collection<T>, T> Uni<COLLECTION> toUniOfCollection(SqlClient sqlClient,
                                                                                          Logger log,
                                                                                          String point,
                                                                                          String sql,
                                                                                          Tuple args,
                                                                                          Function<Row, T> mapper,
                                                                                          Supplier<COLLECTION> empty,
                                                                                          Supplier<COLLECTION> initializer) {
        var queryId = UUID.randomUUID().toString();
        logIn(log, point, queryId, sql, args);
        return sqlClient.preparedQuery(sql).execute(args)
                .onFailure().invoke(e -> logError(log, point, queryId, e))
                .map(rows -> {
                    if (rows.rowCount() == 0) {
                        logOut(log, point, queryId, rows, null);
                        return empty.get();
                    }
                    var result = initializer.get();
                    rows.iterator().forEachRemaining(row -> result.add(mapper.apply(row)));
                    logOut(log, point, queryId, rows, result);
                    return result;
                });
    }

    public <COLLECTION extends Collection<T>, T> Uni<COLLECTION> toUniOfCollection(Logger log,
                                                                                   String point,
                                                                                   Query query,
                                                                                   Function<Row, T> mapper,
                                                                                   Supplier<COLLECTION> empty,
                                                                                   Supplier<COLLECTION> initializer) {
        var sql = extractSql(query);
        var args = extractArgs(query);
        return toUniOfCollection(pgPool, log, point, sql, args, mapper, empty, initializer);
    }

    private static String extractSql(Query query) {
        var sql = query.getSQL(ParamType.NAMED).replaceAll("(?<!:):(?!:)", "\\$");
        for (var entry : query.getParams().entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            var dataType = value.getDataType();
            if (dataType.isArray()) {
                sql = sql.replace("$" + key + "::" + dataType.getCastTypeName() + "[]", "$" + key);
            }
        }
        return sql;
    }

    private static Tuple extractArgs(Query query) {
        var parameters = query.getBindValues().stream()
                .map(arg -> {
                    if (arg instanceof JSON json) {
                        return json.data();
                    }
                    if (arg instanceof JSONB jsonb) {
                        return jsonb.data();
                    }
                    if (arg instanceof EnumType enumeration) {
                        return enumeration.getLiteral();
                    }
                    return arg;
                })
                .collect(Collectors.toList());
        return Tuple.tuple(parameters);
    }

}
