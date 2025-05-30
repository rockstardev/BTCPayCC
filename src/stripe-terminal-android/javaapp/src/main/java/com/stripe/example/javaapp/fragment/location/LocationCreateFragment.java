package com.stripe.example.javaapp.fragment.location;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.stripe.example.javaapp.MainActivity;
import com.stripe.example.javaapp.NavigationListener;
import com.stripe.example.javaapp.R;
import com.stripe.example.javaapp.network.ApiClient;
import com.stripe.example.javaapp.databinding.FragmentLocationCreateBinding;

public class LocationCreateFragment extends Fragment {
    final public static String TAG = "LocationCreateFragment";
    private FragmentLocationCreateBinding binding;

    public static LocationCreateFragment newInstance() {
        return new LocationCreateFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_create, container, false);
        binding = FragmentLocationCreateBinding.bind(view);
        binding.locationCreateSubmit.setOnClickListener(v -> onSubmit());
        binding.locationCreateCancelButton.setOnClickListener(v ->
                ((MainActivity) requireActivity()).onCancelCreateLocation()
        );

        return view;
    }

    private void onSubmit() {
        try {
            ApiClient.createLocation(
                    requireValue(binding.locationCreateDisplayNameInput),
                    requireValue(binding.locationCreateAddressLine1Input),
                    getValue(binding.locationCreateAddressLine2Input),
                    getValue(binding.locationCreateAddressCityInput),
                    getValue(binding.locationCreateAddressPostalInput),
                    getValue(binding.locationCreateAddressStateInput),
                    requireValue(binding.locationCreateAddressCountryInput)
            );
            ((NavigationListener) requireActivity()).onLocationCreated();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Missing required input");
        } catch (Throwable e) {
            Snackbar.make(requireView(), e.getMessage() == null ? "Unknown Error" : e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private String requireValue(EditText receiver) {
        if (receiver.getText() == null || receiver.getText().toString().trim().isEmpty()) {
            receiver.setError(getResources().getString(R.string.field_required));
            throw new IllegalStateException();
        }

        return receiver.getText().toString();
    }

    @Nullable
    private String getValue(EditText receiver) {
        if (receiver.getText() == null || receiver.getText().toString().trim().isEmpty()) {
            return null;
        }
        return receiver.getText().toString();
    }
}
