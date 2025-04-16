package com.example.modified_expensify;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button bntLogout;
    Button bntGetData;
    TextView textView;
    FirebaseUser user;

    EditText expendName, expendAmount;
    TextView expendDate;
    Spinner expendType;
    Spinner expendCategory;
    Button bntAddExpend;
    ImageButton bntCalendar;
    DatabaseReference userExpend;
    private Map<String, List<String>> categoryMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Logout button
        auth = FirebaseAuth.getInstance();
        bntLogout = findViewById(R.id.logout);
        bntGetData = findViewById(R.id.btnRetreiveData);
        textView = findViewById(R.id.user_details);
        user = auth.getCurrentUser();

        if (user == null){
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }else{
            textView.setText(user.getEmail());

        }

        bntLogout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(),Login.class);
                startActivity(intent);
                finish();
            }
        });
        bntGetData.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(MainActivity.this,Show_expend.class);
                startActivity(intent);
            }
        });

        // Adding expend
        expendDate = findViewById(R.id.editTextDate);
        expendName = findViewById(R.id.editTextExpenseName);
        expendAmount = findViewById(R.id.editTextExpenseAmount);
        expendType = findViewById(R.id.spinnerExpenseType);
        expendCategory = findViewById(R.id.spinnerExpenseCategory);
        bntAddExpend = findViewById(R.id.buttonAddExpense);
        bntCalendar = findViewById(R.id.bntCalendar);
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            userExpend = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Expenses");
        } else {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        bntCalendar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                openDialog();
            }
        });

        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.expense_types,
                android.R.layout.simple_spinner_item
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        expendType.setAdapter(typeAdapter);

        expendType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int arrayId;

                String selectedType = expendType.getSelectedItem().toString();

                if (position == 1) {
                    arrayId = R.array.expend_categories;
                } else {
                    arrayId = R.array.income_categories;
                }

                ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                        MainActivity.this,
                        arrayId,
                        android.R.layout.simple_spinner_item
                );
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                expendCategory.setAdapter(categoryAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        bntAddExpend.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                insertExpendData();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    private void insertExpendData(){
        String date = expendDate.getText().toString();
        String name = expendName.getText().toString();
        String type = expendType.getSelectedItem() != null ? expendType.getSelectedItem().toString() : "Unknown";
        String amountText = expendAmount.getText().toString();
        String category = expendCategory.getSelectedItem() != null ? expendCategory.getSelectedItem().toString() : "Unknown";
        float amount;

        try {
            amount = Float.parseFloat(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(MainActivity.this, "Invalid amount!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user == null || userExpend == null) {
            Toast.makeText(MainActivity.this, "User not authenticated or Database reference error!", Toast.LENGTH_SHORT).show();
            return;
        }

        Expend expend = new Expend(date, name, amount, type, category);

        userExpend.push().setValue(expend).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Record added successfully", Toast.LENGTH_SHORT).show();

                Log.e("Add", "Added", task.getException());

                expendDate.setText("");
                expendName.setText("");
                expendAmount.setText("");
                expendType.setSelection(0);
                expendCategory.setSelection(0);
            } else {
                Toast.makeText(MainActivity.this, "Failed to add record: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                Log.e("FirebaseError", "Failed to add record", task.getException());
            }
        });
    }
    private void openDialog(){
        DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                expendDate.setText(String.valueOf(day)+"/"+String.valueOf(month)+"/"+String.valueOf(year));
            }
        }, 2025, 1, 1);
        dialog.show();

    }
}