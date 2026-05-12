package com.example.expensetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 服务状态广播接收器
 * 用于接收服务状态更新并通知Activity
 */
public class ServiceStatusReceiver extends BroadcastReceiver {
    private static final String TAG = "ServiceStatusReceiver";

    // 回调接口
    public interface OnStatusUpdateListener {
        void onStatusUpdate(String status, String message);
    }

    private OnStatusUpdateListener listener;

    public void setOnStatusUpdateListener(OnStatusUpdateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
            intent.getAction().equals(ExpenseMonitorService.ACTION_SERVICE_STATUS)) {

            String status = intent.getStringExtra(ExpenseMonitorService.EXTRA_STATUS);
            String message = intent.getStringExtra(ExpenseMonitorService.EXTRA_MESSAGE);

            Log.d(TAG, "收到服务状态: " + status + " - " + message);

            if (listener != null) {
                listener.onStatusUpdate(status, message);
            }
        }
    }
}