package com.example.expensetracker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;


import java.util.Locale;

/**
 * 记账应用广播接收器
 * 接收账目更新相关的广播
 *
 * 动态注册版本 - 适合教学项目
 *
 * 软件工程思想：观察者模式 - 接收器作为观察者，监听特定事件
 */
public class ExpenseBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "ExpenseBroadcastReceiver";

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "📻 接收到广播，Action: " + action);

        if (action == null) return;

        switch (action) {
            case BroadcastConstants.ACTION_TRANSACTION_ADDED:
                handleTransactionAdded(context, intent);
                break;

            case BroadcastConstants.ACTION_DATA_UPDATED:
                handleDataUpdated(context, intent);
                break;

            case BroadcastConstants.ACTION_BROADCAST_TEST:
                handleBroadcastTest(context, intent);
                break;

            default:
                Log.w(TAG, "未知的广播Action: " + action);
        }
    }

    /**
     * 处理交易添加广播
     * 场景：用户添加新交易后，通知其他组件更新
     */
    @SuppressLint("LongLogTag")
    private void handleTransactionAdded(Context context, Intent intent) {
        String category = intent.getStringExtra("category");
        double amount = intent.getDoubleExtra("amount", 0);
        String time = intent.getStringExtra("time");

        String message = String.format("💸 新增交易\n%s: ¥%.2f\n时间: %s",
                category, amount, time != null ? time : "刚刚");

        // 显示Toast提示
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        // 更新MainActivity的UI状态
        if (context instanceof MainActivity) {
            ((MainActivity) context).updateBroadcastStatus("新增" + category + "交易");
        }

        Log.d(TAG, "处理交易添加广播: " + message);
    }

    /**
     * 处理数据更新广播
     * 场景：手动点击"发送账目更新广播"按钮
     */
    @SuppressLint("LongLogTag")
    private void handleDataUpdated(Context context, Intent intent) {
        int transactionCount = intent.getIntExtra(BroadcastConstants.EXTRA_TRANSACTION_COUNT, 0);
        double totalExpense = intent.getDoubleExtra(BroadcastConstants.EXTRA_TOTAL_EXPENSE, 0.0);
        double totalIncome = intent.getDoubleExtra(BroadcastConstants.EXTRA_TOTAL_INCOME, 0.0);
        double netAmount = intent.getDoubleExtra(BroadcastConstants.EXTRA_NET_AMOUNT, 0.0);
        String customMessage = intent.getStringExtra(BroadcastConstants.EXTRA_MESSAGE);

        String message;
        if (customMessage != null) {
            message = customMessage;
        } else {
            message = String.format(Locale.getDefault(),
                    "📊 账目数据已更新\n" +
                            "当前共%d笔交易\n" +
                            "总支出: ¥%.2f\n" +
                            "总收入: ¥%.2f\n" +
                            "净额: ¥%.2f",
                    transactionCount, totalExpense, totalIncome, netAmount);
        }

        // 显示Toast提示
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

        // 更新MainActivity的UI状态
        if (context instanceof MainActivity) {
            ((MainActivity) context).updateBroadcastStatus("账目数据更新");
        }

        Log.d(TAG, "处理数据更新广播: " + message);
    }

    /**
     * 处理广播测试
     */
    @SuppressLint("LongLogTag")
    private void handleBroadcastTest(Context context, Intent intent) {
        String testMessage = intent.getStringExtra(BroadcastConstants.EXTRA_MESSAGE);
        String message = "📡 广播测试: " + (testMessage != null ? testMessage : "成功");

        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        Log.d(TAG, "处理广播测试: " + message);
    }
}