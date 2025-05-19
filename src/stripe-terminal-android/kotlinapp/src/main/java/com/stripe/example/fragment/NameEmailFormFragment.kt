package com.stripe.example.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.stripe.example.NavigationListener
import com.stripe.example.R
import com.stripe.example.network.ApiClient
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.models.PaymentIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class NameEmailFormFragment : Fragment() {

    companion object {
        const val TAG = "NameEmailFormFragment"
        private const val ARG_PAYMENT_INTENT_ID = "payment_intent_id"

        fun newInstance(paymentIntent: PaymentIntent): NameEmailFormFragment {
            val fragment = NameEmailFormFragment()
            val args = Bundle()
            args.putString(ARG_PAYMENT_INTENT_ID, paymentIntent.id)
            fragment.arguments = args
            return fragment
        }
    }

    // UI Elements
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button

    private var paymentIntentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            paymentIntentId = it.getString(ARG_PAYMENT_INTENT_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_name_email_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements using correct IDs from layout
        nameEditText = view.findViewById(R.id.nameInput)
        emailEditText = view.findViewById(R.id.emailInput)
        submitButton = view.findViewById(R.id.submitButton)
        cancelButton = view.findViewById(R.id.skipButton)
        
        // Explicitly clear the text fields each time the fragment is created
        nameEditText.setText("")
        emailEditText.setText("")

        submitButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()

            if (validateInput(name, email) && paymentIntentId != null) {
                updatePaymentWithCustomerInfo(paymentIntentId!!, name, email)
            } else {
                Toast.makeText(requireContext(), "Error: Missing payment information.", Toast.LENGTH_LONG).show()
            }
        }

        cancelButton.setOnClickListener {
            activity?.let {
                if (it is NavigationListener) {
                    it.runOnUiThread {
                        it.onRequestExitWorkflow()
                    }
                }
            }
        }
    }

    // Input validation logic
    private fun updatePaymentWithCustomerInfo(paymentIntentId: String, name: String, email: String) {
        // Show loading indicator or disable button
        submitButton.isEnabled = false
        
        // Update payment intent with full customer information
        ApiClient.updatePaymentIntentCustomer(
            id = paymentIntentId,
            name = name,
            email = email,
            callback = object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    submitButton.isEnabled = true
                    if (response.isSuccessful) {
                        // Return to keypad fragment on success
                        activity?.let {
                            if (it is NavigationListener) {
                                it.runOnUiThread {
                                    it.onRequestExitWorkflow()
                                }
                            }
                        }
                        Toast.makeText(requireContext(), "Customer information saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                }
                
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    submitButton.isEnabled = true
                    Toast.makeText(requireContext(), "Error updating customer info: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun validateInput(name: String, email: String): Boolean {
        var isValid = true

        // Validate Name
        if (name.isEmpty()) {
            nameEditText.error = getString(R.string.error_name_required)
            isValid = false
        } else {
            nameEditText.error = null
        }

        // Validate Email
        if (email.isEmpty()) {
            emailEditText.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            emailEditText.error = null
        }

        return isValid
    }
}
