package com.example.expensetracker;

/**
 * 应用常量定义
 */
public class Constants {
    // Broadcast Actions
    public static final String ACTION_TRANSACTION_ADDED = "com.example.expensetracker.TRANSACTION_ADDED";
    public static final String ACTION_DATA_UPDATED = "com.example.expensetracker.DATA_UPDATED";
    public static final String ACTION_DAILY_REMINDER = "com.example.expensetracker.DAILY_REMINDER";

    // Notification Channels
    public static final String CHANNEL_ID_TRANSACTION = "transaction_channel";


    public static final String CHANNEL_ID_REMINDER = "expense_reminder_channel";
    public static final String CHANNEL_NAME_REMINDER = "记账提醒";

    // Intent Actions
    public static final String ACTION_SHOW_ADD_DIALOG = "show_add_dialog";
    // Notification IDs
    public static final int NOTIFICATION_ID_REMINDER = 1001;

    public static final int NOTIFICATION_ID_DAILY_REMINDER = 1002;

    // Intent Extras
    public static final String EXTRA_TRANSACTION_ID = "transaction_id";
    public static final String EXTRA_TRANSACTION_AMOUNT = "transaction_amount";
    public static final String EXTRA_TRANSACTION_CATEGORY = "transaction_category";

    // Service Actions
    public static final String ACTION_START_SERVICE = "start_service";
    public static final String ACTION_STOP_SERVICE = "stop_service";

    // Provider Authority
    public static final String PROVIDER_AUTHORITY = "com.example.expensetracker.provider";
    // Broadcast Actions

}