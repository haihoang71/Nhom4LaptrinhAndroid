package com.example.modified_expensify;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button bntLogout;
    Button bntGetData;
    TextView textView;
    FirebaseUser user;

    EditText expendDate, expendName, expendAmount;
    Spinner expendType;
    Button bntAddExpend;
    DatabaseReference userExpend;
    Button getData;


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
        expendType = findViewById(R.id.spinnerExpenseCategory);
        bntAddExpend = findViewById(R.id.buttonAddExpense);
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            userExpend = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("Expenses");
        } else {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

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

        Expend expend = new Expend(date, name, amount, type);

        userExpend.push().setValue(expend).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Record added successfully", Toast.LENGTH_SHORT).show();

                expendDate.setText("");
                expendName.setText("");
                expendAmount.setText("");
                expendType.setSelection(0);
            } else {
                Toast.makeText(MainActivity.this, "Failed to add record: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                Log.e("FirebaseError", "Failed to add record", task.getException());
            }
        });
    }
}