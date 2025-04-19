package com.stripe.example.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.stripe.example.NavigationListener
import com.stripe.example.R
import com.stripe.example.TerminalOfflineListener
import com.stripe.example.customviews.TerminalOnlineIndicator
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.OfflineMode
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.NetworkStatus
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * The `ConnectedReaderFragment` shows the reader connection status and allows starting
 * various workflows.
 */
@OptIn(OfflineMode::class)
class ConnectedReaderFragment : Fragment() {

    companion object {
        const val TAG = "com.stripe.example.fragment.ConnectedReaderFragment"
        private const val ARGS_IS_FROM_RECONNECT = "is_from_reconnect"

        fun newInstance(isFromReconnect: Boolean = false): ConnectedReaderFragment {
            val fragment = ConnectedReaderFragment()
            val args = Bundle()
            args.putBoolean(ARGS_IS_FROM_RECONNECT, isFromReconnect)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var clearSavedConnectionButton: View

    private var askForNameEmail: Boolean = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_connected_reader, container, false)

        // Set the description of the connected reader
        Terminal.getInstance().connectedReader?.let {
            view.findViewById<TextView>(R.id.reader_description).text = getString(
                R.string.reader_description,
                it.deviceType,
                it.serialNumber,
            )
            // TODO: Set status as well
        }

        val askSwitch = view.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.ask_name_email_switch)
        askSwitch.isChecked = false
        askSwitch.setOnCheckedChangeListener { _, isChecked ->
            askForNameEmail = isChecked
        }
        // Set up the disconnect button
        val activityRef = WeakReference(activity)
        view.findViewById<View>(R.id.disconnect_button).setOnClickListener {
            Terminal.getInstance().disconnectReader(object : Callback {

                override fun onSuccess() {
                    activityRef.get()?.let {
                        if (it is NavigationListener) {
                            it.runOnUiThread {
                                (it as? NavigationListener)?.onDisconnectReader()
                            }
                        }
                    }
                }

                override fun onFailure(e: TerminalException) {
                    Log.e("ConnectedReaderFragment", "Failed to disconnect reader", e)
                }
            })
        }

        launchAndRepeatWithViewLifecycle(Lifecycle.State.RESUMED) {
            launch {
                TerminalOfflineListener.offlineStatus
                        .collectLatest {
                            updateTerminalOnlineIndicator(it)
                        }
            }
        }

        updateTerminalOnlineIndicator(Terminal.getInstance().offlineStatus.sdk.networkStatus)

        // Set up the collect payment button
        view.findViewById<View>(R.id.collect_card_payment_button).setOnClickListener {
            (activity as? NavigationListener)?.onSelectPaymentWorkflow()
        }

        // Set up the keycard payment button
        view.findViewById<View>(R.id.keypad_payment_button).setOnClickListener {
            (activity as? NavigationListener)?.onSelectKeypadPaymentWorkflow()
        }

        // Set up the save card button
        view.findViewById<View>(R.id.save_card_button).setOnClickListener {
            (activity as? NavigationListener)?.onRequestSaveCard()
        }

        // Set up the update reader button
        view.findViewById<View>(R.id.update_reader_button).setOnClickListener {
            (activity as? NavigationListener)?.onSelectUpdateWorkflow()
        }

        // Set up the view offline logs button
        view.findViewById<View>(R.id.view_offline_logs_button).setOnClickListener {
            (activity as? NavigationListener)?.onSelectViewOfflineLogs()
        }

        clearSavedConnectionButton = view.findViewById(R.id.clear_saved_connection_button)
        clearSavedConnectionButton.setOnClickListener {
            // Call the listener method (to be added to NavigationListener)
            (activity as? NavigationListener)?.onRequestClearSavedConnection()
        }

        // Update button visibility based on saved state
        updateClearButtonVisibility()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we should trigger the collect payment flow
        val isFromReconnect = arguments?.getBoolean(ARGS_IS_FROM_RECONNECT, false) == true
        if (isFromReconnect) {
            Log.d(TAG, "Navigated from reconnect, triggering keypad payment view right away.")
            view.findViewById<View>(R.id.keypad_payment_button)?.performClick()
        }
    }

    private fun updateTerminalOnlineIndicator(networkStatus: NetworkStatus) {
        view?.findViewById<TerminalOnlineIndicator>(R.id.online_indicator).run {
            this?.networkStatus = networkStatus
        }
    }

    // Public function called by MainActivity when prefs are cleared
    fun updateClearButtonVisibility() {
        // Check if saved details exist via the listener (to be added to NavigationListener)
        val hasSavedDetails = (activity as? NavigationListener)?.hasSavedConnectionDetails() ?: false
        clearSavedConnectionButton.visibility = if (hasSavedDetails) View.VISIBLE else View.GONE
    }
}
