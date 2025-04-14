package com.stripe.example.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.stripe.example.NavigationListener
import com.stripe.example.R
import java.text.NumberFormat
import java.util.Locale

/**
 * Fragment for manually entering a payment amount using a keypad with additive functionality.
 */
class KeypadFragment : Fragment() {

    companion object {
        const val TAG = "com.stripe.example.fragment.KeypadFragment"
        private const val MAX_DIGITS = 10 // Arbitrary limit to prevent overflow/UI issues
        private const val CURRENCY = "usd" // Hardcode currency for now
    }

    // State
    private var currentEntryString: String = "0"
    private val addedAmounts = mutableListOf<Long>()

    // Views
    private lateinit var amountDisplay: TextView
    private lateinit var breakdownDisplay: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_keypad, container, false)

        amountDisplay = view.findViewById(R.id.amount_display)
        breakdownDisplay = view.findViewById(R.id.breakdown_display)

        // Setup number buttons (0-9)
        val numberButtonIds = mapOf(
            R.id.key_0 to 0, R.id.key_1 to 1, R.id.key_2 to 2, R.id.key_3 to 3, R.id.key_4 to 4,
            R.id.key_5 to 5, R.id.key_6 to 6, R.id.key_7 to 7, R.id.key_8 to 8, R.id.key_9 to 9
        )
        numberButtonIds.forEach { (id, digit) ->
            view.findViewById<Button>(id).setOnClickListener { onNumberPressed(digit) }
        }

        // Setup Clear button
        view.findViewById<Button>(R.id.key_clear).setOnClickListener { onClearPressed() }

        // Setup Plus button
        view.findViewById<Button>(R.id.key_plus).setOnClickListener { onPlusPressed() }

        // Setup Charge button
        view.findViewById<Button>(R.id.charge_button).setOnClickListener { onChargePressed() }

        updateDisplay()
        return view
    }

    private fun onNumberPressed(digit: Int) {
        if (currentEntryString == "0") {
            currentEntryString = digit.toString()
        } else if (currentEntryString.length < MAX_DIGITS) {
            currentEntryString += digit.toString()
        }
        updateDisplay()
    }

    private fun onClearPressed() {
        if (currentEntryString != "0") {
            // 1. Clear current entry if it's not zero
            currentEntryString = "0"
        } else {
            // 2. Current entry is zero. Check if there are added amounts.
            if (addedAmounts.isNotEmpty()) {
                // Undo the last '+' operation
                val lastAddedAmount = addedAmounts.removeLast() // Remove and get the last amount
                currentEntryString = lastAddedAmount.toString() // Set current entry to the removed amount
                if (addedAmounts.isEmpty()) {
                     breakdownDisplay.visibility = View.INVISIBLE // Hide breakdown if it was the only added amount
                }
            }
            // 3. If current entry was zero AND addedAmounts was empty, do nothing.
        }
        updateDisplay() // Update display regardless
    }

    private fun onPlusPressed() {
        val currentCents = currentEntryString.toLongOrNull() ?: 0L
        if (currentCents > 0 || addedAmounts.isNotEmpty()) {
            // Add the current entry to the list (even if it's 0, if we already have added amounts)
            addedAmounts.add(currentCents)
            // Reset the current entry
            currentEntryString = "0"
            // Make breakdown visible (explicitly)
            breakdownDisplay.visibility = View.VISIBLE
            updateDisplay()
        }
    }

    private fun onChargePressed() {
        val finalAmountCents = calculateTotalCents()
        if (finalAmountCents > 0) {
            (activity as? NavigationListener)?.onChargeKeypadAmount(finalAmountCents, CURRENCY)
        }
    }

    private fun calculateTotalCents(): Long {
        val currentCents = currentEntryString.toLongOrNull() ?: 0L
        return addedAmounts.sum() + currentCents
    }

    private fun updateDisplay() {
        val totalCents = calculateTotalCents()
        amountDisplay.text = formatCentsToString(totalCents)

        if (addedAmounts.isNotEmpty()) {
            val breakdownText = addedAmounts.joinToString(" + ") { formatCentsToString(it) } +
                    " + ${formatCentsToString(currentEntryString.toLongOrNull() ?: 0L)}"
            breakdownDisplay.text = breakdownText
            breakdownDisplay.visibility = View.VISIBLE
        } else {
            // Use INVISIBLE to keep layout space
            breakdownDisplay.visibility = View.INVISIBLE
        }
    }

    private fun formatCentsToString(cents: Long): String {
        // Consider making Locale dynamic if supporting other regions
        return NumberFormat.getCurrencyInstance(Locale.US).format(cents / 100.0)
    }
}
