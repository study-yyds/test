package com.example.expensetracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 记账监控服务
 * 功能：
 * 1. 定时检查当天账目数据
 * 2. 模拟整理账目数据
 * 3. 检查是否需要提醒记账
 * 4. 后台输出运行日志
 */
public class ExpenseMonitorService extends Service {
    private static final String TAG = "ExpenseMonitorService";

    // 通知相关
    private static final String CHANNEL_ID = "ExpenseMonitorChannel";
    private static final int NOTIFICATION_ID = 1001;

    // 服务状态
    private boolean isRunning = false;
    private Handler handler;
    private Runnable monitoringRunnable;

    // 数据库帮助类
    private DatabaseHelper databaseHelper;

    // 广播相关
    public static final String ACTION_SERVICE_STATUS = "com.example.expensetracker.SERVICE_STATUS";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_MESSAGE = "message";

    // 本地Binder
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        ExpenseMonitorService getService() {
            return ExpenseMonitorService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "服务创建");

        // 初始化数据库帮助类
        databaseHelper = new DatabaseHelper(this);

        // 初始化Handler
        handler = new Handler();

        // 创建通知渠道（如果Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "服务启动命令");

        if (!isRunning) {
            startMonitoring();
            sendStatusBroadcast("服务已启动", "正在监控账目...");
        }

        // 如果服务被杀死，系统会尝试重新启动
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "服务销毁");

        stopMonitoring();
        sendStatusBroadcast("服务已停止", "后台监控已关闭");
    }

    /**
     * 开始监控任务
     */
    private void startMonitoring() {
        if (isRunning) {
            return;
        }

        isRunning = true;
        Log.d(TAG, "开始监控账目");

        // 创建监控任务
        monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // 执行监控任务
                    performMonitoringTasks();

                    // 10秒后再次执行
                    handler.postDelayed(this, 10000);
                }
            }
        };

        // 启动监控
        handler.post(monitoringRunnable);
    }

    /**
     * 停止监控任务
     */
    private void stopMonitoring() {
        isRunning = false;

        if (handler != null && monitoringRunnable != null) {
            handler.removeCallbacks(monitoringRunnable);
        }

        Log.d(TAG, "停止监控账目");
    }

    /**
     * 执行监控任务
     */
    private void performMonitoringTasks() {
        try {
            // 任务1：检查当天账目
            checkTodayExpenses();

            // 任务2：模拟整理账目数据
            organizeAccountData();

            // 任务3：检查是否需要提醒记账
            checkReminderNeeded();

        } catch (Exception e) {
            Log.e(TAG, "监控任务执行失败", e);
        }
    }

    /**
     * 检查当天账目
     */
    private void checkTodayExpenses() {
        try {
            // 获取当天日期
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new Date());

            // 查询当天记录
            String query = "SELECT COUNT(*) as count, " +
                    "SUM(CASE WHEN type = " + Transaction.TYPE_EXPENSE + " THEN amount ELSE 0 END) as total_expense, " +
                    "SUM(CASE WHEN type = " + Transaction.TYPE_INCOME + " THEN amount ELSE 0 END) as total_income " +
                    "FROM " + DatabaseHelper.TABLE_RECORDS + " " +
                    "WHERE date LIKE '" + today + "%'";

            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, null);

            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                double totalExpense = cursor.getDouble(1);
                double totalIncome = cursor.getDouble(2);

                Log.d(TAG, "当天账目统计 - 记录数: " + count +
                        ", 支出: ¥" + totalExpense +
                        ", 收入: ¥" + totalIncome);

                // 如果当天没有记录，发送提醒
                if (count == 0) {
                    Log.d(TAG, "今天还没有记账记录，可能需要提醒用户");
                }
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "检查当天账目失败", e);
        }
    }

    /**
     * 模拟整理账目数据
     */
    private void organizeAccountData() {
        try {
            // 获取总记录数
            int totalRecords = databaseHelper.getRecordCount();

            // 获取本月记录数
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String currentMonth = sdf.format(new Date());

            String monthQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RECORDS +
                    " WHERE date LIKE '" + currentMonth + "%'";

            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(monthQuery, null);
            int monthRecords = 0;
            if (cursor.moveToFirst()) {
                monthRecords = cursor.getInt(0);
            }
            cursor.close();

            Log.d(TAG, "账目整理 - 总记录: " + totalRecords + "条, 本月记录: " + monthRecords + "条");

        } catch (Exception e) {
            Log.e(TAG, "整理账目数据失败", e);
        }
    }

    /**
     * 检查是否需要提醒记账
     */
    private void checkReminderNeeded() {
        try {
            // 获取最后一条记录的时间
            String lastRecordQuery = "SELECT date FROM " + DatabaseHelper.TABLE_RECORDS +
                    " ORDER BY date DESC LIMIT 1";

            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(lastRecordQuery, null);

            if (cursor.moveToFirst()) {
                String lastDate = cursor.getString(0);
                Log.d(TAG, "最后记账时间: " + lastDate);

                // 这里可以添加更复杂的提醒逻辑
                // 例如：如果超过24小时没有记账，发送提醒
            } else {
                Log.d(TAG, "还没有任何记账记录，建议提醒用户开始记账");
            }

            cursor.close();

        } catch (Exception e) {
            Log.e(TAG, "检查提醒失败", e);
        }
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "记账监控服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("后台监控账目变化和提醒服务");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * 显示前台服务通知
     */
    private void showForegroundNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("记账监控服务")
                .setContentText("正在监控您的账目变化...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * 发送状态广播
     */
    private void sendStatusBroadcast(String status, String message) {
        Intent intent = new Intent(ACTION_SERVICE_STATUS);
        intent.putExtra(EXTRA_STATUS, status);
        intent.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }

    /**
     * 获取服务运行状态
     */
    public boolean isRunning() {
        return isRunning;
    }
}