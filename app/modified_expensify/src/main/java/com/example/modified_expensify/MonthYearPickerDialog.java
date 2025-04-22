package com.example.modified_expensify;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class MonthYearPickerDialog extends DialogFragment {

    public interface OnDateSetListener {
        void onDateSet(int year, int month);
    }

    private OnDateSetListener listener;

    public void setListener(OnDateSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_month_year_picker);

        NumberPicker monthPicker = dialog.findViewById(R.id.picker_month);
        NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);

        final Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        String[] monthLabels = getResources().getStringArray(R.array.months_array);
        monthPicker.setDisplayedValues(monthLabels);
        monthPicker.setValue(currentMonth);

        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear);

        dialog.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            if (listener != null) {
                listener.onDateSet(yearPicker.getValue(), monthPicker.getValue());
            }
            dismiss();
        });

        dialog.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        return dialog;
    }
}
