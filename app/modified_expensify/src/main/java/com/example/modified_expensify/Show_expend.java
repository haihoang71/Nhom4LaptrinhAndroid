package com.example.modified_expensify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Show_expend extends AppCompatActivity {
    private ListView myListView;
    private List<Expend> expendList;
    private DatabaseReference expendDbRef;
    private Button bntBack;
    private Button bntSync;
    private SyncManager syncManager;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_expend);

        myListView = findViewById(R.id.myListView);
        bntBack = findViewById(R.id.bntBack);
        bntSync = findViewById(R.id.bntSync);
        expendList = new ArrayList<>();

        syncManager = new SyncManager(this);

        // Thiết lập nút đồng bộ
        bntSync.setOnClickListener(v -> {
            Toast.makeText(Show_expend.this, "Đang đồng bộ dữ liệu...", Toast.LENGTH_SHORT).show();
            syncManager.checkAndSync();
        });
        // Thiết lập nút quay lại
        bntBack.setOnClickListener(v -> finish());


        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            expendDbRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("Expenses");
            //loadDataFromFirebase();
            loadExpenses();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    // load data từ sqlite
    private void loadExpenses() {
        // Tải dữ liệu từ SQLite thông qua SyncManager
        expendList = syncManager.loadExpenses();

        if (expendList.isEmpty()) {
            Toast.makeText(Show_expend.this, "Không có dữ liệu chi tiêu nào!", Toast.LENGTH_SHORT).show();
        } else {
            // Hiển thị dữ liệu lên ListView
            ListAdapter adapter = new ListAdapter(expendList);
            myListView.setAdapter(adapter);
        }
    }
    // load data từ firebase
    private void loadDataFromFirebase() {
        expendDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                expendList.clear();

                for (DataSnapshot expendDatasnap : dataSnapshot.getChildren()) {
                    try {
                        String id = expendDatasnap.getKey();
                        Expend expend = expendDatasnap.getValue(Expend.class);
                        if (expend != null) {
                            expend.setId(id);
                            expendList.add(expend);
                        }
                    } catch (Exception e) {
                        Log.e("Show_expend", "Error parsing data: " + e.getMessage());
                    }
                }

                if (expendList.isEmpty()) {
                    Toast.makeText(Show_expend.this, "Không có dữ liệu chi tiêu nào!", Toast.LENGTH_SHORT).show();
                } else {
                    // Hiển thị dữ liệu lên ListView
                    ListAdapter adapter = new ListAdapter(expendList);
                    myListView.setAdapter(adapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Show_expend", "Database error: " + error.getMessage());
                Toast.makeText(Show_expend.this, "Lỗi khi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private void deleteRecord(Expend expense) {
        // Hiển thị dialog xác nhận trước khi xóa
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa chi tiêu này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Thực hiện xóa thông qua SyncManager
                    syncManager.deleteExpense(expense);
                    // Cập nhật UI
                    expendList.remove(expense);
                    ((ListAdapter)myListView.getAdapter()).notifyDataSetChanged();
                    Toast.makeText(Show_expend.this, "Đã xóa chi tiêu!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private class ListAdapter extends ArrayAdapter<Expend>{
        public ListAdapter(List<Expend> expendsList){
            super(Show_expend.this, R.layout.show_expend, expendsList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent){
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
            }

            TextView tvDay = convertView.findViewById(R.id.tvDate);
            TextView tvName = convertView.findViewById(R.id.tvName);
            TextView tvAmount = convertView.findViewById(R.id.tvAmount);
            TextView tvType = convertView.findViewById(R.id.tvType);
            Button bntDelete = convertView.findViewById(R.id.bntDelete);

            Expend expend = getItem(position);
            Log.d("ListAdapter", "getView called at position: " + position);
            Log.d("ListAdapter", "Expend: " + expend.getName()); // Kiểm tra xem có dữ liệu không

            tvDay.setText(expend.getDate());
            tvName.setText(expend.getName());
            tvAmount.setText(String.valueOf(expend.getAmount()));
            tvType.setText(expend.getType());

            // Thiết lập sự kiện cho nút xóa
            bntDelete.setOnClickListener(view -> {
                deleteRecord(expend);
            });

            return convertView;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra và đồng bộ khi mở lại activity
        syncManager.checkAndSync();
    }
}


