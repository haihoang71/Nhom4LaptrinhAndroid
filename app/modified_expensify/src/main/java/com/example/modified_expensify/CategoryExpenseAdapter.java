package com.example.modified_expensify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryExpenseAdapter extends RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder> {

    public static class CategoryExpense {
        public final String category;
        public final double amount;
        public final double percent;

        public CategoryExpense(String category, double amount, double percent) {
            this.category = category;
            this.amount = amount;
            this.percent = percent;
        }
    }

    private final List<CategoryExpense> list;

    public CategoryExpenseAdapter(List<CategoryExpense> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryExpense item = list.get(position);
        holder.tvCategory.setText(item.category);
        holder.tvAmount.setText(String.format("%,.0fđ", item.amount));
        holder.tvPercent.setText(String.format("%.1f%%", item.percent));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvPercent;

        ViewHolder(View view) {
            super(view);
            tvCategory = view.findViewById(R.id.tvCategory);
            tvAmount = view.findViewById(R.id.tvAmount);
            tvPercent = view.findViewById(R.id.tvPercent);
        }
    }
}