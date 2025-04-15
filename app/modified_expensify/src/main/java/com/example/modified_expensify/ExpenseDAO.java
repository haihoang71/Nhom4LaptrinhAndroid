package com.example.modified_expensify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private Context context;

    public ExpenseDAO(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    // Thêm chi tiêu mới vào SQLite
    public long addExpense(Expend expense, String firebaseId, int syncStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_FIREBASE_ID, firebaseId);
        values.put(DBHelper.COLUMN_DATE, expense.getDate());
        values.put(DBHelper.COLUMN_NAME, expense.getName());
        values.put(DBHelper.COLUMN_AMOUNT, expense.getAmount());
        values.put(DBHelper.COLUMN_TYPE, expense.getType());
        values.put(DBHelper.COLUMN_SYNC_STATUS, syncStatus);

        return database.insert(DBHelper.TABLE_EXPENSES, null, values);
    }

    // Cập nhật chi tiêu trong SQLite
    public int updateExpense(Expend expense, String firebaseId, int syncStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_DATE, expense.getDate());
        values.put(DBHelper.COLUMN_NAME, expense.getName());
        values.put(DBHelper.COLUMN_AMOUNT, expense.getAmount());
        values.put(DBHelper.COLUMN_TYPE, expense.getType());
        values.put(DBHelper.COLUMN_SYNC_STATUS, syncStatus);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_FIREBASE_ID + " = ?",
                new String[]{firebaseId});
    }

    // Đánh dấu xóa chi tiêu (không xóa thật sự, chỉ đánh dấu để đồng bộ lên Firebase sau)
    public int markExpenseDeleted(String firebaseId) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SYNC_STATUS, DBHelper.SYNC_STATUS_DELETED);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_FIREBASE_ID + " = ?",
                new String[]{firebaseId});
    }

    // Xóa chi tiêu khỏi SQLite sau khi đã đồng bộ với Firebase
    public int deleteExpense(String firebaseId) {
        return database.delete(DBHelper.TABLE_EXPENSES,
                DBHelper.COLUMN_FIREBASE_ID + " = ?",
                new String[]{firebaseId});
    }

    // Lấy tất cả chi tiêu từ SQLite
    public List<Expend> getAllExpenses() {
        List<Expend> expenses = new ArrayList<>();
        Cursor cursor = database.query(DBHelper.TABLE_EXPENSES,
                null,
                DBHelper.COLUMN_SYNC_STATUS + " != ?",
                new String[]{String.valueOf(DBHelper.SYNC_STATUS_DELETED)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_FIREBASE_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TYPE));

                Expend expense = new Expend(date, name, amount, type);
                expense.setId(firebaseId);
                expenses.add(expense);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    // Lấy các chi tiêu cần đồng bộ (mới, cập nhật, xóa)
    public List<Expend> getExpensesToSync() {
        List<Expend> expenses = new ArrayList<>();
        Cursor cursor = database.query(DBHelper.TABLE_EXPENSES,
                null,
                DBHelper.COLUMN_SYNC_STATUS + " != ?",
                new String[]{String.valueOf(DBHelper.SYNC_STATUS_OK)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_FIREBASE_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TYPE));
                int syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_SYNC_STATUS));

                Expend expense = new Expend(date, name, amount, type);
                expense.setId(firebaseId);
                // Thêm thông tin trạng thái đồng bộ để xử lý sau
                expense.setSyncStatus(syncStatus);
                expense.setLocalId(id);
                expenses.add(expense);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    // Cập nhật trạng thái đồng bộ
    public int updateSyncStatus(String firebaseId, int syncStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SYNC_STATUS, syncStatus);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_FIREBASE_ID + " = ?",
                new String[]{firebaseId});
    }

    // Cập nhật Firebase ID cho bản ghi mới
    public int updateFirebaseId(int localId, String firebaseId) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_FIREBASE_ID, firebaseId);
        values.put(DBHelper.COLUMN_SYNC_STATUS, DBHelper.SYNC_STATUS_OK);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(localId)});
    }
}

