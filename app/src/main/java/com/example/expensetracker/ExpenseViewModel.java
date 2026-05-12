package com.example.expensetracker;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseViewModel {

    private TransactionRepository repository;

    public ExpenseViewModel(Context context) {
        repository = TransactionRepository.getInstance(context);
        initTestData();
    }

    private void initTestData() {
        if(repository.getRecordCount() ==0){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date now = new Date();

            // 添加支出记录
            repository.addTransaction(25.5, "支出", "餐饮", "午餐", sdf.format(now));
            repository.addTransaction(8.0, "支出", "交通", "地铁", sdf.format(now));
            repository.addTransaction(15.0, "支出", "学习", "购买书籍", sdf.format(now));
            repository.addTransaction(10.0, "支出", "餐饮", "早餐", sdf.format(now));
            repository.addTransaction(20.0, "支出", "餐饮", "晚餐", sdf.format(now));
            repository.addTransaction(80.0, "支出", "娱乐", "游乐场", sdf.format(now));
            repository.addTransaction(8.0, "支出", "餐饮", "早餐", sdf.format(now));
            repository.addTransaction(200.0, "支出", "学习", "百度网盘年卡", sdf.format(now));

            // 添加收入记录
            repository.addTransaction(3000.0, "收入", "工资", "本月工资", sdf.format(now));
            repository.addTransaction(500.0, "收入", "兼职", "周末兼职", sdf.format(now));
        }

    }

    public boolean addTransaction(double amount, String category, String note, String date, int type) {
        if (amount <= 0) {
            return false;
        }

        if (category == null || category.trim().isEmpty()) {
            category = "其他";
        }

        if (date == null || date.trim().isEmpty()) {
            date = getCurrentDateTime();
        }

        // 将整数类型转换为字符串类型
        String typeStr = (type == Transaction.TYPE_INCOME) ? "收入" : "支出";
        repository.addTransaction(amount, typeStr, category, note, date);
        return true;
    }

    public boolean deleteTransaction(int position) {
        Transaction removed = repository.deleteTransaction(position);
        return removed != null;
    }

    public List<Transaction> getAllTransactions() {
        return repository.getAllTransactions();
    }

    public double getTotalAmount() {
        return repository.getNetAmount();
    }

    public double getTotalExpense() {
        return repository.getTotalExpense();
    }

    public double getTotalIncome() {
        return repository.getTotalIncome();
    }

    public int getRecordCount() {
        return repository.getRecordCount();
    }

    public int getCurrentMonthCount() {
        return repository.getRecordCount();
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void clearAllTransactions() {
        repository.clearAllTransactions();
    }

    public Transaction getTransaction(int position) {
        return repository.getTransaction(position);
    }
}