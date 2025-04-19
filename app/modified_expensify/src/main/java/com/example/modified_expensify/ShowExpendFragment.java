package com.example.modified_expensify;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class ShowExpendFragment extends Fragment {

    private ListView myListView;
    private List<Expend> expendList;
    private DatabaseReference expendDbRef;
    private Button bntSync;
    private SyncManager syncManager;

    private ArrayAdapter<Expend> adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.show_expend, container, false);

        myListView = view.findViewById(R.id.myListView);
        bntSync = view.findViewById(R.id.bntSync);

        expendList = new ArrayList<>();
        syncManager = new SyncManager(requireContext());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            expendDbRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("Expenses");

            loadExpenses(); // load từ SQLite
        } else {
            Toast.makeText(getContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }

        bntSync.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show();
            syncManager.checkAndSync();
            loadExpenses(); // reload lại sau khi sync
        });

        return view;
    }

    private void loadExpenses() {
        expendList = syncManager.loadExpenses();

        if (expendList.isEmpty()) {
            Toast.makeText(getContext(), "Không có dữ liệu chi tiêu nào!", Toast.LENGTH_SHORT).show();
        } else {
            adapter = new ListAdapter(expendList);
            myListView.setAdapter(adapter);
        }
    }

    private void deleteRecord(Expend expense) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa chi tiêu này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    syncManager.deleteExpense(expense);
                    expendList.remove(expense);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Đã xóa chi tiêu!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class ListAdapter extends ArrayAdapter<Expend> {
        public ListAdapter(List<Expend> expendsList) {
            super(requireContext(), R.layout.show_expend, expendsList);
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
            Button bntDelete = convertView.findViewById(R.id.bntDelete);

            Expend expend = getItem(position);
            if (expend != null) {
                tvDay.setText(expend.getDate());
                tvName.setText(expend.getName());
                tvAmount.setText(String.valueOf(expend.getAmount()));
                tvType.setText(expend.getType());
                tvCategory.setText(expend.getCategory());

                bntDelete.setOnClickListener(v -> deleteRecord(expend));
            }

            return convertView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncManager.checkAndSync();
        loadExpenses();
    }
}
