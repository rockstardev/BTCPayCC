package com.stripe.example

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.stripe.example.fragment.ConnectedReaderFragment
import com.stripe.example.fragment.ConnectingToReaderFragment
import com.stripe.example.fragment.KeypadFragment
import com.stripe.example.fragment.NameEmailFormFragment
import com.stripe.example.fragment.PaymentFragment
import com.stripe.example.fragment.TerminalFragment
import com.stripe.example.fragment.UpdateReaderFragment
import com.stripe.example.fragment.discovery.DiscoveryFragment
import com.stripe.example.fragment.discovery.DiscoveryMethod
import com.stripe.example.fragment.event.EventFragment
import com.stripe.example.fragment.location.LocationCreateFragment
import com.stripe.example.fragment.location.LocationSelectionController
import com.stripe.example.fragment.location.LocationSelectionFragment
import com.stripe.example.fragment.offline.OfflinePaymentsLogFragment
import com.stripe.example.model.OfflineBehaviorSelection
import com.stripe.example.network.TokenProvider
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.OfflineMode
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.InternetReaderListener
import com.stripe.stripeterminal.external.callable.MobileReaderListener
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.DeviceType
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.Location
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel

class MainActivity :
    AppCompatActivity(),
    NavigationListener,
    MobileReaderListener,
    TapToPayReaderListener,
    InternetReaderListener,
    LocationSelectionController {

    private lateinit var readerConnectionPersistence: ReaderConnectionPersistence

    private var reconnectDiscoveryCancelable: Cancelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        readerConnectionPersistence = ReaderConnectionPersistence(applicationContext)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            BluetoothAdapter.getDefaultAdapter()?.let { adapter ->
                if (!adapter.isEnabled) {
                    adapter.enable()
                }
            }
        } else {
            Log.w(MainActivity::class.java.simpleName, "Failed to acquire Bluetooth permission")
        }

        initialize()
    }

    // Navigation callbacks

    /**
     * Callback function called when discovery has been canceled by the [DiscoveryFragment]
     */
    override fun onCancelDiscovery() {
        navigateTo(TerminalFragment.TAG, TerminalFragment())
    }

    override fun onRequestLocationSelection() {
        navigateTo(
            LocationSelectionFragment.TAG,
            LocationSelectionFragment.newInstance(),
            replace = false,
            addToBackStack = true,
        )
    }

    /**
     * Callback function called to exit the change location flow
     */
    override fun onCancelLocationSelection() {
        supportFragmentManager.popBackStackImmediate()
    }

    override fun onRequestCreateLocation() {
        navigateTo(
            LocationCreateFragment.TAG,
            LocationCreateFragment.newInstance(),
            replace = false,
            addToBackStack = true,
        )
    }

    /**
     * Callback function called to exit the create location flow
     */
    override fun onCancelCreateLocation() {
        supportFragmentManager.popBackStackImmediate()
    }

    override fun onLocationCreated() {
        supportFragmentManager.popBackStackImmediate()
    }

    /**
     * Callback function called once discovery has been selected by the [TerminalFragment]
     */
    override fun onRequestDiscovery(isSimulated: Boolean, discoveryMethod: DiscoveryMethod) {
        navigateTo(DiscoveryFragment.TAG, DiscoveryFragment.newInstance(isSimulated, discoveryMethod))
    }

    /**
     * Callback function called to exit the payment workflow
     */
    override fun onRequestExitWorkflow() {
        // Check if we need to return to KeypadFragment
        // Find if KeypadFragment exists in the stack
        val keypadFragment = supportFragmentManager.findFragmentByTag(KeypadFragment.TAG)
        
        if (keypadFragment != null) {
            // We came from KeypadFragment, so return there
            navigateTo(KeypadFragment.TAG, KeypadFragment())
        } else if (Terminal.getInstance().connectionStatus == ConnectionStatus.CONNECTED) {
            // Default case when connected
            navigateTo(ConnectedReaderFragment.TAG, ConnectedReaderFragment())
        } else {
            // Default case when not connected
            navigateTo(TerminalFragment.TAG, TerminalFragment())
        }
    }

    /**
     * Callback function called to start a payment by the [PaymentFragment]
     */
    override fun onRequestPayment(
        amount: Long,
        currency: String,
        skipTipping: Boolean,
        extendedAuth: Boolean,
        incrementalAuth: Boolean,
        offlineBehaviorSelection: OfflineBehaviorSelection
    ) {
        // Create a unique tag for each payment session to prevent fragment reuse
        val uniqueTag = "${EventFragment.TAG}_${System.currentTimeMillis()}"
        
        // Force creating a new fragment instance by using a unique tag
        navigateTo(
            uniqueTag,
            EventFragment.requestPayment(
                amount,
                currency,
                skipTipping,
                extendedAuth,
                incrementalAuth,
                offlineBehaviorSelection,
                readerConnectionPersistence.loadAskForNameEmail()
            ),
            replace = true,  // Replace any existing content
            addToBackStack = false  // Don't add to back stack to keep navigation clean
        )
    }

    /**
     * Callback function called once the payment workflow has been selected by the
     * [ConnectedReaderFragment]
     */
    override fun onSelectPaymentWorkflow() {
        navigateTo(PaymentFragment.TAG, PaymentFragment())
    }

    /**
     * Callback function called once the read card workflow has been selected by the
     * [ConnectedReaderFragment]
     */
    override fun onRequestSaveCard() {
        navigateTo(EventFragment.TAG, EventFragment.collectSetupIntentPaymentMethod())
    }

    /**
     * Callback function called once the update reader workflow has been selected by the
     * [ConnectedReaderFragment]
     */
    override fun onSelectUpdateWorkflow() {
        navigateTo(UpdateReaderFragment.TAG, UpdateReaderFragment())
    }

    /**
     * Callback function called once the view offline logs has been selected by the
     * [ConnectedReaderFragment]
     */
    override fun onSelectViewOfflineLogs() {
        navigateTo(OfflinePaymentsLogFragment.TAG, OfflinePaymentsLogFragment())
    }

    override fun onSelectKeypadPaymentWorkflow() {
        navigateTo(KeypadFragment.TAG, KeypadFragment(), addToBackStack = true)
    }

    override fun onCancelKeypadEntry() {
        supportFragmentManager.popBackStack()
    }

    override fun onRequestClearSavedConnection() {
        Log.d("MainActivity", "Clearing saved reader connection details.")
        readerConnectionPersistence.clearReaderConnectionDetails()
        // Optionally, force update the fragment's button visibility if it doesn't auto-update
        supportFragmentManager.findFragmentByTag(ConnectedReaderFragment.TAG)?.let {
            if (it is ConnectedReaderFragment && it.isVisible) {
                it.updateClearButtonVisibility()
            }
        }
        // After clearing, maybe navigate back to discovery? Or let the user disconnect manually.
        // For now, just clear and let the ConnectedReaderFragment update.
    }

    override fun hasSavedConnectionDetails(): Boolean {
        return readerConnectionPersistence.hasSavedConnectionDetails()
    }

    override fun navigateToNameEmailForm(paymentIntent: PaymentIntent?) {
        // Only navigate if we have a valid PaymentIntent
        // If null, it means the flow ended without a successful payment (e.g., cancelled, failed)
        // In that case, we might not want to show the name/email form.
        // Adjust this logic if you *do* want to show the form even on failure/cancel.
        paymentIntent?.let {
            navigateTo(
                NameEmailFormFragment.TAG,
                NameEmailFormFragment.newInstance(it),
                addToBackStack = true
            )
        }
    }

    // Terminal event callbacks

    /**
     * Callback function called when collect payment method has been canceled
     */
    override fun onCancelCollectPaymentMethod() {
        navigateTo(ConnectedReaderFragment.TAG, ConnectedReaderFragment())
    }

    /**
     * Callback function called when collect setup intent has been canceled
     */
    override fun onCancelCollectSetupIntent() {
        navigateTo(ConnectedReaderFragment.TAG, ConnectedReaderFragment())
    }

    /**
     * Callback function called on completion of [Terminal.connectReader]
     */
    override fun onConnectReader() {
        Log.d("MainActivity", "onConnectReader callback triggered.")
        val reader = Terminal.getInstance().connectedReader
        // Ensure reader and required properties are non-null before accessing them
        if (reader != null && reader.location != null && reader.serialNumber != null) {
            val currentLocationId = reader.location!!.id.toString()
            val currentSerialNumber = reader.serialNumber!!

            // Determine type based on reader properties
            val connectionType = when {
                // Check if it's a known Tap to Pay / Local Mobile reader type
                reader.deviceType == DeviceType.TAP_TO_PAY_DEVICE -> {
                    Log.d("MainActivity", "Reader identified as TAP_TO_PAY.")
                    ReaderConnectionPersistence.ConnectionType.TAP_TO_PAY
                }
                // Check network status for Internet type
                reader.networkStatus == Reader.NetworkStatus.ONLINE -> {
                    Log.d("MainActivity", "Reader identified as INTERNET.")
                    ReaderConnectionPersistence.ConnectionType.INTERNET
                }
                // Default to Bluetooth if not TapToPay or Internet
                else -> {
                    Log.d("MainActivity", "Reader identified as BLUETOOTH.")
                    ReaderConnectionPersistence.ConnectionType.BLUETOOTH
                }
            }

            // Save details *inside* the null check
            readerConnectionPersistence.saveReaderConnectionDetails(
                currentLocationId, // Use non-null vars
                currentSerialNumber,
                connectionType // Pass the determined type
            )
            Log.d("MainActivity", "Saved connection details: Type=$connectionType, Serial=$currentSerialNumber, Location=$currentLocationId")
        } else {
            Log.w("MainActivity", "onConnectReader called but reader or essential details are null.")
        }
        // Navigate to the standard connected reader screen
        navigateTo(ConnectedReaderFragment.TAG, ConnectedReaderFragment())
    }

    override fun onDisconnectReader() {
        Log.w("MainActivity", "onDisconnectReader called! Navigating back to TerminalFragment.")
        navigateTo(TerminalFragment.TAG, TerminalFragment())
    }

    override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: Cancelable?) {
        runOnUiThread {
            // Delegate out to the current fragment, if it acts as a MobileReaderListener
            supportFragmentManager.fragments.last()?.let {
                if (it is MobileReaderListener) {
                    it.onStartInstallingUpdate(update, cancelable)
                }
            }
        }
    }

    override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
        runOnUiThread {
            // Delegate out to the current fragment, if it acts as a MobileReaderListener
            supportFragmentManager.fragments.last()?.let {
                if (it is MobileReaderListener) {
                    it.onReportReaderSoftwareUpdateProgress(progress)
                }
            }
        }
    }

    override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
        runOnUiThread {
            // Delegate out to the current fragment, if it acts as a MobileReaderListener
            supportFragmentManager.fragments.last()?.let {
                if (it is MobileReaderListener) {
                    it.onFinishInstallingUpdate(update, e)
                }
            }
        }
    }

    override fun onRequestReaderInput(options: ReaderInputOptions) {
        runOnUiThread {
            // Delegate out to the current fragment, if it acts as a MobileReaderListener
            supportFragmentManager.fragments.last()?.let {
                if (it is MobileReaderListener) {
                    it.onRequestReaderInput(options)
                }
            }
        }
    }

    override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
        runOnUiThread {
            // Delegate out to the current fragment, if it acts as a MobileReaderListener
            supportFragmentManager.fragments.last()?.let {
                if (it is MobileReaderListener) {
                    it.onRequestReaderDisplayMessage(message)
                }
            }
        }
    }

    override fun onLocationSelected(location: Location) {
        supportFragmentManager.popBackStackImmediate()
        (supportFragmentManager.fragments.last() as? LocationSelectionController)?.onLocationSelected(location)
    }

    override fun onLocationCleared() {
        supportFragmentManager.popBackStackImmediate()
        (supportFragmentManager.fragments.last() as? LocationSelectionController)?.onLocationCleared()
    }

    override fun onReaderReconnectStarted(reader: Reader, cancelReconnect: Cancelable, reason: DisconnectReason) {
        Log.d("MainActivity", "Reconnection to reader ${reader.id} started!")
    }

    override fun onReaderReconnectSucceeded(reader: Reader) {
        Log.d("MainActivity", "Reader ${reader.id} reconnected successfully")
    }

    override fun onReaderReconnectFailed(reader: Reader) {
        Log.d("MainActivity", "Reconnection to reader ${reader.id} failed!")
    }

    override fun onDisconnect(reason: DisconnectReason) {
        if (reason == DisconnectReason.UNKNOWN) {
            Log.i("UnexpectedDisconnect", "disconnect reason: $reason")
        }
    }

    /**
     * Initialize the [Terminal] and go to the [TerminalFragment]
     */
    @OptIn(OfflineMode::class)
    private fun initialize(attemptReconnect: Boolean = true) {
        // Initialize the Terminal as soon as possible
        try {
            if (!Terminal.isInitialized()) {
                Terminal.initTerminal(
                    applicationContext,
                    LogLevel.VERBOSE,
                    TokenProvider(),
                    TerminalEventListener,
                    TerminalOfflineListener
                )
            }
        } catch (e: TerminalException) {
            throw RuntimeException(e)
        }

        // Check if all details are present and we should attempt reconnect
        if (attemptReconnect && readerConnectionPersistence.hasSavedConnectionDetails()) {
            Log.i("MainActivity", "Saved reader details found. Attempting reconnect via ReaderConnectionPersistence.")

            // Common failure handler - now expects ReconnectException
            val failureCallback = { e: ReaderConnectionPersistence.ReconnectException ->
                Log.e("MainActivity", "Reconnect process failed: ${e.message}", e.cause) // Log message and original cause
                readerConnectionPersistence.clearReaderConnectionDetails() // Clear invalid details
                reconnectDiscoveryCancelable = null // Clear cancelable on failure
                runOnUiThread {
                    // Only navigate if we are not already connected or connecting
                    if (Terminal.getInstance().connectionStatus == ConnectionStatus.NOT_CONNECTED) {
                        navigateTo(TerminalFragment.TAG, TerminalFragment())
                    }
                }
            }

            // Call the encapsulated reconnect logic
            reconnectDiscoveryCancelable = readerConnectionPersistence.attemptReconnect(
                this,
                Terminal.getInstance(),
                failureCallback,
                20
            )

            // If attemptReconnect returned a cancelable, it means an attempt is in progress
            if (reconnectDiscoveryCancelable != null) {
                runOnUiThread { // Ensure fragment navigation is on the main thread
                    // Retrieve the actual serial number for the UI if available
                    val serialToShow = readerConnectionPersistence.getLastReaderSerialNumber() ?: "Connecting..."
                    navigateTo(ConnectingToReaderFragment.TAG, ConnectingToReaderFragment.newInstance(serialToShow))
                }
            } else {
                // If null, prerequisites weren't met (logged in attemptReconnect), go to terminal fragment
                Log.d("MainActivity", "Reconnect prerequisites failed or not applicable. Navigating to TerminalFragment.")
                navigateTo(TerminalFragment.TAG, TerminalFragment())
            }

        } else {
            Log.d("MainActivity", "No saved reader or not attempting reconnect");
            navigateTo(TerminalFragment.TAG, TerminalFragment())
        }
    }

    /**
     * Navigate to the given fragment.
     *
     * @param fragment Fragment to navigate to.
     */
    public fun navigateTo(
        tag: String,
        fragment: Fragment,
        replace: Boolean = true,
        addToBackStack: Boolean = false,
    ) {
        val frag = supportFragmentManager.findFragmentByTag(tag) ?: fragment
        supportFragmentManager
            .beginTransaction()
            .apply {
                if (replace) {
                    replace(R.id.container, frag, tag)
                } else {
                    add(R.id.container, frag, tag)
                }

                if (addToBackStack) {
                    addToBackStack(tag)
                }
            }
            .commitAllowingStateLoss()
    }
}
