package com.example.modified_expensify;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Calendar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.view.Menu;
import android.view.MenuItem;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser user;
    private DatabaseReference userExpend;
    private String userId;
    private EditText expendName, expendAmount;
    private TextView expendDate;
    private Spinner expendType, expendCategory;
    private ImageButton bntCalendar;
    private Button bntAddExpend;

    private Map<String, List<String>> categoryMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        loadLocale(); // Ngôn ngữ

        View view = inflater.inflate(R.layout.main_fragment, container, false);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        expendDate = view.findViewById(R.id.editTextDate);
        expendName = view.findViewById(R.id.editTextExpenseName);
        expendAmount = view.findViewById(R.id.editTextExpenseAmount);
        expendType = view.findViewById(R.id.spinnerExpenseType);
        expendCategory = view.findViewById(R.id.spinnerExpenseCategory);
        bntAddExpend = view.findViewById(R.id.buttonAddExpense);
        bntCalendar = view.findViewById(R.id.bntCalendar);;

        if (user != null) {
            userId = user.getUid();
            userExpend = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Expenses");
        } else {
            Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return view;
        }

        bntCalendar.setOnClickListener(v -> openDialog());

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.expense_types,
                android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        expendType.setAdapter(typeAdapter);

        expendType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int arrayId = (position == 1) ? R.array.expend_categories : R.array.income_categories;
                ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                        getContext(),
                        arrayId,
                        android.R.layout.simple_spinner_item
                );
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                expendCategory.setAdapter(categoryAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        bntAddExpend.setOnClickListener(v -> insertExpendData());

        return view;
    }

    private void insertExpendData() {
        String date = expendDate.getText().toString();
        String name = expendName.getText().toString();
        String type = expendType.getSelectedItem() != null ? expendType.getSelectedItem().toString() : "Unknown";
        String amountText = expendAmount.getText().toString();
        String category = expendCategory.getSelectedItem() != null ? expendCategory.getSelectedItem().toString() : "Unknown";
        float amount;

        amount = Float.parseFloat(amountText);


        Expend expend = new Expend(date, name, amount, type, category, userId);

        SyncManager syncManager = new SyncManager(requireContext());
        syncManager.addExpense(expend);

        expendDate.setText("");
        expendName.setText("");
        expendAmount.setText("");
        expendType.setSelection(0);
        expendCategory.setSelection(0);
    }

    private void openDialog() {
        Calendar today = Calendar.getInstance();

        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            expendDate.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
        }, todayYear, todayMonth, todayDay);
        dialog.show();
    }

    private void loadLocale() {
        SharedPreferences prefs = requireContext().getSharedPreferences("Settings", getContext().MODE_PRIVATE);
        String langCode = prefs.getString("My_Lang", "vi");
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}