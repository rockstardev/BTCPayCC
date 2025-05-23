package com.stripe.example.javaapp.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.stripe.example.javaapp.NavigationListener;
import com.stripe.example.javaapp.R;
import com.stripe.example.javaapp.model.OfflineBehaviorSelection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The `PaymentFragment` allows the user to create a custom payment and ask the reader to handle it.
 */
public class PaymentFragment extends Fragment {

    @NotNull public static final String TAG = "com.stripe.example.fragment.PaymentFragment";

    private OfflineBehaviorSelection selectedBehavior = OfflineBehaviorSelection.DEFAULT;

    @Nullable
    @Override
    public View onCreateView(
            @NotNull LayoutInflater layoutInflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        final View view = layoutInflater.inflate(R.layout.fragment_payment, container, false);

        ((TextView) view.findViewById(R.id.amount_edit_text)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                final String editableString = editable.toString();
                if (!editableString.isEmpty()) {
                    ((TextView) view.findViewById(R.id.charge_amount))
                            .setText(formatCentsToString(Integer.parseInt(editable.toString())));
                }
            }
        });

        view.findViewById(R.id.collect_payment_button).setOnClickListener(v -> {
            final boolean skipTipping = ((SwitchMaterial) view.findViewById(R.id.skip_tipping_switch)).isChecked();
            final boolean extendedAuth = ((SwitchMaterial) view.findViewById(R.id.extended_auth_switch)).isChecked();
            final boolean incrementalAuth = ((SwitchMaterial) view.findViewById(R.id.incremental_auth_switch)).isChecked();
            final FragmentActivity activity = getActivity();
            if (activity instanceof NavigationListener) {
                final String amount = ((TextView) view.findViewById(R.id.amount_edit_text))
                        .getText().toString();
                final String currency = ((TextView) view.findViewById(R.id.currency_edit_text))
                        .getText().toString();
                ((NavigationListener) activity).onRequestPayment(Long.parseLong(amount), currency, skipTipping,
                        extendedAuth, incrementalAuth, selectedBehavior);
            }
        });

        view.findViewById(R.id.home_button).setOnClickListener(v -> {
            final FragmentActivity activity = getActivity();
            if (activity instanceof NavigationListener) {
                ((NavigationListener) activity).onRequestExitWorkflow();
            }
        });

        Spinner spinner = view.findViewById(R.id.offline_behavior_spinner);
        List<String> offlineBehaviorOptions = Arrays.stream(OfflineBehaviorSelection.values()).map(
                offlineBehaviorSelection -> getResources().getString(offlineBehaviorSelection.labelResource)
        ).collect(Collectors.toList());
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                offlineBehaviorOptions
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedBehavior = OfflineBehaviorSelection.values()[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        return view;
    }

    private String formatCentsToString(int cents) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0);
    }
}
