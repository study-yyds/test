package com.example.expensetracker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private OnItemLongClickListener onItemLongClickListener;
    private Context context;

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);

        // 根据类别设置图标
        setCategoryIcon(holder.ivCategoryIcon, transaction.getCategory());

        // 修复：使用getAdapterPosition()获取实时位置
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onItemLongClickListener != null) {
                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onItemLongClickListener.onItemLongClick(adapterPosition);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    /**
     * 根据交易类别设置对应的图标
     * @param imageView 图标控件
     * @param category 交易类别（如：餐饮、交通、购物等）
     */
    private void setCategoryIcon(ImageView imageView, String category) {
        int iconResId;
        int backgroundColorResId;

        // 使用 switch-case 语句根据不同类别设置不同图标
        switch (category) {
            case "餐饮":
            case "早餐":
            case "午餐":
            case "晚餐":
            case "外卖":
            case "零食":
            case "饮料":
                iconResId = R.drawable.ic_food;
                backgroundColorResId = R.color.food_color;
                break;

            case "交通":
            case "公交":
            case "地铁":
            case "打车":
            case "加油":
            case "停车":
            case "出行":
                iconResId = R.drawable.ic_transport;
                backgroundColorResId = R.color.transport_color;
                break;

            case "购物":
            case "超市":
            case "网购":
            case "日用品":
            case "服饰":
            case "家电":
            case "数码":
                iconResId = R.drawable.ic_shopping;
                backgroundColorResId = R.color.shopping_color;
                break;

            case "娱乐":
            case "电影":
            case "游戏":
            case "旅游":
            case "健身":
            case "KTV":
            case "休闲":
                iconResId = R.drawable.ic_entertainment;
                backgroundColorResId = R.color.entertainment_color;
                break;

            case "工资":
            case "奖金":
            case "收入":
            case "兼职":
            case "理财":
            case "红包":
                iconResId = R.drawable.ic_salary;
                backgroundColorResId = R.color.salary_color;
                break;

            case "医疗":
            case "看病":
            case "药品":
            case "体检":
                iconResId = R.drawable.ic_medical;
                backgroundColorResId = R.color.medical_color;
                break;

            case "通讯":
            case "话费":
            case "流量":
            case "网络":
                iconResId = R.drawable.ic_communication;
                backgroundColorResId = R.color.communication_color;
                break;

            case "教育":
            case "学习":
            case "培训":
            case "书籍":
            case "课程":
                iconResId = R.drawable.ic_education;
                backgroundColorResId = R.color.education_color;
                break;

            // 其他类别使用默认图标
            default:
                iconResId = R.drawable.ic_default_category;
                backgroundColorResId = R.color.default_color;
                break;
        }

        // 设置图标
        Drawable drawable = ContextCompat.getDrawable(context, iconResId);
        if (drawable != null) {
            // 对图标进行着色，使其与背景颜色匹配
            drawable.setTint(ContextCompat.getColor(context, backgroundColorResId));
        }
        imageView.setImageDrawable(drawable);

        // 设置背景圆角（使用图标背景资源）
        imageView.setBackgroundResource(R.drawable.icon_background);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCategoryIcon;
        private TextView tvAmount;
        private TextView tvType;
        private TextView tvCategory;
        private TextView tvDate;
        private TextView tvNote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvType = itemView.findViewById(R.id.tv_type);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvNote = itemView.findViewById(R.id.tv_note);
        }

        public void bind(Transaction transaction) {
            // 设置金额
            tvAmount.setText(String.format("¥%.2f", transaction.getAmount()));

            // 设置收支类型和颜色
            if (transaction.isExpense()) {
                tvType.setText("支出");
                tvType.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvType.setText("收入");
                tvType.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
                tvAmount.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            }

            // 设置类别
            tvCategory.setText(transaction.getCategory());

            // 设置日期
            tvDate.setText(transaction.getDate());

            // 设置备注
            if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
                tvNote.setText(transaction.getNote());
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }
        }
    }
}