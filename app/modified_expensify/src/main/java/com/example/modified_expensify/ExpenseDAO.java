package com.example.modified_expensify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private Context context;

    public final String userId;

    public ExpenseDAO(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        this.userId = user.getUid();
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long addExpense(Expend expense, String firebaseId, int syncStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_EXPEND_USER_ID, userId);
        values.put(DBHelper.COLUMN_FIREBASE_ID, firebaseId);
        values.put(DBHelper.COLUMN_DATE, expense.getDate());
        values.put(DBHelper.COLUMN_NAME, expense.getName());
        values.put(DBHelper.COLUMN_AMOUNT, expense.getAmount());
        values.put(DBHelper.COLUMN_TYPE, expense.getType());
        values.put(DBHelper.COLUMN_CATEGORY, expense.getCategory());
        values.put(DBHelper.COLUMN_SYNC_STATUS, syncStatus);

        return database.insert(DBHelper.TABLE_EXPENSES, null, values);
    }

    public int updateExpense(Expend expense, String firebaseId, int syncStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_EXPEND_USER_ID, userId);
        values.put(DBHelper.COLUMN_DATE, expense.getDate());
        values.put(DBHelper.COLUMN_NAME, expense.getName());
        values.put(DBHelper.COLUMN_AMOUNT, expense.getAmount());
        values.put(DBHelper.COLUMN_TYPE, expense.getType());
        values.put(DBHelper.COLUMN_CATEGORY, expense.getCategory());
        values.put(DBHelper.COLUMN_SYNC_STATUS, syncStatus);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_FIREBASE_ID + " = ?",
                new String[]{firebaseId});
    }

    public int deleteExpense(String firebaseId, String userId) {
        return database.delete(DBHelper.TABLE_EXPENSES,
                DBHelper.COLUMN_FIREBASE_ID + " = ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + " = ?",
                new String[]{firebaseId, userId});
    }

    // Lấy tất cả chi tiêu từ SQLite
    public List<Expend> getAllExpenses(String chooseDate) {
        List<Expend> expenses = new ArrayList<>();
        Cursor cursor = database.query(DBHelper.TABLE_EXPENSES,
                null,
                DBHelper.COLUMN_SYNC_STATUS + " != ? AND "
                        + DBHelper.COLUMN_DATE + " =? AND "
                        + DBHelper.COLUMN_EXPEND_USER_ID + " =?",
                new String[]{String.valueOf(DBHelper.SYNC_STATUS_DELETED), chooseDate, userId},
                null,null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_FIREBASE_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CATEGORY));

                Expend expense = new Expend(date, name, amount, type, category);
                expense.setId(firebaseId);
                expenses.add(expense);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    public List<Expend> getExpensesToSync() {
        List<Expend> expenses = new ArrayList<>();
        Cursor cursor = database.query(DBHelper.TABLE_EXPENSES,
                null,
                DBHelper.COLUMN_SYNC_STATUS + " != ? AND "
                        + DBHelper.COLUMN_EXPEND_USER_ID + " =?",
                new String[]{String.valueOf(DBHelper.SYNC_STATUS_OK), userId},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_ID));
                String firebaseId = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_FIREBASE_ID));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_DATE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_NAME));
                float amount = cursor.getFloat(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AMOUNT));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_CATEGORY));
                int syncStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_SYNC_STATUS));

                Expend expense = new Expend(date, name, amount, type, category);
                expense.setId(firebaseId);
                expense.setSyncStatus(syncStatus);
                expense.setLocalId(id);
                expenses.add(expense);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return expenses;
    }

    public int updateSyncStatus(String firebaseId, int syncStatus) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SYNC_STATUS, syncStatus);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_FIREBASE_ID + " = ? AND "
                + DBHelper.COLUMN_EXPEND_USER_ID + " =?",
                new String[]{firebaseId, userId});
    }

    public int updateFirebaseId(int localId, String firebaseId) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_FIREBASE_ID, firebaseId);
        values.put(DBHelper.COLUMN_SYNC_STATUS, DBHelper.SYNC_STATUS_OK);

        return database.update(DBHelper.TABLE_EXPENSES,
                values,
                DBHelper.COLUMN_ID + " = ? AND "
                        + DBHelper.COLUMN_EXPEND_USER_ID + " =?",
                new String[]{String.valueOf(localId), userId});
    }

    // Tổng thu/chi theo loại và ngày cụ thể
    public double getTotalByTypeAndDate(String type, String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String type_en, type_vn, type_ja;
        if (type.equals("IN")){
            type_vn = "Thu nhập";
            type_en = "Income";
            type_ja = "収入";
        }else{
            type_vn = "Chi tiêu";
            type_en = "Expense";
            type_ja = "支出";
        }
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + DBHelper.COLUMN_AMOUNT + ") FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (" + DBHelper.COLUMN_TYPE + " =? OR " + DBHelper.COLUMN_TYPE + " =? OR " + DBHelper.COLUMN_TYPE + " =?)"+ " AND " + DBHelper.COLUMN_DATE + "=? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=?",
                new String[]{type_vn, type_en, type_ja, date, userId}
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public double getMonthlyTotalByType(String type, String month) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String type_en, type_vn, type_ja;
        if (type.equals("IN")){
            type_vn = "Thu nhập";
            type_en = "Income";
            type_ja = "収入";
        }else{
            type_vn = "Chi tiêu";
            type_en = "Expense";
            type_ja = "支出";
        }
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + DBHelper.COLUMN_AMOUNT + ") FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (" + DBHelper.COLUMN_TYPE + " =? OR " + DBHelper.COLUMN_TYPE + " =? OR " + DBHelper.COLUMN_TYPE + " =?)" + " AND " + DBHelper.COLUMN_DATE + " LIKE ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=?",
                new String[]{type_vn, type_en, type_ja, month + "%", userId}
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public double getYearlyTotalByType(String type, String year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String type_en, type_vn, type_ja;
        if (type.equals("IN")){
            type_vn = "Thu nhập";
            type_en = "Income";
            type_ja = "収入";
        }else{
            type_vn = "Chi tiêu";
            type_en = "Expense";
            type_ja = "支出";
        }
        Cursor cursor = db.rawQuery(
                "SELECT SUM(" + DBHelper.COLUMN_AMOUNT + ") FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (" + DBHelper.COLUMN_TYPE + " =? OR " + DBHelper.COLUMN_TYPE + " =? OR " + DBHelper.COLUMN_TYPE + " =?)" + " AND " + DBHelper.COLUMN_DATE + " LIKE ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=?",
                new String[]{type_vn, type_en, type_ja, year + "%", userId}
        );
        double total = 0;
        if (cursor.moveToFirst()) total = cursor.getDouble(0);
        cursor.close();
        return total;
    }

    public Cursor getExpensesByDay(String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT category, SUM(amount) FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (type = 'Chi tiêu' OR type = 'Expense' OR type = '支出') AND date = ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=? GROUP BY category",
                new String[]{date, userId}
        );
    }

    public Cursor getExpensesByMonth(String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT category, SUM(amount) FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (type = 'Chi tiêu' OR type = 'Expense' OR type = '支出') AND date LIKE ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=? GROUP BY category",
                new String[]{monthYear + "%", userId}
        );
    }

    public Cursor getExpensesByYear(String year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT category, SUM(amount) FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (type = 'Chi tiêu' OR type = 'Expense' OR type = '支出') AND date LIKE ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=? GROUP BY category",
                new String[]{year + "%", userId}
        );
    }

    public Cursor getIncomeByDay(String date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT category, SUM(amount) FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (type = 'Thu nhập' OR type = 'Income' OR type = '収入') AND date = ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=? GROUP BY category",
                new String[]{date, userId}
        );
    }

    public Cursor getIncomeByMonth(String monthYear) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT category, SUM(amount) FROM " + DBHelper.TABLE_EXPENSES +
                " WHERE (type ='Thu nhập' OR type = 'Income' OR type = '収入') AND date LIKE ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=? GROUP BY category",
                new String[]{monthYear + "%", userId}
        );
    }

    public Cursor getIncomeByYear(String year) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return db.rawQuery(
                "SELECT category, SUM(amount) FROM " + DBHelper.TABLE_EXPENSES +
                        " WHERE (type = 'Thu nhập' OR type = 'Income' OR type = '収入') AND date LIKE ? AND " + DBHelper.COLUMN_EXPEND_USER_ID + "=? GROUP BY category",
                new String[]{year + "%", userId}
        );
    }
}

