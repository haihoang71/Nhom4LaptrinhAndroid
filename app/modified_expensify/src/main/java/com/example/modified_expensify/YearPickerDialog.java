package com.example.modified_expensify;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class YearPickerDialog extends DialogFragment {

    public interface OnYearSetListener {
        void onYearSet(int year);
    }

    private OnYearSetListener listener;

    public void setListener(OnYearSetListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_year_picker);

        NumberPicker yearPicker = dialog.findViewById(R.id.picker_year);

        final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(2000);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear);

        dialog.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            if (listener != null) {
                listener.onYearSet(yearPicker.getValue());
            }
            dismiss();
        });

        dialog.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        return dialog;
    }
}
