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
import com.stripe.example.R
import com.stripe.stripeterminal.external.models.PaymentIntent

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

        submitButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()

            if (validateInput(name, email) && paymentIntentId != null) {
                // TODO: Submit email and name to Stripe SDK to update paymentIntent with customer data
            } else {
                    Toast.makeText(requireContext(), "Error: Missing payment information.", Toast.LENGTH_LONG).show()

            }
        }

        cancelButton.setOnClickListener {
            // TODO: Go back to previous fragment
        }
    }

    // Input validation logic
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
