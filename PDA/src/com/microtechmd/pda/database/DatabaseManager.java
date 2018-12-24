package com.microtechmd.pda.database;

import android.content.Context;

import com.litesuits.orm.LiteOrm;
import com.litesuits.orm.db.assit.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/12/29.
 */

public class DatabaseManager {
    private static LiteOrm liteOrm;

    private DatabaseManager(Context context) {
        liteOrm = LiteOrm.newSingleInstance(context, "pda.db");
    }

    public static DatabaseManager getInstance(Context context) {
        return new DatabaseManager(context);
    }

    public void close() {
        liteOrm.close();
    }

    /**
     * 插入一条记录
     *
     * @param t
     */
    public <T> long insert(T t) {
        return liteOrm.save(t);
    }

    /**
     * 插入所有记录
     *
     * @param list
     */
    public <T> void insertAll(List<T> list) {
        liteOrm.save(list);
    }

    /**
     * 查询所有
     *
     * @param cla
     * @return
     */
    public <T> List<T> getQueryAll(Class<T> cla) {
        return liteOrm.query(cla);
    }

    /**
     * 查询所有指定列
     *
     * @param cla
     * @return
     */
    public <T> List<T> getQueryAllByColumns(Class<T> cla) {
        long current = System.currentTimeMillis();
        return liteOrm.query(new QueryBuilder<>(cla)
//                        .whereEquals("event_type", 7)
//                        .whereAppendOr()
//                        .whereEquals("event_type", 8)
                        .whereGreaterThan("date_time", current - 60 * 60 * 24 * 30 * 1000L)
                        .whereAppendAnd()
                        .whereLessThan("date_time", current)
                        .columns(new String[]{"date_time", "event_type", "value"})
//                .appendOrderAscBy("date_time")
        );
    }

    /**
     * 查询  某字段 等于 Value的值
     *
     * @param cla
     * @param field
     * @param value
     * @return
     */
    public <T> List<T> getQueryByWhere(Class<T> cla, String field, String value) {
        return liteOrm.<T>query(new QueryBuilder(cla)
                .whereEquals(field, value));
    }

    /**
     * 查询  某字段 在某两者之间的值
     *
     * @param cla
     * @return
     */
    public <T> List<T> getQueryByDate(Class<T> cla, long date1, long date2) {
        return liteOrm.<T>query(new QueryBuilder(cla)
                .distinct(true)//去重
                .appendOrderAscBy("date_time")//升序
                .whereGreaterThan("date_time", date1 - 1)
                .whereAppendAnd()
                .whereLessThan("date_time", date2 + 1));
    }

    /**
     * 查询  某字段 等于 Value的值  可以指定从1-20，就是分页
     *
     * @param cla
     * @param field
     * @param value
     * @param start
     * @param length
     * @return
     */
    public <T> List<T> getQueryByWhereLength(Class<T> cla, String field, String[] value, int start, int length) {
        return liteOrm.<T>query(new QueryBuilder(cla).where(field + "=?", value).limit(start, length));
    }

    /**
     * 查询当前序列号最后一个数据
     *
     * @return
     */
    public <T> List<T> getAddressQueryLast(Class<T> cla, String adress) {
        if (liteOrm != null) {
            return liteOrm.<T>query(new QueryBuilder(cla)
                    .whereEquals("rf_address", adress)
                    .appendOrderDescBy("id")
                    .limit(0, 1));
        } else {
            return null;
        }
    }

    /**
     * 查询  最后一个数据
     *
     * @return
     */
    public <T> List<T> getQueryLast(Class<T> cla) {
        return liteOrm.<T>query(new QueryBuilder(cla).appendOrderDescBy("id").limit(0, 1));
    }

    /**
     * 删除一个数据
     *
     * @param t
     * @param <T>
     */
    public <T> void delete(T t) {
        liteOrm.delete(t);
    }

    /**
     * 删除一个表
     *
     * @param cla
     * @param <T>
     */
    public <T> void delete(Class<T> cla) {
        liteOrm.delete(cla);
    }

    /**
     * 删除集合中的数据
     *
     * @param list
     * @param <T>
     */
    public <T> void deleteList(List<T> list) {
        liteOrm.delete(list);
    }

    /**
     * 删除数据库
     */
    public void deleteDatabase() {
        liteOrm.deleteDatabase();
    }
}
