package com.example.modified_expensify;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AnalysisFragment extends Fragment {

    private PieChart pieChart;
    private CombinedChart combinedChart;
    private RecyclerView recyclerView;
    private TextView tvMonthYear, tvExpense, tvIncome, tvBalance;
    private Spinner spinnerViewMode;
    private ImageView btnPrev, btnNext;
    private DBHelper dbHelper;
    private boolean isExpenseTab = true;

    private enum ViewMode { DAY, MONTH, YEAR }
    private ViewMode currentViewMode = ViewMode.DAY;
    private Calendar currentCalendar = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment's layout
        View view = inflater.inflate(R.layout.activity_chart, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize DatabaseHelper
        dbHelper = new DBHelper(requireContext());

        ExpenseDAO expenseDAO = new ExpenseDAO(requireContext());
        expenseDAO.open();

        // Initialize views
        pieChart = view.findViewById(R.id.pieChart);
        combinedChart = view.findViewById(R.id.combinedChart);
        recyclerView = view.findViewById(R.id.recyclerViewCategories);
        tvMonthYear = view.findViewById(R.id.tvMonthYear);
        tvExpense = view.findViewById(R.id.tvExpense);
        tvIncome = view.findViewById(R.id.tvIncome);
        tvBalance = view.findViewById(R.id.tvBalance);
        spinnerViewMode = view.findViewById(R.id.spinnerViewMode);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup tab switching
        view.findViewById(R.id.tabExpense).setOnClickListener(v -> {
            isExpenseTab = true;
            updateTabUI();
            loadChartData();
        });

        view.findViewById(R.id.tabIncome).setOnClickListener(v -> {
            isExpenseTab = false;
            updateTabUI();
            loadChartData();
        });

        // Setup spinner and buttons
        setupSpinner();
        setupButtons();
        updateTabUI();
        updateDateDisplay();
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = requireActivity().getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.view_modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerViewMode.setAdapter(adapter);

        spinnerViewMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentViewMode = ViewMode.values()[position];
                updateDateDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupButtons() {
        tvMonthYear.setOnClickListener(v -> showDatePicker());
        btnPrev.setOnClickListener(v -> {
            shiftDate(-1);
            updateDateDisplay();
        });
        btnNext.setOnClickListener(v -> {
            shiftDate(1);
            updateDateDisplay();
        });
    }

    private void showDatePicker() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);
        int day = currentCalendar.get(Calendar.DAY_OF_MONTH);

        if (currentViewMode == ViewMode.DAY) {
            new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                currentCalendar.set(y, m, d);
                updateDateDisplay();
            }, year, month, day).show();
        } else if (currentViewMode == ViewMode.MONTH) {
            MonthYearPickerDialog dialog = new MonthYearPickerDialog();
            dialog.setListener((y, m) -> {
                currentCalendar.set(Calendar.YEAR, y);
                currentCalendar.set(Calendar.MONTH, m);
                updateDateDisplay();
            });
            dialog.show(getParentFragmentManager(), "MonthYearPickerDialog");
        } else if (currentViewMode == ViewMode.YEAR) {
            YearPickerDialog dialog = new YearPickerDialog();
            dialog.setListener(y -> {
                currentCalendar.set(Calendar.YEAR, y);
                updateDateDisplay();
            });
            dialog.show(getParentFragmentManager(), "YearPickerDialog");
        }
    }

    private void shiftDate(int value) {
        switch (currentViewMode) {
            case DAY:
                currentCalendar.add(Calendar.DAY_OF_MONTH, value);
                break;
            case MONTH:
                currentCalendar.add(Calendar.MONTH, value);
                break;
            case YEAR:
                currentCalendar.add(Calendar.YEAR, value);
                break;
        }
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf;
        switch (currentViewMode) {
            case DAY:
                sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
                break;
            case MONTH:
                sdf = new SimpleDateFormat("yyyy/MM", Locale.getDefault());
                break;
            case YEAR:
                sdf = new SimpleDateFormat("yyyy", Locale.getDefault());
                break;
            default:
                sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        }
        tvMonthYear.setText(sdf.format(currentCalendar.getTime()));
        loadChartData(); // Automatically load chart data when date changes
    }

    private void updateTabUI() {
        TextView tabExpense = getView().findViewById(R.id.tabExpense);
        TextView tabIncome = getView().findViewById(R.id.tabIncome);
        int tabSelectedColor = getThemeColor(R.attr.tabSelectedColor);
        int tabUnselectedColor = getThemeColor(R.attr.tabUnselectedColor);

        if (isExpenseTab) {
            tabExpense.setBackgroundColor(tabSelectedColor);
            tabIncome.setBackgroundColor(tabUnselectedColor);

            pieChart.setVisibility(View.VISIBLE);
            combinedChart.setVisibility(View.GONE);

            tvExpense.setVisibility(View.VISIBLE);
            tvBalance.setVisibility(View.VISIBLE);
            tvIncome.setVisibility(View.VISIBLE);
        } else {
            tabExpense.setBackgroundColor(tabUnselectedColor);
            tabIncome.setBackgroundColor(tabSelectedColor);

            pieChart.setVisibility(View.GONE);
            combinedChart.setVisibility(View.VISIBLE);

            tvExpense.setVisibility(View.VISIBLE);
            tvBalance.setVisibility(View.VISIBLE);
            tvIncome.setVisibility(View.VISIBLE);
        }
    }

    private void loadChartData() {
        String formattedDate;
        String type = isExpenseTab ? "OUT" : "IN";
        Cursor cursor = null;
        double total = 0;
        double sizeTotal = 0;

        ExpenseDAO expenseDAO = new ExpenseDAO(requireContext());
        expenseDAO.open();

        switch (currentViewMode) {
            case DAY:
                formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.getTime());
                total = expenseDAO.getTotalByTypeAndDate(type, formattedDate);
                type = (!isExpenseTab) ? "OUT" : "IN";
                sizeTotal = expenseDAO.getTotalByTypeAndDate(type, formattedDate);
                cursor = isExpenseTab ? expenseDAO.getExpensesByDay(formattedDate) : expenseDAO.getIncomeByDay(formattedDate);
                break;

            case MONTH:
                formattedDate = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.getTime());
                total = expenseDAO.getMonthlyTotalByType(type, formattedDate);
                type = (!isExpenseTab) ? "OUT" : "IN";
                sizeTotal = expenseDAO.getMonthlyTotalByType(type, formattedDate);
                cursor = isExpenseTab ? expenseDAO.getExpensesByMonth(formattedDate) : expenseDAO.getIncomeByMonth(formattedDate);
                break;

            case YEAR:
                formattedDate = new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentCalendar.getTime());
                total = expenseDAO.getYearlyTotalByType(type, formattedDate);
                type = (!isExpenseTab) ? "OUT" : "IN";
                sizeTotal = expenseDAO.getYearlyTotalByType(type, formattedDate);
                cursor = isExpenseTab ? expenseDAO.getExpensesByYear(formattedDate) : expenseDAO.getIncomeByYear(formattedDate);
                break;

            default:
                return;
        }

        // Cập nhật thông tin tổng thu/chi và số dư
        tvExpense.setText(String.format("%s\n-%,.0f", getString(R.string.tab_expense), isExpenseTab ? total : sizeTotal));
        tvIncome.setText(String.format("%s\n+%,.0f", getString(R.string.tab_income), isExpenseTab ? sizeTotal : total));
        tvBalance.setText(String.format("%s\n%,.0f", getString(R.string.balance), isExpenseTab ? sizeTotal - total : total - sizeTotal));


        if (cursor != null && cursor.moveToFirst()) {
            List<CategoryExpenseAdapter.CategoryExpense> list = new ArrayList<>();
            List<PieEntry> pieEntries = new ArrayList<>();
            List<BarEntry> barEntries = new ArrayList<>();
            int index = 0;

            do {
                String category = cursor.getString(0);
                double amount = cursor.getDouble(1);
                double percent = (total > 0) ? (amount / total) * 100 : 0;

                list.add(new CategoryExpenseAdapter.CategoryExpense(category, amount, percent));

                if (isExpenseTab) {
                    pieEntries.add(new PieEntry((float) amount, category));
                } else {
                    barEntries.add(new BarEntry(index++, (float) amount));
                }
            } while (cursor.moveToNext());

            cursor.close();
            recyclerView.setAdapter(new CategoryExpenseAdapter(list));

            if (isExpenseTab) {
                PieDataSet dataSet = new PieDataSet(pieEntries, "");
                dataSet.setColors(new int[]{R.color.red, R.color.orange, R.color.green, R.color.blue, R.color.purple}, requireContext());
                dataSet.setValueTextSize(14f);
                dataSet.setSliceSpace(2f);

                pieChart.setData(new PieData(dataSet));
                pieChart.setUsePercentValues(false);
                pieChart.setDrawHoleEnabled(true);
                pieChart.setEntryLabelTextSize(12f);
                pieChart.getDescription().setEnabled(false);
                pieChart.getLegend().setEnabled(false);
                pieChart.invalidate();
            } else {
                BarDataSet barDataSet = new BarDataSet(barEntries, "Thu nhập theo danh mục");
                barDataSet.setColors(new int[]{R.color.red, R.color.orange, R.color.green, R.color.blue, R.color.purple}, requireContext());
                barDataSet.setValueTextSize(14f);

                CombinedData combinedData = new CombinedData();
                combinedData.setData(new BarData(barDataSet));

                combinedChart.setData(combinedData);
                combinedChart.getDescription().setEnabled(false);
                combinedChart.getXAxis().setEnabled(false);
                combinedChart.getAxisLeft().setAxisMinimum(0);
                combinedChart.getAxisRight().setEnabled(false);
                combinedChart.invalidate();
            }
        }else{
            recyclerView.setAdapter(new CategoryExpenseAdapter(new ArrayList<>()));

            if (isExpenseTab) {
                // Reset PieChart
                pieChart.clear();
                pieChart.invalidate();
            } else {
                // Reset CombinedChart (Bar)
                combinedChart.clear();
                combinedChart.invalidate();
            }
        }
    }

}