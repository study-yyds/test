package com.example.expensetracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import java.util.Locale;

/**
 * 通知工具类
 * 封装所有通知相关逻辑，实现职责分离
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";  // 添加TAG常量
    private Context context;
    private NotificationManager notificationManager;

    // 添加通知ID常量
    private static final int NOTIFICATION_ID_TRANSACTION = 1001;
    private static final int NOTIFICATION_ID_REMINDER = 1002;

    // 添加通知渠道ID常量
    private static final String CHANNEL_ID_TRANSACTION = "transaction_channel";
    private static final String CHANNEL_NAME_TRANSACTION = "交易通知";

    public NotificationHelper(Context context) {
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    /**
     * 创建通知渠道（Android 8.0+ 必需）
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 创建记账提醒通知渠道
            NotificationChannel reminderChannel = new NotificationChannel(
                    Constants.CHANNEL_ID_REMINDER,
                    Constants.CHANNEL_NAME_REMINDER,
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("记账提醒通知");
            reminderChannel.enableLights(true);
            reminderChannel.setLightColor(0xFF03DAC5);
            reminderChannel.enableVibration(true);
            reminderChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
            notificationManager.createNotificationChannel(reminderChannel);

            // 创建交易通知渠道
            NotificationChannel transactionChannel = new NotificationChannel(
                    CHANNEL_ID_TRANSACTION,
                    CHANNEL_NAME_TRANSACTION,
                    NotificationManager.IMPORTANCE_HIGH
            );
            transactionChannel.setDescription("交易成功通知");
            transactionChannel.enableLights(true);
            transactionChannel.setLightColor(0xFF4CAF50);
            transactionChannel.enableVibration(true);
            transactionChannel.setVibrationPattern(new long[]{0, 200, 100, 200});
            notificationManager.createNotificationChannel(transactionChannel);
        }
    }

    /**
     * 发送记账提醒通知
     */
    public void sendExpenseReminderNotification() {
        try {
            // 检查通知权限（Android 13+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!notificationManager.areNotificationsEnabled()) {
                    Log.w(TAG, "通知权限未开启");
                    return;
                }
            }

            // 1. 创建Intent
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.ACTION_SHOW_ADD_DIALOG, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // 2. 创建PendingIntent
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, flags
            );

            // 3. 获取小图标
            int smallIcon = context.getResources().getIdentifier(
                    "ic_launcher_foreground", "drawable", context.getPackageName()
            );
            if (smallIcon == 0) {
                smallIcon = android.R.drawable.ic_dialog_info;
            }

            // 4. 构建通知
            Notification notification = new NotificationCompat.Builder(context, Constants.CHANNEL_ID_REMINDER)
                    .setSmallIcon(smallIcon)
                    .setContentTitle("记账提醒")
                    .setContentText("今天还没有记录支出，点击立即记账")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            // 5. 发送通知
            notificationManager.notify(Constants.NOTIFICATION_ID_REMINDER, notification);
            Log.d(TAG, "记账提醒通知已发送");

        } catch (SecurityException e) {
            Log.e(TAG, "发送通知权限不足", e);
        } catch (Exception e) {
            Log.e(TAG, "发送记账提醒通知失败", e);
        }
    }

    /**
     * 发送交易成功通知
     */
    public void sendTransactionNotification(String category, double amount) {
        try {
            // 检查通知权限（Android 13+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!notificationManager.areNotificationsEnabled()) {
                    Log.w(TAG, "通知权限未开启");
                    return;
                }
            }

            // 创建通知内容
            String title = "💸 记账成功";
            String content = String.format(Locale.getDefault(),
                    "您刚刚记录了：%s ¥%.2f", category, amount);

            // 创建PendingIntent
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(Constants.ACTION_SHOW_ADD_DIALOG, true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent, flags
            );

            // 获取小图标
            int smallIcon = context.getResources().getIdentifier(
                    "ic_launcher_foreground", "drawable", context.getPackageName()
            );
            if (smallIcon == 0) {
                smallIcon = android.R.drawable.ic_dialog_info;
            }

            // 构建通知
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_TRANSACTION)
                    .setSmallIcon(smallIcon)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE);

            // 显示通知
            notificationManager.notify(NOTIFICATION_ID_TRANSACTION, builder.build());
            Log.d(TAG, "交易通知已发送: " + content);

        } catch (SecurityException e) {
            Log.e(TAG, "发送通知权限不足", e);
        } catch (Exception e) {
            Log.e(TAG, "发送交易通知失败", e);
        }
    }

    /**
     * 检查通知渠道是否已创建
     */
    public boolean isNotificationChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(Constants.CHANNEL_ID_REMINDER);
            return channel != null;
        }
        return true;
    }
}