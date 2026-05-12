package com.example.expensetracker;

/**
 * 广播相关常量定义
 * 统一管理Action字符串，避免硬编码
 */
public class BroadcastConstants {
    // 自定义广播Action
    public static final String ACTION_TRANSACTION_ADDED = "com.example.expensetracker.ACTION_TRANSACTION_ADDED";
    public static final String ACTION_DATA_UPDATED = "com.example.expensetracker.ACTION_DATA_UPDATED";
    public static final String ACTION_BROADCAST_TEST = "com.example.expensetracker.ACTION_BROADCAST_TEST";

    // Intent Extra Keys
    public static final String EXTRA_TRANSACTION_COUNT = "transaction_count";
    public static final String EXTRA_TOTAL_AMOUNT = "total_amount";
    public static final String EXTRA_MESSAGE = "message";

    public static final String EXTRA_TOTAL_EXPENSE = "total_expense";
    public static final String EXTRA_TOTAL_INCOME = "total_income";
    public static final String EXTRA_NET_AMOUNT = "net_amount";

    // 广播类型
    public static final int BROADCAST_TYPE_NORMAL = 1;
    public static final int BROADCAST_TYPE_ORDERED = 2;
    public static final int BROADCAST_TYPE_STICKY = 3;
}