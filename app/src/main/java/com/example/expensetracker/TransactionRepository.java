package com.example.expensetracker;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransactionRepository {

    private static TransactionRepository instance;
    private final List<Transaction> transactionList;

    private LocalStorageHelper localStorageHelper;
    private TransactionRepository(Context context) {

        transactionList = new ArrayList<>();
        // 初始化本地存储工具
        localStorageHelper = new LocalStorageHelper(context);

        // 从本地存储加载数据
        loadFromStorage();
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionRepository(context);
        }
        return instance;
    }

    /**
     * 从本地存储加载数据
     */
    private void loadFromStorage() {
        List<Transaction> loadedTransactions = localStorageHelper.loadTransactions();
        transactionList.clear();
        transactionList.addAll(loadedTransactions);
    }

    /**
     * 保存数据到本地存储
     */
    private void saveToStorage() {
        localStorageHelper.saveTransactions(transactionList);
    }

    /**
     * 添加交易记录
     */
    public Transaction addTransaction(double amount, String type, String category, String note, String date) {
        // 将字符串类型转换为整数类型
        int typeInt = "收入".equals(type) ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
        Transaction transaction = new Transaction(amount, category, note, date, typeInt);
        transactionList.add(0, transaction); // 添加到开头，实现倒序

        saveToStorage();
        return transaction;
    }

    /**
     * 添加Transaction对象
     */
    public Transaction addTransaction(Transaction transaction) {
        // 按日期插入到正确位置
        int insertPosition = 0;
        for (int i = 0; i < transactionList.size(); i++) {
            if (transaction.getDate().compareTo(transactionList.get(i).getDate()) > 0) {
                insertPosition = i;
                break;
            }
        }
        transactionList.add(insertPosition, transaction);
        saveToStorage();
        return transaction;
    }

    public Transaction deleteTransaction(int position) {
        if (position >= 0 && position < transactionList.size()) {
            Transaction removed = transactionList.remove(position);

            // 保存到本地存储
            saveToStorage();

            return removed;
        }
        return null;
    }

    public boolean deleteTransaction(Transaction transaction) {
        boolean removed = transactionList.remove(transaction);

        if (removed) {
            // 保存到本地存储
            saveToStorage();
        }

        return removed;
    }



    /**
     * 获取所有交易记录（按时间倒序）
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> copyList = new ArrayList<>(transactionList);
        Collections.sort(copyList, new Comparator<Transaction>() {
            @Override
            public int compare(Transaction t1, Transaction t2) {
                return t2.getDate().compareTo(t1.getDate());
            }
        });
        return copyList;
    }

    /**
     * 获取总支出金额
     */
    public double getTotalExpense() {
        double total = 0.0;
        for (Transaction transaction : transactionList) {
            if (transaction.isExpense()) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    /**
     * 获取总收入金额
     */
    public double getTotalIncome() {
        double total = 0.0;
        for (Transaction transaction : transactionList) {
            if (transaction.isIncome()) {
                total += transaction.getAmount();
            }
        }
        return total;
    }

    /**
     * 获取净额（收入 - 支出）
     */
    public double getNetAmount() {
        return getTotalIncome() - getTotalExpense();
    }

    public int getRecordCount() {
        return transactionList.size();
    }

    public void clearAllTransactions() {
        transactionList.clear();
        saveToStorage();
    }

    /**
     * 清空本地存储（完全删除文件）
     */
    public boolean clearStorage() {
        transactionList.clear();
        return localStorageHelper.clearStorage();
    }

    public Transaction getTransaction(int position) {
        if (position >= 0 && position < transactionList.size()) {
            return transactionList.get(position);
        }
        return null;
    }

    public boolean updateTransaction(int position, Transaction transaction) {
        if (position >= 0 && position < transactionList.size()) {
            transactionList.set(position, transaction);
            return true;
        }
        return false;
    }

    public List<Transaction> getTransactionsSortedByDate() {
        return getAllTransactions();
    }

    public List<Transaction> getTransactionsByCategory(String category) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction transaction : transactionList) {
            if (transaction.getCategory().equals(category)) {
                result.add(transaction);
            }
        }
        return result;
    }
}