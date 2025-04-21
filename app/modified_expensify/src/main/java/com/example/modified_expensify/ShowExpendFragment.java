package com.example.modified_expensify;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.*;

public class ShowExpendFragment extends Fragment {

    private ListView myListView;
    private List<Expend> expendList;
    private DatabaseReference expendDbRef;
    private SyncManager syncManager;
    private ArrayAdapter<Expend> adapter;
    private CalendarView calendarView;
    private String date;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.show_expend, container, false);

        myListView = view.findViewById(R.id.myListView);
        calendarView = view.findViewById(R.id.calendarView);

        expendList = new ArrayList<>();
        syncManager = new SyncManager(requireContext());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userId = user.getUid();
        expendDbRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(userId)
                .child("Expenses");

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(year, month, dayOfMonth);
            this.date = dayOfMonth + "/" + (month + 1) + "/" + year;
            loadExpenses(this.date);
        });


        myListView.setOnItemLongClickListener((parent, view1, position, id) -> {
            Expend expense = expendList.get(position);
            confirmDelete(expense, position);
            return true;
        });

        return view;
    }

    private void loadExpenses(String date) {
        expendList = syncManager.loadExpenses(date);
        adapter = new ListAdapter(expendList);
        myListView.setAdapter(adapter);
    }

    private void confirmDelete(Expend expense, int position) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa chi tiêu này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    syncManager.deleteExpense(expense);
                    expendList.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Đã xóa chi tiêu!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class ListAdapter extends ArrayAdapter<Expend> {
        public ListAdapter(List<Expend> expendsList) {
            super(requireContext(), R.layout.list_item, expendsList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            TextView tvDay = convertView.findViewById(R.id.tvDate);
            TextView tvName = convertView.findViewById(R.id.tvName);
            TextView tvAmount = convertView.findViewById(R.id.tvAmount);
            TextView tvType = convertView.findViewById(R.id.tvType);
            TextView tvCategory = convertView.findViewById(R.id.tvCategory);

            Expend expend = getItem(position);
            if (expend != null) {
                tvDay.setText(expend.getDate());
                tvName.setText(expend.getName());
                tvAmount.setText(String.valueOf(expend.getAmount()));
                tvType.setText(expend.getType());
                tvCategory.setText(expend.getCategory());
            }

            return convertView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncManager.checkAndSync();
    }
}
