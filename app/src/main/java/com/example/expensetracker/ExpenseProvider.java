package com.example.expensetracker;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * ContentProvider实现类 - 数据访问接口层
 * 教学重点：
 * 1. Provider不是数据库本身，而是访问接口
 * 2. URI的设计和匹配机制
 * 3. CRUD操作的标准接口
 */
public class ExpenseProvider extends ContentProvider {
    private static final String TAG = "ExpenseProvider";

    // 数据库帮助类
    private DatabaseHelper databaseHelper;

    // URI匹配器 - 用于解析不同的URI请求
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // URI匹配代码
    private static final int CODE_TRANSACTIONS = 100;          // 所有交易记录
    private static final int CODE_TRANSACTION_ID = 101;        // 单条交易记录
    private static final int CODE_STATISTICS = 200;            // 统计信息

    // 静态初始化块 - 注册URI模式
    static {
        // content://com.example.expensetracker.provider/transactions
        uriMatcher.addURI(
                ExpenseContract.AUTHORITY,
                ExpenseContract.TransactionEntry.TABLE_NAME,
                CODE_TRANSACTIONS
        );

        // content://com.example.expensetracker.provider/transactions/#
        uriMatcher.addURI(
                ExpenseContract.AUTHORITY,
                ExpenseContract.TransactionEntry.TABLE_NAME + "/#",
                CODE_TRANSACTION_ID
        );

        // content://com.example.expensetracker.provider/statistics
        uriMatcher.addURI(
                ExpenseContract.AUTHORITY,
                ExpenseContract.StatisticsEntry.PATH_STATISTICS,
                CODE_STATISTICS
        );
    }

    @Override
    public boolean onCreate() {
        // 初始化数据库帮助类
        databaseHelper = new DatabaseHelper(getContext());
        Log.d(TAG, "ContentProvider已创建 - 数据访问接口层就绪");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "查询请求: " + uri.toString());

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor;

        switch (uriMatcher.match(uri)) {
            case CODE_TRANSACTIONS:
                // 查询所有交易记录
                cursor = db.query(
                        DatabaseHelper.TABLE_RECORDS,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                Log.d(TAG, "查询所有交易记录，返回 " + cursor.getCount() + " 条");
                break;

            case CODE_TRANSACTION_ID:
                // 查询特定ID的交易记录
                String id = uri.getLastPathSegment();
                String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
                String[] whereArgs = new String[]{id};

                cursor = db.query(
                        DatabaseHelper.TABLE_RECORDS,
                        projection,
                        whereClause,
                        whereArgs,
                        null,
                        null,
                        sortOrder
                );
                Log.d(TAG, "查询ID为 " + id + " 的交易记录");
                break;

            case CODE_STATISTICS:
                // 查询统计信息（演示虚拟表查询）
                cursor = queryStatistics(db);
                Log.d(TAG, "查询统计信息");
                break;

            default:
                throw new IllegalArgumentException("未知的查询URI: " + uri);
        }

        // 设置通知URI，当数据变化时通知观察者
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    /**
     * 查询统计信息（演示虚拟表）
     */
    private Cursor queryStatistics(SQLiteDatabase db) {
        // 创建统计查询
        String sql = "SELECT " +
                "COUNT(*) as total_count, " +
                "SUM(CASE WHEN " + DatabaseHelper.COLUMN_TYPE + " = " + Transaction.TYPE_EXPENSE +
                " THEN " + DatabaseHelper.COLUMN_AMOUNT + " ELSE 0 END) as total_expense, " +
                "SUM(CASE WHEN " + DatabaseHelper.COLUMN_TYPE + " = " + Transaction.TYPE_INCOME +
                " THEN " + DatabaseHelper.COLUMN_AMOUNT + " ELSE 0 END) as total_income " +
                "FROM " + DatabaseHelper.TABLE_RECORDS;

        return db.rawQuery(sql, null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.d(TAG, "插入请求: " + uri.toString());

        // 只支持向transactions表插入数据
        if (uriMatcher.match(uri) != CODE_TRANSACTIONS) {
            throw new IllegalArgumentException("插入操作只支持transactions URI");
        }

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        // 验证必要字段
        if (!values.containsKey(DatabaseHelper.COLUMN_CATEGORY)) {
            throw new IllegalArgumentException("必须提供category字段");
        }
        if (!values.containsKey(DatabaseHelper.COLUMN_AMOUNT)) {
            throw new IllegalArgumentException("必须提供amount字段");
        }

        // 插入数据
        long id = db.insert(DatabaseHelper.TABLE_RECORDS, null, values);

        if (id > 0) {
            // 构建新记录的URI
            Uri newUri = ContentUris.withAppendedId(
                    ExpenseContract.TransactionEntry.CONTENT_URI,
                    id
            );

            // 通知数据变化
            getContext().getContentResolver().notifyChange(newUri, null);
            getContext().getContentResolver().notifyChange(
                    ExpenseContract.StatisticsEntry.CONTENT_URI,
                    null
            );

            Log.d(TAG, "插入成功，新记录ID: " + id + ", URI: " + newUri);
            return newUri;
        }

        throw new SQLException("插入失败: " + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.d(TAG, "更新请求: " + uri.toString());

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int rowsUpdated;

        switch (uriMatcher.match(uri)) {
            case CODE_TRANSACTIONS:
                // 更新多条记录
                rowsUpdated = db.update(
                        DatabaseHelper.TABLE_RECORDS,
                        values,
                        selection,
                        selectionArgs
                );
                break;

            case CODE_TRANSACTION_ID:
                // 更新单条记录
                String id = uri.getLastPathSegment();
                String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
                String[] whereArgs = new String[]{id};

                rowsUpdated = db.update(
                        DatabaseHelper.TABLE_RECORDS,
                        values,
                        whereClause,
                        whereArgs
                );
                break;

            default:
                throw new IllegalArgumentException("未知的更新URI: " + uri);
        }

        if (rowsUpdated > 0) {
            // 通知数据变化
            getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(
                    ExpenseContract.StatisticsEntry.CONTENT_URI,
                    null
            );
            Log.d(TAG, "更新成功，影响 " + rowsUpdated + " 条记录");
        }

        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.d(TAG, "删除请求: " + uri.toString());

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case CODE_TRANSACTIONS:
                // 删除多条记录
                rowsDeleted = db.delete(
                        DatabaseHelper.TABLE_RECORDS,
                        selection,
                        selectionArgs
                );
                break;

            case CODE_TRANSACTION_ID:
                // 删除单条记录
                String id = uri.getLastPathSegment();
                String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
                String[] whereArgs = new String[]{id};

                rowsDeleted = db.delete(
                        DatabaseHelper.TABLE_RECORDS,
                        whereClause,
                        whereArgs
                );
                break;

            default:
                throw new IllegalArgumentException("未知的删除URI: " + uri);
        }

        if (rowsDeleted > 0) {
            // 通知数据变化
            getContext().getContentResolver().notifyChange(uri, null);
            getContext().getContentResolver().notifyChange(
                    ExpenseContract.StatisticsEntry.CONTENT_URI,
                    null
            );
            Log.d(TAG, "删除成功，删除 " + rowsDeleted + " 条记录");
        }

        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        // 返回MIME类型
        switch (uriMatcher.match(uri)) {
            case CODE_TRANSACTIONS:
                return ExpenseContract.TransactionEntry.CONTENT_TYPE;
            case CODE_TRANSACTION_ID:
                return ExpenseContract.TransactionEntry.CONTENT_ITEM_TYPE;
            case CODE_STATISTICS:
                return "vnd.android.cursor.dir/vnd." + ExpenseContract.AUTHORITY + ".statistics";
            default:
                throw new IllegalArgumentException("未知的URI: " + uri);
        }
    }
}