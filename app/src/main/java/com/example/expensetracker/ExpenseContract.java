package com.example.expensetracker;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * ContentProvider合约类
 * 定义URI、MIME类型、表名、列名等常量
 * 教学重点：理解URI的设计和命名规范
 */
public class ExpenseContract {

    // ContentProvider的授权标识（必须唯一）
    public static final String AUTHORITY = "com.example.expensetracker.provider";

    // 基础URI
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // 交易记录表相关定义
    public static class TransactionEntry implements BaseColumns {
        // 表名
        public static final String TABLE_NAME = "transactions";

        // 访问该表的URI
        public static final Uri CONTENT_URI =
            Uri.withAppendedPath(BASE_CONTENT_URI, TABLE_NAME);

        // MIME类型
        public static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE_NAME;
        public static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE_NAME;

        // 列名（与DatabaseHelper保持一致）
        public static final String COLUMN_CATEGORY = "category";
        public static final String COLUMN_NOTE = "note";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_AMOUNT = "amount";

        // 查询参数
        public static final String QUERY_PARAM_LIMIT = "limit";
        public static final String QUERY_PARAM_ORDER_BY = "orderBy";
    }

    // 统计信息表（虚拟表，用于演示）
    public static class StatisticsEntry {
        public static final String PATH_STATISTICS = "statistics";
        public static final Uri CONTENT_URI =
            Uri.withAppendedPath(BASE_CONTENT_URI, PATH_STATISTICS);
    }
}