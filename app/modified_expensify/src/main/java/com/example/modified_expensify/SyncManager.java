package com.example.modified_expensify;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.List;

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

    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void addExpense(Expend expense) {
        expenseDAO.open();
        long localId = expenseDAO.addExpense(expense, "", DBHelper.SYNC_STATUS_NEW);
        expense.setLocalId((int) localId);
        expenseDAO.close();


        if (isNetworkAvailable() && user != null) {
            addToFirebase(expense);
        }
    }

    public void deleteExpense(Expend expense) {
        String firebaseId = expense.getId();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (firebaseId != null && !firebaseId.isEmpty()) {
            if (isNetworkAvailable() && user != null) {
                expenseDbRef.child(firebaseId).removeValue().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        expenseDAO.open();
                        expenseDAO.deleteExpense(firebaseId, userId);
                        expenseDAO.close();
                    } else {
                        Log.e("SyncManager", "Xoá Firebase thất bại: " + task.getException());
                    }
                });
            } else {
                expenseDAO.open();
                expenseDAO.deleteExpense(firebaseId, userId);
                expenseDAO.close();
            }
        } else {
            Log.w("SyncManager", "Không có firebaseId để xóa!");
        }
    }


    public void checkAndSync() {
        if (!isNetworkAvailable() || user == null) return;

        syncToFirebase();
        syncFromFirebase();
    }

    private void syncToFirebase() {
        expenseDAO.open();
        List<Expend> expensesToSync = expenseDAO.getExpensesToSync();
        expenseDAO.close();

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

    private void syncFromFirebase() {
        expenseDbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                expenseDAO.open();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Expend expense = snap.getValue(Expend.class);
                    if (expense != null) {
                        String firebaseId = snap.getKey();
                        expense.setId(firebaseId);
                        if (expenseDAO.updateExpense(expense, firebaseId, DBHelper.SYNC_STATUS_OK) == 0) {
                            expenseDAO.addExpense(expense, firebaseId, DBHelper.SYNC_STATUS_OK);
                        }
                    }
                }
                expenseDAO.close();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("SyncManager", "Firebase read error: " + error.getMessage());
            }
        });
    }

    private void addToFirebase(final Expend expense) {
        DatabaseReference newRef = expenseDbRef.push();
        String firebaseId = newRef.getKey();

        if (firebaseId == null) return;

        newRef.setValue(expense).addOnCompleteListener(task -> {
            expenseDAO.open();
            if (task.isSuccessful()) {
                expenseDAO.updateFirebaseId(expense.getLocalId(), firebaseId);
            }
            expenseDAO.close();
        });
    }

    private void updateOnFirebase(final Expend expense) {
        String firebaseId = expense.getId();
        if (firebaseId == null || firebaseId.isEmpty()) return;

        expenseDbRef.child(firebaseId).setValue(expense).addOnCompleteListener(task -> {
            expenseDAO.open();
            if (task.isSuccessful()) {
                expenseDAO.updateSyncStatus(firebaseId, DBHelper.SYNC_STATUS_OK);
            }
            expenseDAO.close();
        });
    }

    private void deleteFromFirebase(final Expend expense) {
        String firebaseId = expense.getId();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (firebaseId == null || firebaseId.isEmpty()) return;

        expenseDbRef.child(firebaseId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                expenseDAO.open();
                expenseDAO.deleteExpense(firebaseId, userId);
                expenseDAO.close();
            }
        });
    }

    public List<Expend> loadExpenses(String date) {
        expenseDAO.open();
        List<Expend> list = expenseDAO.getAllExpenses(date);
        expenseDAO.close();
        return list;
    }
}
