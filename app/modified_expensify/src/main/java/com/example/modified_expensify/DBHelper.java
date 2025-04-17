package com.example.modified_expensify;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "expenses.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_EXPENSES = "expenses";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FIREBASE_ID = "firebase_id";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_SYNC_STATUS = "sync_status";

    // Trạng thái đồng bộ
    public static final int SYNC_STATUS_OK = 0; // Đã đồng bộ với Firebase
    public static final int SYNC_STATUS_NEW = 1; // Mới tạo, chưa đồng bộ lên Firebase
    public static final int SYNC_STATUS_UPDATED = 2; // Đã cập nhật, chưa đồng bộ lên Firebase
    public static final int SYNC_STATUS_DELETED = 3; // Đã xóa, chưa đồng bộ lên Firebase

    private static final String SQL_CREATE_EXPENSES =
            "CREATE TABLE " + TABLE_EXPENSES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_FIREBASE_ID + " TEXT," +
                    COLUMN_DATE + " TEXT," +
                    COLUMN_NAME + " TEXT," +
                    COLUMN_AMOUNT + " REAL," +
                    COLUMN_TYPE + " TEXT," +
                    COLUMN_CATEGORY + " TEXT," +
                    COLUMN_SYNC_STATUS + " INTEGER DEFAULT " + SYNC_STATUS_NEW + ")";

    public static final String TABLE_PROFILE = "user_profile";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_NAME = "user_name";
    public static final String COLUMN_BIRDTH_DATE = "birth_date";
    public static final String COLUMN_AVATAR = "avatar";
    private static final String SQL_CREATE_PROFILE =
            "CREATE TABLE " + TABLE_PROFILE + " (" +
                    COLUMN_USER_ID + " TEXT," +
                    COLUMN_USER_NAME + " TEXT," +
                    COLUMN_BIRDTH_DATE + " TEXT," +
                    COLUMN_AVATAR + " TEXT)";
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_EXPENSES);
        db.execSQL(SQL_CREATE_PROFILE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       if (oldVersion < 2){
           db.execSQL(SQL_CREATE_PROFILE);
       }
    }
}

