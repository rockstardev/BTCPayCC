package com.stripe.example.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stripe.example.NavigationListener
import com.stripe.example.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment for manually entering a payment amount using a keypad.
 */
class KeypadFragment : Fragment() {

    companion object {
        const val TAG = "com.stripe.example.fragment.KeypadFragment"
        private const val MAX_DIGITS = 10 // Arbitrary limit to prevent overflow/UI issues
        private const val CURRENCY = "usd" // Hardcode currency for now
    }

    private var currentAmountString: String = "0"
    private lateinit var amountDisplay: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_keypad, container, false)

        amountDisplay = view.findViewById(R.id.amount_display)
        updateDisplay()

        // Setup number buttons (0-9)
        val numberButtonIds = listOf(
            R.id.key_0, R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4,
            R.id.key_5, R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9
        )
        numberButtonIds.forEachIndexed { index, id ->
            view.findViewById<Button>(id).setOnClickListener { onNumberPressed(index) }
        }

        // Setup Clear button
        view.findViewById<Button>(R.id.key_clear).setOnClickListener { onClearPressed() }

        // Setup Charge button
        view.findViewById<Button>(R.id.charge_button).setOnClickListener { onChargePressed() }

        // Setup Plus button (currently does nothing)
        // view.findViewById<Button>(R.id.key_plus).setOnClickListener { /* TODO: Implement if needed */ }

        return view
    }

    private fun onNumberPressed(digit: Int) {
        if (currentAmountString == "0") {
            currentAmountString = digit.toString()
        } else if (currentAmountString.length < MAX_DIGITS) {
            currentAmountString += digit.toString()
        }
        updateDisplay()
    }

    private fun onClearPressed() {
        currentAmountString = "0"
        updateDisplay()
    }

    private fun onChargePressed() {
        val amountCents = currentAmountString.toLongOrNull() ?: 0L
        if (amountCents > 0) {
            // TODO: Define and call onChargeKeypadAmount in NavigationListener/MainActivity
            (activity as? NavigationListener)?.onChargeKeypadAmount(amountCents, CURRENCY)
        }
    }

    private fun updateDisplay() {
        val amountCents = currentAmountString.toLongOrNull() ?: 0L
        amountDisplay.text = formatCentsToString(amountCents)
    }

    private fun formatCentsToString(cents: Long): String {
        // TODO: Get locale dynamically if needed
        return NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)
    }
}
