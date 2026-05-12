package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLite数据库帮助类
 * 负责创建数据库和表，提供数据库操作接口
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    // 数据库信息
    private static final String DATABASE_NAME = "expense_tracker.db";
    private static final int DATABASE_VERSION = 1;

    // 表名和字段名
    public static final String TABLE_RECORDS = "records";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_NOTE = "note";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_AMOUNT = "amount";

    // 创建表的SQL语句
    private static final String CREATE_TABLE_RECORDS =
            "CREATE TABLE " + TABLE_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CATEGORY + " TEXT NOT NULL, " +
                    COLUMN_NOTE + " TEXT, " +
                    COLUMN_DATE + " TEXT NOT NULL, " +
                    COLUMN_TYPE + " INTEGER NOT NULL, " +
                    COLUMN_AMOUNT + " REAL NOT NULL" +
                    ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "DatabaseHelper初始化完成");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建表
        db.execSQL(CREATE_TABLE_RECORDS);
        Log.d(TAG, "数据库表创建成功: " + TABLE_RECORDS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级数据库时删除旧表，创建新表
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        onCreate(db);
        Log.d(TAG, "数据库升级完成，从版本 " + oldVersion + " 到 " + newVersion);
    }

    /**
     * 插入一条记录到数据库
     * @param category 消费类别
     * @param note 消费备注
     * @param date 日期
     * @param type 类型（1-支出，2-收入）
     * @param amount 金额
     * @return 插入的行ID，失败返回-1
     */
    public long insertRecord(String category, String note, String date, int type, double amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = -1;

        try {
            // 创建ContentValues对象存储数据
            ContentValues values = new ContentValues();
            values.put(COLUMN_CATEGORY, category);
            values.put(COLUMN_NOTE, note);
            values.put(COLUMN_DATE, date);
            values.put(COLUMN_TYPE, type);
            values.put(COLUMN_AMOUNT, amount);

            // 插入数据
            result = db.insert(TABLE_RECORDS, null, values);

            if (result != -1) {
                Log.d(TAG, "记录插入成功，ID: " + result);
            } else {
                Log.e(TAG, "记录插入失败");
            }

        } catch (Exception e) {
            Log.e(TAG, "插入记录异常", e);
        } finally {
            db.close();
        }

        return result;
    }

    /**
     * 查询所有记录
     * @return Cursor对象，包含所有记录
     */
    public Cursor getAllRecords() {
        SQLiteDatabase db = this.getReadableDatabase();

        // 定义要查询的列
        String[] columns = {
                COLUMN_ID,
                COLUMN_CATEGORY,
                COLUMN_NOTE,
                COLUMN_DATE,
                COLUMN_TYPE,
                COLUMN_AMOUNT
        };

        // 查询所有记录，按日期倒序排列
        Cursor cursor = db.query(
                TABLE_RECORDS,          // 表名
                columns,                // 要查询的列
                null,                   // WHERE条件
                null,                   // WHERE条件参数
                null,                   // GROUP BY
                null,                   // HAVING
                COLUMN_DATE + " DESC"   // ORDER BY（按日期倒序）
        );

        Log.d(TAG, "查询到 " + cursor.getCount() + " 条记录");
        return cursor;
    }

    /**
     * 删除所有记录
     * @return 删除的行数
     */
    public int deleteAllRecords() {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_RECORDS, null, null);
        db.close();

        Log.d(TAG, "删除了 " + rowsDeleted + " 条记录");
        return rowsDeleted;
    }

    /**
     * 根据ID删除记录
     * @param id 记录ID
     * @return 删除的行数
     */
    public int deleteRecordById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_RECORDS,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();

        Log.d(TAG, "删除记录ID: " + id + ", 结果: " + rowsDeleted + " 行");
        return rowsDeleted;
    }


    /**
     * 获取记录总数
     * @return 记录总数
     */
    public int getRecordCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECORDS, null);

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return count;
    }
}