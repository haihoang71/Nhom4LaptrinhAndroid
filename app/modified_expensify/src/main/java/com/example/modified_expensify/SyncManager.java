package com.example.modified_expensify;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncManager {
    private Context context;
    private ExpenseDAO expenseDAO;
    private FirebaseUser user;
    private DatabaseReference expenseDbRef;
    private ConnectivityManager connectivityManager;

    public SyncManager(Context context) {
        this.context = context;
        this.expenseDAO = new ExpenseDAO(context);
        this.user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            expenseDbRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId)
                    .child("Expenses");
        }
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    // Kiểm tra kết nối mạng
    public boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Đồng bộ từ Firebase xuống SQLite
    public void syncFromFirebase() {
        if (!isNetworkAvailable() || user == null) {
            return;
        }

        expenseDbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                expenseDAO.open();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String firebaseId = snapshot.getKey();
                    Expend expense = snapshot.getValue(Expend.class);
                    if (expense != null) {
                        expense.setId(firebaseId);

                        // Cập nhật hoặc thêm vào SQLite
                        if (expenseDAO.updateExpense(expense, firebaseId, DBHelper.SYNC_STATUS_OK) == 0) {
                            expenseDAO.addExpense(expense, firebaseId, DBHelper.SYNC_STATUS_OK);
                        }
                    }
                }

                expenseDAO.close();

                // Thông báo đã đồng bộ xong
                Toast.makeText(context, "Đã đồng bộ dữ liệu từ server", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("SyncManager", "Sync from Firebase error: " + error.getMessage());
                Toast.makeText(context, "Lỗi đồng bộ: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Đồng bộ từ SQLite lên Firebase
    public void syncToFirebase() {
        if (!isNetworkAvailable() || user == null) {
            Toast.makeText(context, "Không có kết nối mạng hoặc chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        expenseDAO.open();
        List<Expend> expensesToSync = expenseDAO.getExpensesToSync();
        expenseDAO.close();

        if (expensesToSync.isEmpty()) {
            Toast.makeText(context, "Không có dữ liệu cần đồng bộ", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Expend expense : expensesToSync) {
            switch (expense.getSyncStatus()) {
                case DBHelper.SYNC_STATUS_NEW:
                    addToFirebase(expense);
                    break;
                case DBHelper.SYNC_STATUS_UPDATED:
                    updateOnFirebase(expense);
                    break;
                case DBHelper.SYNC_STATUS_DELETED:
                    deleteFromFirebase(expense);
                    break;
            }
        }
    }

    // Thêm chi tiêu mới lên Firebase
    private void addToFirebase(final Expend expense) {
        // Tạo reference mới với ID tự động
        DatabaseReference newRef = expenseDbRef.push();
        String firebaseId = newRef.getKey();

        // Đặt giá trị không bao gồm ID và syncStatus
        Map<String, Object> expenseValues = new HashMap<>();
        expenseValues.put("date", expense.getDate());
        expenseValues.put("name", expense.getName());
        expenseValues.put("amount", expense.getAmount());
        expenseValues.put("type", expense.getType());
        expenseValues.put("category", expense.getCategory());

        newRef.setValue(expenseValues).addOnCompleteListener(task -> {
            expenseDAO.open();
            if (task.isSuccessful()) {
                // Cập nhật Firebase ID và trạng thái đồng bộ
                expenseDAO.updateFirebaseId(expense.getLocalId(), firebaseId);
                Toast.makeText(context, "Đã đồng bộ thêm mới lên server", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("SyncManager", "Error adding to Firebase: " + task.getException());
            }
            expenseDAO.close();
        });
    }

    // Cập nhật chi tiêu lên Firebase
    private void updateOnFirebase(final Expend expense) {
        String firebaseId = expense.getId();
        if (firebaseId == null || firebaseId.isEmpty()) {
            return;
        }

        Map<String, Object> expenseValues = new HashMap<>();
        expenseValues.put("date", expense.getDate());
        expenseValues.put("name", expense.getName());
        expenseValues.put("amount", expense.getAmount());
        expenseValues.put("type", expense.getType());
        expenseValues.put("category", expense.getCategory());

        expenseDbRef.child(firebaseId).updateChildren(expenseValues).addOnCompleteListener(task -> {
            expenseDAO.open();
            if (task.isSuccessful()) {
                // Cập nhật trạng thái đồng bộ
                expenseDAO.updateSyncStatus(firebaseId, DBHelper.SYNC_STATUS_OK);
                Toast.makeText(context, "Đã đồng bộ cập nhật lên server", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("SyncManager", "Error updating on Firebase: " + task.getException());
            }
            expenseDAO.close();
        });
    }

    // Xóa chi tiêu khỏi Firebase
    private void deleteFromFirebase(final Expend expense) {
        String firebaseId = expense.getId();
        if (firebaseId == null || firebaseId.isEmpty()) {
            return;
        }

        expenseDbRef.child(firebaseId).removeValue().addOnCompleteListener(task -> {
            expenseDAO.open();
            if (task.isSuccessful()) {
                // Xóa khỏi SQLite sau khi đã xóa thành công trên Firebase
                expenseDAO.deleteExpense(firebaseId);
                Toast.makeText(context, "Đã đồng bộ xóa lên server", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("SyncManager", "Error deleting from Firebase: " + task.getException());
            }
            expenseDAO.close();
        });
    }

    // Thêm chi tiêu mới (sẽ được gọi từ UI)
    public void addExpense(Expend expense) {
        // Thêm vào SQLite trước
        expenseDAO.open();
        long localId = expenseDAO.addExpense(expense, "", DBHelper.SYNC_STATUS_NEW);
        expense.setLocalId((int) localId);
        expenseDAO.close();

        // Nếu có mạng, đồng bộ ngay lên Firebase
        if (isNetworkAvailable() && user != null) {
            addToFirebase(expense);
        } else {
            Toast.makeText(context, "Đã lưu offline. Sẽ đồng bộ khi có kết nối mạng.", Toast.LENGTH_SHORT).show();
        }
    }

    // Cập nhật chi tiêu (sẽ được gọi từ UI)
    public void updateExpense(Expend expense) {
        // Cập nhật trong SQLite trước
        expenseDAO.open();
        expenseDAO.updateExpense(expense, expense.getId(), DBHelper.SYNC_STATUS_UPDATED);
        expenseDAO.close();

        // Nếu có mạng, đồng bộ ngay lên Firebase
        if (isNetworkAvailable() && user != null) {
            updateOnFirebase(expense);
        } else {
            Toast.makeText(context, "Đã lưu offline. Sẽ đồng bộ khi có kết nối mạng.", Toast.LENGTH_SHORT).show();
        }
    }

    // Xóa chi tiêu (sẽ được gọi từ UI)
    public void deleteExpense(Expend expense) {
        String firebaseId = expense.getId();

        // Đánh dấu xóa trong SQLite trước
        expenseDAO.open();
        if (firebaseId != null && !firebaseId.isEmpty()) {
            // Nếu đã có Firebase ID, đánh dấu để xóa sau
            expenseDAO.markExpenseDeleted(firebaseId);
        } else {
            // Nếu chưa có Firebase ID (chưa từng đồng bộ), xóa luôn
            expenseDAO.deleteExpense(firebaseId);
        }
        expenseDAO.close();

        // Nếu có mạng, đồng bộ ngay lên Firebase
        if (isNetworkAvailable() && user != null && firebaseId != null && !firebaseId.isEmpty()) {
            deleteFromFirebase(expense);
        } else if (firebaseId != null && !firebaseId.isEmpty()) {
            Toast.makeText(context, "Đã đánh dấu xóa offline. Sẽ đồng bộ khi có kết nối mạng.", Toast.LENGTH_SHORT).show();
        }
    }

    // Tải danh sách chi tiêu (ưu tiên từ SQLite)
    public List<Expend> loadExpenses() {
        expenseDAO.open();
        List<Expend> expenses = expenseDAO.getAllExpenses();
        expenseDAO.close();

        // Nếu có mạng và không có dữ liệu trong SQLite, đồng bộ từ Firebase
        if (expenses.isEmpty() && isNetworkAvailable() && user != null) {
            syncFromFirebase();
        }

        return expenses;
    }

    // Kiểm tra và đồng bộ khi có kết nối mạng
    public void checkAndSync() {
        if (isNetworkAvailable() && user != null) {
            syncToFirebase();  // Đẩy dữ liệu từ SQLite lên Firebase trước
            syncFromFirebase(); // Sau đó cập nhật dữ liệu từ Firebase xuống
        }
    }
}
