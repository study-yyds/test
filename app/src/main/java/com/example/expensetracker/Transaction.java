package com.example.expensetracker;

import java.io.Serializable;

/**
 * 消费记录实体类
 * 实现Serializable接口，支持对象序列化存储
 *
 * 教学要点：
 * 1. Serializable接口：标记类可被序列化
 * 2. serialVersionUID：序列化版本号，确保兼容性
 * 3. 所有字段都必须是可序列化的（基本类型、String等）
 */
public class Transaction implements Serializable {
    // 序列化版本号 - 重要：确保序列化兼容性
    private static final long serialVersionUID = 1L;
    private int id;
    // 消费金额
    private double amount;
    // 消费类别
    private String category;
    // 消费备注
    private String note;
    // 消费日期（格式：yyyy-MM-dd HH:mm:ss）
    private String date;
    // 交易类型：1-支出，2-收入
    private int type;

    // 交易类型常量
    public static final int TYPE_EXPENSE = 1;
    public static final int TYPE_INCOME = 2;

    // 默认构造方法
    public Transaction() {
        this.amount = 0.0;
        this.category = "其他";
        this.note = "";
        this.date = "";
        this.type = TYPE_EXPENSE;
    }

    // 带参数的构造方法
    public Transaction(int id, double amount, String category, String note, String date, int type) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.date = date;
        this.type = type;
    }

    // 保留原有构造函数用于兼容
    public Transaction(double amount, String category, String note, String date, int type) {
        this(-1, amount, category, note, date, type);
    }

    // 添加getter和setter
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter 和 Setter 方法
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    // 判断是否为支出
    public boolean isExpense() {
        return type == TYPE_EXPENSE;
    }

    // 判断是否为收入
    public boolean isIncome() {
        return type == TYPE_INCOME;
    }

    // 获取带符号的金额（支出为负，收入为正）
    public double getSignedAmount() {
        if (isExpense()) {
            return -amount;
        } else {
            return amount;
        }
    }

    // 重写toString方法，便于调试和显示
    @Override
    public String toString() {
        String typeStr = isExpense() ? "支出" : "收入";
        return "Transaction{" +
                "amount=" + amount +
                ", category='" + category + '\'' +
                ", note='" + note + '\'' +
                ", date='" + date + '\'' +
                ", type=" + typeStr +
                '}';
    }
}