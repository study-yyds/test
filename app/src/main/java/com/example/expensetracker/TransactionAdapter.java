package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Transaction> transactions;
    private OnItemLongClickListener onItemLongClickListener;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);

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
    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvAmount;
        private TextView tvType;
        private TextView tvCategory;
        private TextView tvDate;
        private TextView tvNote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
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
