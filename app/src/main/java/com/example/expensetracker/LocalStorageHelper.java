package com.example.expensetracker;

import android.content.Context;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地存储工具类
 * 负责将消费记录数据持久化存储到设备本地文件
 *
 * 教学要点：
 * 1. 内部存储：数据存储在App私有目录，其他App无法访问
 * 2. 对象序列化：将Java对象转换为字节流
 * 3. 文件I/O操作：读写文件的基本流程
 * 4. 异常处理：确保存储过程稳定可靠
 */
public class LocalStorageHelper {

    private static final String TAG = "LocalStorageHelper";
    private static final String FILE_NAME = "transactions.dat";

    private Context context;

    /**
     * 构造函数
     * @param context 上下文对象，用于获取文件存储路径
     */
    public LocalStorageHelper(Context context) {
        this.context = context;
    }

    /**
     * 保存消费记录列表到本地文件
     * @param transactions 消费记录列表
     * @return 是否保存成功
     *
     * 存储流程：
     * 1. 打开文件输出流
     * 2. 创建对象输出流
     * 3. 将List<Transaction>序列化为字节流
     * 4. 写入文件
     * 5. 关闭流
     */
    public boolean saveTransactions(List<Transaction> transactions) {
        Log.d(TAG, "开始保存消费记录，共" + transactions.size() + "条");

        try {
            // 1. 打开文件输出流（MODE_PRIVATE：私有文件，其他App无法访问）
            FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);

            // 2. 创建对象输出流（用于序列化对象）
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            // 3. 写入对象到文件
            oos.writeObject(transactions);

            // 4. 关闭流（重要：确保数据写入磁盘）
            oos.close();
            fos.close();

            Log.d(TAG, "消费记录保存成功");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "保存消费记录失败", e);
            return false;
        }
    }

    /**
     * 从本地文件加载消费记录列表
     * @return 消费记录列表（如果文件不存在或读取失败，返回空列表）
     *
     * 加载流程：
     * 1. 检查文件是否存在
     * 2. 打开文件输入流
     * 3. 创建对象输入流
     * 4. 反序列化字节流为List<Transaction>
     * 5. 关闭流
     */
    @SuppressWarnings("unchecked")
    public List<Transaction> loadTransactions() {
        Log.d(TAG, "开始加载消费记录");

        try {
            // 1. 检查文件是否存在
            if (!fileExists()) {
                Log.d(TAG, "存储文件不存在，返回空列表");
                return new ArrayList<>();
            }

            // 2. 打开文件输入流
            FileInputStream fis = context.openFileInput(FILE_NAME);

            // 3. 创建对象输入流（用于反序列化对象）
            ObjectInputStream ois = new ObjectInputStream(fis);

            // 4. 读取对象（需要类型转换）
            List<Transaction> transactions = (List<Transaction>) ois.readObject();

            // 5. 关闭流
            ois.close();
            fis.close();

            Log.d(TAG, "消费记录加载成功，共" + transactions.size() + "条");
            return transactions;

        } catch (Exception e) {
            Log.e(TAG, "加载消费记录失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 清空本地存储文件
     * @return 是否清空成功
     */
    public boolean clearStorage() {
        Log.d(TAG, "开始清空本地存储");

        try {
            // 删除存储文件
            boolean deleted = context.deleteFile(FILE_NAME);

            if (deleted) {
                Log.d(TAG, "本地存储清空成功");
            } else {
                Log.d(TAG, "本地存储文件不存在或删除失败");
            }

            return deleted;

        } catch (Exception e) {
            Log.e(TAG, "清空本地存储失败", e);
            return false;
        }
    }

    /**
     * 检查存储文件是否存在
     * @return 文件是否存在
     */
    public boolean fileExists() {
        String[] files = context.fileList();
        for (String file : files) {
            if (file.equals(FILE_NAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取存储文件路径（用于调试）
     * @return 文件完整路径
     */
    public String getStoragePath() {
        return context.getFilesDir() + "/" + FILE_NAME;
    }
}