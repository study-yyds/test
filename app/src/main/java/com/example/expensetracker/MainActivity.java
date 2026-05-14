package com.example.expensetracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.IntentFilter;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.ContentValues;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // UI组件
    private TextView tvTotalAmount;
    private TextView tvExpense;
    private TextView tvIncome;
    private TextView tvMonthSummary;
    private TextView tvRecordCount;
    private TextView tvLifecycleLog;
    private Button btnAddExpense;
    private Button btnStartService;
    private Button btnStopService;
    private Button btnTestProvider;
    private TextView tvServiceStatus;
    private NotificationHelper notificationHelper;

    private TextView tvNotificationStatus;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // 服务相关
    private ServiceStatusReceiver serviceStatusReceiver;
    private RecyclerView recyclerView;
    private View emptyView;

    private AlertDialog addExpenseDialog;

    private TextView tvBroadcastStatus;

    // 广播接收器
    private ExpenseBroadcastReceiver broadcastReceiver;

    // 数据库帮助类
    private DatabaseHelper databaseHelper;

    // RecyclerView适配器
    private TransactionAdapter transactionAdapter;


    private int transactionCount = 0;
    private double totalExpense = 0.0;
    private double totalIncome = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "1. onCreate() - Activity正在被创建");

        setContentView(R.layout.activity_main);

        // 初始化数据库帮助类
        databaseHelper = new DatabaseHelper(this);
        notificationHelper = new NotificationHelper(this);

        initBroadcastReceiver();
        initViews();
        setupListeners();
        setupServiceReceiver();

        // 检查Intent中是否包含显示添加对话框的标志
        checkAndUpdateNotificationPermissionStatus();
        loadAndDisplayRecords();

        // 启动后台服务 - 在Activity创建时自动启动
        startExpenseMonitorService();
        // 注册广播接收器
        registerBroadcastReceiver();

        // 应用启动时自动发送广播
        sendStartupBroadcast();

        Log.d(TAG, "onCreate()执行完成，Activity已创建但不可见");
    }
    @Override
    protected void onStart() {
        super.onStart();
        updateLifecycleLog("2. onStart() - Activity即将变得可见");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLifecycleLog("3. onResume() - Activity获得焦点，可以与用户交互");

        // 自动发送记账提醒通知（检查当天是否有记账记录）
        autoSendReminderIfNeeded();

        Log.d(TAG, "onResume()执行完成，Activity已准备好与用户交互");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateLifecycleLog("4. onPause() - Activity即将失去焦点");

        // 在Activity失去焦点时可以清理资源或保存状态
        Log.d(TAG, "onPause()执行完成，Activity即将进入后台");
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateLifecycleLog("5. onStop() - Activity即将变得不可见");

        // 停止后台服务 - 在Activity进入后台时自动停止
        stopExpenseMonitorService();

        Log.d(TAG, "onStop()执行完成，Activity已不可见");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateLifecycleLog("onRestart() - Activity从停止状态重新启动");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 关闭数据库连接
        if (databaseHelper != null) {
            databaseHelper.close();
        }

        // 动态注册的广播接收器必须手动注销
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            Log.d(TAG, "广播接收器已注销");
        }

        // 注销服务状态广播接收器
        if (serviceStatusReceiver != null) {
            unregisterReceiver(serviceStatusReceiver);
        }

        Log.d(TAG, "onDestroy()执行完成，Activity已被销毁");
    }

    /**
     * 自动发送记账提醒（如果当天还没有记账记录）
     * 不需要用户点击按钮，在onResume()中自动调用
     */
    private void autoSendReminderIfNeeded() {
        try {
            // 1. 检查当天是否有记账记录
            int todayCount = getTodayRecordCount();

            // 2. 如果当天还没有记录，发送提醒
            if (todayCount == 0) {
                Log.d(TAG, "当天无记账记录，自动发送提醒通知");

                // 发送记账提醒通知
                if (notificationHelper != null) {
                    notificationHelper.sendExpenseReminderNotification();

                    // 更新状态显示
                    String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                    tvNotificationStatus.setText("已自动发送记账提醒 at " + time);
                }
            } else {
                Log.d(TAG, "当天已有" + todayCount + "条记录，无需提醒");
                tvNotificationStatus.setText("今日已记账 " + todayCount + " 条 ✓");
            }
        } catch (Exception e) {
            Log.e(TAG, "自动发送提醒失败", e);
        }
    }


    /**
     * 获取当天记账记录数量
     */
    private int getTodayRecordCount() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());

        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_RECORDS + " WHERE date LIKE '" + today + "%'";
        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(query, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();

        return count;
    }

    /**
     * 初始化广播相关组件
     */
    /**
     * 初始化广播接收器（动态注册）
     */
    private void initBroadcastReceiver() {
        broadcastReceiver = new ExpenseBroadcastReceiver();
        IntentFilter filter = new IntentFilter();

        // 添加要监听的广播Action
        filter.addAction(BroadcastConstants.ACTION_TRANSACTION_ADDED);
        filter.addAction(BroadcastConstants.ACTION_DATA_UPDATED);
        filter.addAction(BroadcastConstants.ACTION_BROADCAST_TEST);

        // Android 8.0+ 兼容性处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, filter);
        }

        Log.d(TAG, "广播接收器已动态注册");
    }

    private void initViews() {
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvExpense = findViewById(R.id.tv_expense);
        tvIncome = findViewById(R.id.tv_income);
        tvMonthSummary = findViewById(R.id.tv_month_summary);
        tvRecordCount = findViewById(R.id.tv_record_count);
        btnAddExpense = findViewById(R.id.btn_add_expense);
        recyclerView = findViewById(R.id.recycler_view);
        emptyView = findViewById(R.id.empty_view);

        // 初始化RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter();
        recyclerView.setAdapter(transactionAdapter);


        btnTestProvider = findViewById(R.id.btn_test_provider);



        tvNotificationStatus = findViewById(R.id.tv_notification_status);

        tvBroadcastStatus = findViewById(R.id.tv_broadcast_status);



    }

    /**
     * 注册广播接收器
     */
    private void registerBroadcastReceiver() {
        broadcastReceiver = new ExpenseBroadcastReceiver();

        // 创建IntentFilter并注册要监听的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(BroadcastConstants.ACTION_TRANSACTION_ADDED);
        filter.addAction(BroadcastConstants.ACTION_DATA_UPDATED);
        filter.addAction(BroadcastConstants.ACTION_BROADCAST_TEST);

        // 注册广播接收器（动态注册）
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, filter);

        updateBroadcastStatus("广播接收器已注册");
    }

    /**
     * 应用启动时发送广播
     * 通知其他组件应用已启动
     */
    private void sendStartupBroadcast() {
        Intent intent = new Intent(BroadcastConstants.ACTION_BROADCAST_TEST);
        intent.putExtra(BroadcastConstants.EXTRA_MESSAGE, "应用启动完成，广播系统已就绪");

        // 发送本地广播
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        updateBroadcastStatus("已发送启动广播");

        // 同时发送数据更新广播（初始状态）
        sendDataUpdatedBroadcast();
    }

    /**
     * 账目更新时发送广播
     * 在添加、修改或删除账目时调用此方法
     */
    public void onTransactionUpdated(String category, double amount, String time) {
        // 更新统计数据
        transactionCount++;
        if (amount < 0) {
            totalExpense += Math.abs(amount);
        } else {
            totalIncome += amount;
        }

        // 发送交易添加广播
        sendTransactionAddedBroadcast(category, amount, time);

        // 发送数据更新广播
        sendDataUpdatedBroadcast();

        updateBroadcastStatus("账目已更新，共" + transactionCount + "笔交易");
    }
    /**
     * 发送交易添加广播
     */
    private void sendTransactionAddedBroadcast(String category, double amount, String time) {
        Intent intent = new Intent(BroadcastConstants.ACTION_TRANSACTION_ADDED);
        intent.putExtra("category", category);
        intent.putExtra("amount", amount);
        intent.putExtra("time", time != null ? time : String.valueOf(System.currentTimeMillis()));

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 发送数据更新广播
     */
    private void sendDataUpdatedBroadcast() {
        Intent intent = new Intent(BroadcastConstants.ACTION_DATA_UPDATED);
        intent.putExtra(BroadcastConstants.EXTRA_TRANSACTION_COUNT, transactionCount);
        intent.putExtra(BroadcastConstants.EXTRA_TOTAL_EXPENSE, totalExpense);
        intent.putExtra(BroadcastConstants.EXTRA_TOTAL_INCOME, totalIncome);
        intent.putExtra(BroadcastConstants.EXTRA_NET_AMOUNT, totalIncome - totalExpense);
        intent.putExtra(BroadcastConstants.EXTRA_MESSAGE, "账目数据已自动更新");

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private void setupListeners() {
        btnAddExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddExpenseDialog();
            }
        });
        // 新增：服务控制按钮监



        // 测试Provider按钮
        btnTestProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testContentProvider();  // 调用新的测试方法
            }
        });



    }

    /**
     * 检查通知权限（Android 13+ 需要）
     */
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // 请求权限
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );

                tvNotificationStatus.setText("需要通知权限，请授权");
            } else {
                tvNotificationStatus.setText("通知权限已授权");
            }
        } else {
            tvNotificationStatus.setText("Android 13以下无需特殊权限");
        }
    }



    /**
     * 发送记账提醒通知
     */
    private void sendExpenseReminder() {
        try {
            // 检查Android 13+的通知权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // 直接请求权限，不显示解释对话框
                    requestNotificationPermission();
                    return;
                }
            }

            // 发送通知
            notificationHelper.sendExpenseReminderNotification();

            // 更新状态
            tvNotificationStatus.setText("通知已发送 - " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            Toast.makeText(this, "记账提醒通知已发送", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "记账提醒通知已发送");

        } catch (Exception e) {
            Log.e(TAG, "发送通知失败", e);
            tvNotificationStatus.setText("发送失败: " + e.getMessage());
            Toast.makeText(this, "发送通知失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示通知权限解释对话框
     */
    private void showNotificationPermissionExplanation() {
        new AlertDialog.Builder(this)
                .setTitle("需要通知权限")
                .setMessage("记账提醒功能需要通知权限来提醒您及时记录消费。\n\n" +
                        "请授予通知权限以使用完整的记账功能。")
                .setPositiveButton("去授权", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 请求权限
                        requestNotificationPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvNotificationStatus.setText("通知权限被拒绝，部分功能受限");
                        Toast.makeText(MainActivity.this,
                                "您可以在设置中手动开启通知权限", Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    /**
     * 请求通知权限
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    /**
     * 处理权限请求结果
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                tvNotificationStatus.setText("通知权限已授权 ✓");
                Toast.makeText(this, "通知权限已获得，现在可以发送通知了", Toast.LENGTH_SHORT).show();

                // 自动发送通知
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendExpenseReminder();
                    }
                }, 500);

            } else {
                // 权限被拒绝 - 显示引导信息
                showPermissionDeniedGuide();
            }
        }
    }
    /**
     * 显示权限被拒绝的引导信息
     */
    private void showPermissionDeniedGuide() {
        tvNotificationStatus.setText("通知权限被拒绝 ✗");

        // 显示简单的Toast提示
        Toast.makeText(this,
                "通知权限被拒绝。如需使用通知功能，请前往系统设置中手动开启。\n" +
                        "路径：设置 → 应用 → 记账助手 → 通知",
                Toast.LENGTH_LONG).show();

        // 也可以显示一个简单的对话框
        new AlertDialog.Builder(this)
                .setTitle("通知权限被拒绝")
                .setMessage("您拒绝了通知权限。\n\n" +
                        "如需使用通知功能，请手动前往系统设置开启：\n" +
                        "1. 打开手机设置\n" +
                        "2. 找到'应用'或'应用管理'\n" +
                        "3. 找到'记账助手'\n" +
                        "4. 点击'通知'\n" +
                        "5. 开启通知权限")
                .setPositiveButton("知道了", null)
                .show();
    }


    /**
     * 显示前往设置页面的对话框
     */
    private void showGoToSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("通知权限被永久拒绝")
                .setMessage("您已经永久拒绝了通知权限。\n\n" +
                        "如需使用通知功能，请前往设置页面手动开启权限。")
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openAppSettings();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 检查并更新通知权限状态显示
     */
    private void checkAndUpdateNotificationPermissionStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                tvNotificationStatus.setText("通知权限: 已授权 ✓");
            } else {
                tvNotificationStatus.setText("通知权限: 未授权 ✗");
            }
        } else {
            tvNotificationStatus.setText("通知权限: 自动拥有");
        }
    }

    /**
     * 打开应用设置页面
     */
    private void openAppSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "打开设置页面失败", e);
            Toast.makeText(this, "无法打开设置页面", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 检查Intent中是否包含显示添加对话框的标志
     * 用于处理从通知点击跳转过来的情况
     */
    private void checkIntentForAddDialog() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(Constants.ACTION_SHOW_ADD_DIALOG)) {
            boolean showDialog = intent.getBooleanExtra(Constants.ACTION_SHOW_ADD_DIALOG, false);
            if (showDialog) {
                // 延迟显示对话框，确保UI已完全加载
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showAddExpenseDialog();
                    }
                }, 500);

                Log.d(TAG, "从通知跳转，显示添加对话框");
            }
        }
    }






    /**
     * 发送交易添加广播（在添加交易成功后调用）
     */
    private void sendTransactionAddedBroadcast(int transactionCount, double totalAmount) {
        Intent broadcastIntent = new Intent(BroadcastConstants.ACTION_TRANSACTION_ADDED);
        broadcastIntent.putExtra(BroadcastConstants.EXTRA_TRANSACTION_COUNT, transactionCount);
        broadcastIntent.putExtra(BroadcastConstants.EXTRA_TOTAL_AMOUNT, totalAmount);

        sendBroadcast(broadcastIntent);
        Log.d(TAG, "发送交易添加广播，数量: " + transactionCount + ", 总额: " + totalAmount);
    }

    /**
     * 发送数据更新广播
     */
    private void sendDataUpdatedBroadcast(String message) {
        Intent broadcastIntent = new Intent(BroadcastConstants.ACTION_DATA_UPDATED);
        broadcastIntent.putExtra(BroadcastConstants.EXTRA_MESSAGE, message);

        sendBroadcast(broadcastIntent);
        Log.d(TAG, "发送数据更新广播: " + message);
    }




    /**
     * 计算总金额
     */
    private double[] calculateFinancialSummary() {
        double totalExpense = 0;
        double totalIncome = 0;

        Cursor cursor = databaseHelper.getAllRecords();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT));

                if (type == Transaction.TYPE_INCOME) {
                    totalIncome += amount;
                } else {
                    totalExpense += amount;
                }
            }
            cursor.close();
        }

        double netAmount = totalIncome - totalExpense;
        return new double[]{totalExpense, totalIncome, netAmount};
    }
    /**
     * 在添加交易成功后发送广播
     */
    private void sendTransactionAddedBroadcast(String category, double amount) {
        Intent broadcastIntent = new Intent(BroadcastConstants.ACTION_TRANSACTION_ADDED);
        broadcastIntent.putExtra("category", category);
        broadcastIntent.putExtra("amount", amount);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        broadcastIntent.putExtra("time", sdf.format(new Date()));

        sendBroadcast(broadcastIntent);
        Log.d(TAG, "交易添加广播已发送: " + category + " ¥" + amount);
    }

    /**
     * 更新广播状态（供广播接收器回调）
     */
    public void updateBroadcastStatus(String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentText = tvBroadcastStatus.getText().toString();
                String newText = "📥 收到广播: " + status + "\n\n" + currentText;

                // 限制显示行数
                String[] lines = newText.split("\n");
                if (lines.length > 8) {
                    StringBuilder limitedText = new StringBuilder();
                    for (int i = 0; i < 8; i++) {
                        limitedText.append(lines[i]);
                        if (i < 7) limitedText.append("\n");
                    }
                    newText = limitedText.toString();
                }

                tvBroadcastStatus.setText(newText);
            }
        });
    }
    /**
     * 设置服务状态广播接收器
     */
    private void setupServiceReceiver() {
        serviceStatusReceiver = new ServiceStatusReceiver();
        serviceStatusReceiver.setOnStatusUpdateListener(new ServiceStatusReceiver.OnStatusUpdateListener() {
            @Override
            public void onStatusUpdate(String status, String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateServiceStatus(status, message);
                    }
                });
            }
        });

        // 注册广播接收器 - 修复Android 13+的安全问题
        IntentFilter filter = new IntentFilter(ExpenseMonitorService.ACTION_SERVICE_STATUS);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要指定导出标志
            registerReceiver(serviceStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // Android 12及以下版本
            registerReceiver(serviceStatusReceiver, filter);
        }

        Log.d(TAG, "广播接收器已动态注册");
    }

    /**
     * 启动记账监控服务
     */
    private void startExpenseMonitorService() {
        try {
            Intent serviceIntent = new Intent(this, ExpenseMonitorService.class);
            startService(serviceIntent);

            tvServiceStatus.setText("服务: 启动中...");
            Toast.makeText(this, "正在启动后台监控服务", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "启动记账监控服务");

        } catch (Exception e) {
            Log.e(TAG, "启动服务失败", e);
            Toast.makeText(this, "启动失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 停止记账监控服务
     */
    private void stopExpenseMonitorService() {
        try {
            Intent serviceIntent = new Intent(this, ExpenseMonitorService.class);
            stopService(serviceIntent);

            tvServiceStatus.setText("服务: 已停止");
            Toast.makeText(this, "已停止后台监控服务", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "停止记账监控服务");

        } catch (Exception e) {
            Log.e(TAG, "停止服务失败", e);
            Toast.makeText(this, "停止失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 更新服务状态显示
     */
    private void updateServiceStatus(String status, String message) {
        tvServiceStatus.setText("服务: " + status);

        // 在Logcat中显示详细消息
        Log.d(TAG, "服务状态更新: " + status + " - " + message);

        // 可以在这里添加更多状态更新逻辑
        // 例如：显示Toast通知
        if (!status.equals("服务已停止")) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 测试ContentProvider
     */
    /**
     * 测试ContentProvider - 演示数据访问接口层的使用
     */
    private void testContentProvider() {
        try {
            Log.d(TAG, "=== 开始测试ContentProvider ===");

            // 1. 通过ContentProvider查询所有交易记录
            Uri queryUri = ExpenseContract.TransactionEntry.CONTENT_URI;
            Cursor cursor = getContentResolver().query(
                    queryUri,
                    null,  // 查询所有列
                    null,  // 无筛选条件
                    null,  // 无参数
                    DatabaseHelper.COLUMN_DATE + " DESC"  // 按日期倒序
            );

            if (cursor != null) {
                int count = cursor.getCount();
                Log.d(TAG, "通过ContentProvider查询到 " + count + " 条交易记录");

                // 演示遍历数据
                if (cursor.moveToFirst()) {
                    do {
                        String category = cursor.getString(
                                cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY)
                        );
                        double amount = cursor.getDouble(
                                cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT)
                        );
                        Log.d(TAG, "记录: " + category + " - ¥" + amount);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }

            // 2. 通过ContentProvider插入一条新记录
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_CATEGORY, "测试类别");
            values.put(DatabaseHelper.COLUMN_NOTE, "通过Provider插入");
            values.put(DatabaseHelper.COLUMN_DATE, getCurrentDateTime());
            values.put(DatabaseHelper.COLUMN_TYPE, Transaction.TYPE_EXPENSE);
            values.put(DatabaseHelper.COLUMN_AMOUNT, 99.99);

            Uri insertUri = getContentResolver().insert(
                    ExpenseContract.TransactionEntry.CONTENT_URI,
                    values
            );

            if (insertUri != null) {
                Log.d(TAG, "通过ContentProvider插入成功，URI: " + insertUri);

                // 3. 查询统计信息（演示虚拟表）
                Uri statsUri = ExpenseContract.StatisticsEntry.CONTENT_URI;
                Cursor statsCursor = getContentResolver().query(
                        statsUri,
                        null, null, null, null
                );

                if (statsCursor != null && statsCursor.moveToFirst()) {
                    int totalCount = statsCursor.getInt(0);
                    double totalExpense = statsCursor.getDouble(1);
                    double totalIncome = statsCursor.getDouble(2);

                    String message = String.format(
                            "统计信息: 共%d条记录，总支出¥%.2f，总收入¥%.2f",
                            totalCount, totalExpense, totalIncome
                    );

                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, message);

                    statsCursor.close();
                }
            }

            // 4. 重新加载数据
            loadAndDisplayRecords();

            Log.d(TAG, "=== ContentProvider测试完成 ===");

        } catch (Exception e) {
            Log.e(TAG, "测试ContentProvider失败", e);
            Toast.makeText(this, "Provider测试失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 发送交易成功广播和通知
     */
    private void sendTransactionBroadcastAndNotification(String category, double amount) {
        // 1. 发送广播
        Intent broadcastIntent = new Intent(Constants.ACTION_TRANSACTION_ADDED);
        broadcastIntent.putExtra(Constants.EXTRA_TRANSACTION_AMOUNT, amount);
        broadcastIntent.putExtra(Constants.EXTRA_TRANSACTION_CATEGORY, category);
        sendBroadcast(broadcastIntent);

        // 2. 发送通知
        notificationHelper.sendTransactionNotification(category, amount);

        // 3. 发送数据更新广播
        Intent updateIntent = new Intent(Constants.ACTION_DATA_UPDATED);
        sendBroadcast(updateIntent);

        Log.d(TAG, "交易广播和通知已发送: " + category + " ¥" + amount);
    }

    /**
     * 发送账目更新广播（贴合记账场景）
     */
    private void sendTransactionUpdateBroadcast() {
        try {
            // 1. 获取当前交易统计数据
            int transactionCount = databaseHelper.getRecordCount();

            // 2. 计算总金额
            double[] financialSummary = calculateFinancialSummary();
            double totalExpense = financialSummary[0];
            double totalIncome = financialSummary[1];
            double netAmount = financialSummary[2];

            // 3. 创建广播Intent
            Intent broadcastIntent = new Intent(BroadcastConstants.ACTION_DATA_UPDATED);

            // 4. 添加记账相关的数据
            broadcastIntent.putExtra(BroadcastConstants.EXTRA_TRANSACTION_COUNT, transactionCount);
            broadcastIntent.putExtra(BroadcastConstants.EXTRA_TOTAL_EXPENSE, totalExpense);
            broadcastIntent.putExtra(BroadcastConstants.EXTRA_TOTAL_INCOME, totalIncome);
            broadcastIntent.putExtra(BroadcastConstants.EXTRA_NET_AMOUNT, netAmount);

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            broadcastIntent.putExtra(BroadcastConstants.EXTRA_MESSAGE,
                    "账目数据更新于 " + timestamp + "，当前共" + transactionCount + "笔交易");

            // 5. 发送广播
            sendBroadcast(broadcastIntent);

            // 6. 更新UI状态
            String status = "📢 已发送账目更新广播\n" +
                    "时间: " + timestamp + "\n" +
                    "交易数: " + transactionCount + " 笔\n" +
                    "总支出: ¥" + String.format("%.2f", totalExpense) + "\n" +
                    "总收入: ¥" + String.format("%.2f", totalIncome) + "\n" +
                    "净额: ¥" + String.format("%.2f", netAmount);
            tvBroadcastStatus.setText(status);

            Toast.makeText(this, "账目更新广播已发送", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "账目更新广播已发送: " + status);

        } catch (Exception e) {
            Log.e(TAG, "发送广播失败", e);
            tvBroadcastStatus.setText("发送失败: " + e.getMessage());
            Toast.makeText(this, "发送广播失败", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * 修改添加交易的方法，在成功后发送广播
     */
    /**
     * 修改添加交易的方法，在成功后发送广播
     */
    private void processAddExpenseInDialog(EditText etAmount, EditText etCategory, EditText etNote, int type) {
        String amountStr = etAmount.getText().toString().trim();
        String category = etCategory.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr)) {
            etAmount.setError("请输入金额");
            etAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("金额必须大于0");
                etAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            etAmount.setError("请输入有效的金额");
            etAmount.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(category)) {
            etCategory.setError("请输入类别");
            etCategory.requestFocus();
            return;
        }

        String currentDate = getCurrentDateTime();

        // 插入记录到数据库
        long result = insertRecordToDatabase(category, note, currentDate, type, amount);

        if (result != -1) {
            addExpenseDialog.dismiss();

            // 发送交易添加广播
            sendTransactionAddedBroadcast(category, amount);
            if (notificationHelper != null) {
                notificationHelper.sendTransactionNotification(category, amount);
            }

            // 重新加载并显示记录
            loadAndDisplayRecords();

            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "记录添加成功，ID: " + result);
        } else {
            Toast.makeText(this, "添加失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将当前显示的交易记录保存到数据库
     */
    private void saveCurrentTransactionsToDatabase() {
        try {
            // 1. 清空数据库
            databaseHelper.deleteAllRecords();

            // 2. 获取当前显示的交易记录
            List<Transaction> transactions = transactionAdapter.getTransactions();

            if (transactions.isEmpty()) {
                Toast.makeText(this, "没有交易记录可保存", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. 将每条记录保存到数据库
            int successCount = 0;
            for (Transaction transaction : transactions) {
                long result = databaseHelper.insertRecord(
                        transaction.getCategory(),
                        transaction.getNote(),
                        transaction.getDate(),
                        transaction.getType(),
                        transaction.getAmount()
                );

                if (result != -1) {
                    successCount++;
                }
            }

            // 4. 显示成功消息
            Toast.makeText(this, "成功保存 " + successCount + " 条记录到数据库",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "保存了 " + successCount + " 条记录到数据库");

        } catch (Exception e) {
            Log.e(TAG, "保存到数据库失败", e);
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void createSampleRecords() {
        try {
            // 1. 先清空现有记录
            databaseHelper.deleteAllRecords();

            // 2. 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());

            // 3. 创建10条示例记录
            long[] results = new long[10];

            // 支出记录
            results[0] = databaseHelper.insertRecord("餐饮", "午餐", currentTime, Transaction.TYPE_EXPENSE, 25.50);
            results[1] = databaseHelper.insertRecord("交通", "地铁", currentTime, Transaction.TYPE_EXPENSE, 8.00);
            results[2] = databaseHelper.insertRecord("学习", "购买书籍", currentTime, Transaction.TYPE_EXPENSE, 15.00);
            results[3] = databaseHelper.insertRecord("餐饮", "早餐", currentTime, Transaction.TYPE_EXPENSE, 10.00);
            results[4] = databaseHelper.insertRecord("餐饮", "晚餐", currentTime, Transaction.TYPE_EXPENSE, 20.00);
            results[5] = databaseHelper.insertRecord("娱乐", "游乐场", currentTime, Transaction.TYPE_EXPENSE, 80.00);
            results[6] = databaseHelper.insertRecord("餐饮", "早餐", currentTime, Transaction.TYPE_EXPENSE, 8.00);
            results[7] = databaseHelper.insertRecord("学习", "百度网盘年卡", currentTime, Transaction.TYPE_EXPENSE, 200.00);

            // 收入记录
            results[8] = databaseHelper.insertRecord("工资", "本月工资", currentTime, Transaction.TYPE_INCOME, 3000.00);
            results[9] = databaseHelper.insertRecord("兼职", "周末兼职", currentTime, Transaction.TYPE_INCOME, 500.00);

            // 4. 检查插入结果
            int successCount = 0;
            for (long result : results) {
                if (result != -1) {
                    successCount++;
                }
            }

            // 5. 重新加载并显示记录
            loadAndDisplayRecords();

            // 6. 显示成功消息
            Toast.makeText(this, "成功创建 " + successCount + " 条示例记录", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "创建了 " + successCount + " 条示例记录");

        } catch (Exception e) {
            Log.e(TAG, "创建示例记录失败", e);
            Toast.makeText(this, "创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    // ... existing code ...

    /**
     * 添加示例记录（独立方法，不通过按钮调用）
     */
    private void addSampleRecords() {
        try {
            // 1. 获取当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());

            // 2. 创建示例记录（不删除现有数据）
            long[] results = new long[10];

            // 支出记录
            results[0] = databaseHelper.insertRecord("餐饮", "午餐", currentTime, Transaction.TYPE_EXPENSE, 25.50);
            results[1] = databaseHelper.insertRecord("交通", "地铁", currentTime, Transaction.TYPE_EXPENSE, 8.00);
            results[2] = databaseHelper.insertRecord("学习", "购买书籍", currentTime, Transaction.TYPE_EXPENSE, 15.00);
            results[3] = databaseHelper.insertRecord("餐饮", "早餐", currentTime, Transaction.TYPE_EXPENSE, 10.00);
            results[4] = databaseHelper.insertRecord("餐饮", "晚餐", currentTime, Transaction.TYPE_EXPENSE, 20.00);
            results[5] = databaseHelper.insertRecord("娱乐", "游乐场", currentTime, Transaction.TYPE_EXPENSE, 80.00);
            results[6] = databaseHelper.insertRecord("餐饮", "早餐", currentTime, Transaction.TYPE_EXPENSE, 8.00);
            results[7] = databaseHelper.insertRecord("学习", "百度网盘年卡", currentTime, Transaction.TYPE_EXPENSE, 200.00);

            // 收入记录
            results[8] = databaseHelper.insertRecord("工资", "本月工资", currentTime, Transaction.TYPE_INCOME, 3000.00);
            results[9] = databaseHelper.insertRecord("兼职", "周末兼职", currentTime, Transaction.TYPE_INCOME, 500.00);

            // 3. 检查插入结果
            int successCount = 0;
            for (long result : results) {
                if (result != -1) {
                    successCount++;
                }
            }

            // 4. 重新加载并显示记录
            loadAndDisplayRecords();

            // 5. 显示成功消息
            Toast.makeText(this, "成功添加 " + successCount + " 条示例记录", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "添加了 " + successCount + " 条示例记录");

        } catch (Exception e) {
            Log.e(TAG, "添加示例记录失败", e);
            Toast.makeText(this, "添加失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从数据库加载记录并显示在RecyclerView中
     */
    private void loadAndDisplayRecords() {
        try {
            // 1. 获取Cursor对象（包含查询结果）
            Cursor cursor = databaseHelper.getAllRecords();

            // 2. 检查是否有数据
            if (cursor == null || cursor.getCount() == 0) {
                // 显示空视图
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                tvRecordCount.setText("共 0 条");
                return;
            }

            // 3. 创建交易记录列表
            List<Transaction> transactions = new ArrayList<>();
            double totalExpense = 0;
            double totalIncome = 0;
            double monthExpense = 0;

            // 获取当前月份
            SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String currentMonth = monthFormat.format(new Date());

            while (cursor.moveToNext()) {
                // 4. 使用Cursor的get方法获取各列数据
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CATEGORY));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AMOUNT));

                // 5. 创建Transaction对象
                Transaction transaction = new Transaction(id, amount, category, note, date, type);
                transactions.add(transaction);

                // 6. 统计总支出和总收入
                if (type == Transaction.TYPE_EXPENSE) {
                    totalExpense += amount;

                    // 统计本月支出
                    if (date.startsWith(currentMonth)) {
                        monthExpense += amount;
                    }
                } else {
                    totalIncome += amount;
                }
            }

            // 7. 关闭Cursor
            cursor.close();

            // 8. 更新RecyclerView
            transactionAdapter.setTransactions(transactions);
            transactionAdapter.setOnItemLongClickListener(new TransactionAdapter.OnItemLongClickListener() {
                @Override
                public void onItemLongClick(int position) {
                    showDeleteDialog(position);
                }
            });
            transactionAdapter.notifyDataSetChanged();

            // 9. 更新UI统计信息
            int recordCount = transactions.size();
            tvRecordCount.setText(String.format("共 %d 条", recordCount));
            tvExpense.setText(String.format("¥ %.2f", totalExpense));
            tvIncome.setText(String.format("¥ %.2f", totalIncome));
            tvTotalAmount.setText(String.format("¥ %.2f", (totalIncome - totalExpense)));

            // 10. 更新本月消费记录
            int monthCount = getCurrentMonthCount(transactions, currentMonth);
            tvMonthSummary.setText(String.format("本月已消费 %d 笔", monthCount));

            // 11. 显示RecyclerView，隐藏空视图
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            Log.d(TAG, "从数据库加载了 " + recordCount + " 条记录");



        } catch (Exception e) {
            Log.e(TAG, "从数据库读取失败", e);
        }
    }

    /**
     * 显示删除对话框
     */
    private void showDeleteDialog(final int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除记录")
                .setMessage("确定要删除这条交易记录吗？")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRecordFromDatabase(position);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    /**
     * 插入一条记录到数据库
     */
    private long insertRecordToDatabase(String category, String note, String date, int type, double amount) {
        return databaseHelper.insertRecord(category, note, date, type, amount);
    }



    /**
     * 从数据库删除记录
     */
    private void deleteRecordFromDatabase(int position) {
        try {
            // 1. 获取当前显示的交易记录
            List<Transaction> transactions = transactionAdapter.getTransactions();
            if (position < 0 || position >= transactions.size()) {
                Toast.makeText(this, "记录不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            Transaction transaction = transactions.get(position);

            // 2. 从数据库删除记录（使用ID）
            if (transaction.getId() != -1) {
                int rowsDeleted = databaseHelper.deleteRecordById(transaction.getId());
                if (rowsDeleted > 0) {
                    // 3. 重新加载数据
                    loadAndDisplayRecords();
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "删除记录成功，ID: " + transaction.getId());
                } else {
                    Toast.makeText(this, "删除失败，记录不存在", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "无法删除，记录ID无效", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "删除记录失败", e);
            Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddExpenseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加交易记录");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        final EditText etAmount = dialogView.findViewById(R.id.et_amount);
        final EditText etCategory = dialogView.findViewById(R.id.et_category);
        final EditText etNote = dialogView.findViewById(R.id.et_note);
        final RadioGroup rgType = dialogView.findViewById(R.id.rg_type);
        builder.setView(dialogView);
        builder.setPositiveButton("添加", null);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        addExpenseDialog = builder.create();
        addExpenseDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = addExpenseDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int selectedType = Transaction.TYPE_EXPENSE;
                        int selectedId = rgType.getCheckedRadioButtonId();
                        if (selectedId == R.id.rb_income) {
                            selectedType = Transaction.TYPE_INCOME;
                        }
                        processAddExpenseInDialog(etAmount, etCategory, etNote, selectedType);
                    }
                });
            }
        });
        addExpenseDialog.show();
    }



    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void updateLifecycleLog(String lifecycleEvent) {
        if (tvLifecycleLog != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            String logText = tvLifecycleLog.getText().toString();
            String newLog = currentTime + " " + lifecycleEvent;
            if (!logText.isEmpty()) {
                logText = newLog + "\n" + logText;
            } else {
                logText = newLog;
            }
            String[] lines = logText.split("\n");
            if (lines.length > 6) {
                StringBuilder limitedLog = new StringBuilder();
                for (int i = 0; i < 6; i++) {
                    limitedLog.append(lines[i]);
                    if (i < 5) limitedLog.append("\n");
                }
                logText = limitedLog.toString();
            }
            tvLifecycleLog.setText(logText);
        }
    }

    private void refreshUI() {
        // 这里可以更新UI显示
        int recordCount = databaseHelper.getRecordCount();
        tvRecordCount.setText(String.format("共 %d 条", recordCount));
    }
    /**
     * 获取本月交易笔数
     */
    private int getCurrentMonthCount(List<Transaction> transactions, String currentMonth) {
        int count = 0;
        for (Transaction transaction : transactions) {
            if (transaction.getDate().startsWith(currentMonth)) {
                count++;
            }
        }
        return count;
    }

}