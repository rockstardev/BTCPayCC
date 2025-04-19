package com.stripe.example.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.stripe.example.R

/**
 * A simple fragment shown while attempting to reconnect to a previously connected reader.
 */
class ConnectingToReaderFragment : Fragment() {

    companion object {
        const val TAG = "com.stripe.example.fragment.ConnectingToReaderFragment"
        private const val ARGS_SERIAL_NUMBER = "serial_number"

        fun newInstance(serialNumber: String): ConnectingToReaderFragment {
            val fragment = ConnectingToReaderFragment()
            val args = Bundle()
            args.putString(ARGS_SERIAL_NUMBER, serialNumber)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_connecting_reader, container, false)
        val messageView = view.findViewById<TextView>(R.id.connecting_message)
        var serialNumber = arguments?.getString(ARGS_SERIAL_NUMBER) ?: "unknown reader"

        // Truncate serial number if too long
        if (serialNumber.length > 21) {
            serialNumber = serialNumber.substring(0, 21) + "..."
        }

        messageView.text = getString(R.string.connecting_to_reader, serialNumber)

        // You might want to add a timeout mechanism here as well that calls
        // navigationListener.onCancelReconnectDiscovery() if it takes too long.

        return view
    }
}
