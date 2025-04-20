package com.stripe.example

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.edit
import com.stripe.example.fragment.ConnectedReaderFragment
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException

/**
 * Handles saving and retrieving reader connection details using SharedPreferences.
 */
class ReaderConnectionPersistence(context: Context) {

    enum class ConnectionType {
        BLUETOOTH,
        TAP_TO_PAY,
        INTERNET
    }

    companion object {
        private const val SHARED_PREFS_NAME = "StripeTerminalExamplePrefs"
        private const val KEY_LOCATION_ID = "reader_connection_location_id"
        private const val KEY_READER_SERIAL = "reader_connection_serial"
        private const val KEY_CONNECTION_TYPE = "reader_connection_type"
        private const val KEY_IS_SIMULATED = "reader_connection_is_simulated"
        private const val ASK_FOR_NAME_EMAIL = "ask_for_name_email"
    }

    private val sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAskForNameEmail(ask: Boolean) {
        sharedPreferences.edit() {
            putBoolean(ASK_FOR_NAME_EMAIL, ask)
        }
    }

    fun loadAskForNameEmail() : Boolean {
        return sharedPreferences.getBoolean(ASK_FOR_NAME_EMAIL, false)
    }

    fun saveReaderConnectionDetails(
        locationId: String,
        serialNumber: String,
        connectionType: ConnectionType
    ) {
        sharedPreferences.edit() {
            putString(KEY_LOCATION_ID, locationId)
                .putString(KEY_READER_SERIAL, serialNumber)
                .putString(KEY_CONNECTION_TYPE, connectionType.name)
        }
    }

    fun saveDiscoverySimulationStatus(isSimulated: Boolean) {
        sharedPreferences.edit() {
            putBoolean(KEY_IS_SIMULATED, isSimulated)
        }
        Log.d("ReaderConnectionPersistence", "Saved discovery simulation status: $isSimulated")
    }

    fun getLastReaderLocationId(): String? {
        return sharedPreferences.getString(KEY_LOCATION_ID, null)
    }

    fun getLastReaderSerialNumber(): String? {
        return sharedPreferences.getString(KEY_READER_SERIAL, null)
    }

    fun getLastReaderConnectionType(): ConnectionType? {
        val typeName = sharedPreferences.getString(KEY_CONNECTION_TYPE, null)
        return try {
            typeName?.let { ConnectionType.valueOf(it) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun clearReaderConnectionDetails() {
        sharedPreferences.edit() {
            remove(KEY_LOCATION_ID)
                .remove(KEY_READER_SERIAL)
                .remove(KEY_CONNECTION_TYPE)
                .remove(KEY_IS_SIMULATED)
        }
    }

    fun hasSavedConnectionDetails(): Boolean {
        return getLastReaderLocationId() != null &&
               getLastReaderSerialNumber() != null &&
               getLastReaderConnectionType() != null
    }

    /**
     * Attempts to reconnect to the last used reader by discovering it and then connecting.
     * @param activity MainActivity instance, used as the listener for connection events.
     * @param terminal Terminal instance.
     * @param failureCallback Lambda to execute if reconnection fails at any step.
     * @return A Cancelable for the discovery process, or null if reconnect prerequisites are not met.
     */
    class ReconnectException(message: String, cause: Throwable? = null) : Exception(message, cause)

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun attemptReconnect(
        activity: MainActivity,
        terminal: Terminal,
        failureCallback: (ReconnectException) -> Unit,
        timeoutSeconds: Int
    ): Cancelable? {
        val lastLocationId = sharedPreferences.getString(KEY_LOCATION_ID, null)
        val lastReaderSerial = sharedPreferences.getString(KEY_READER_SERIAL, null)
        val lastConnectionTypeString = sharedPreferences.getString(KEY_CONNECTION_TYPE, null)
        val lastIsSimulated = sharedPreferences.getBoolean(KEY_IS_SIMULATED, false)

        // Validate all details are present
        if (lastLocationId == null || lastReaderSerial == null || lastConnectionTypeString == null) {
            Log.e("ReaderConnectionPersistence", "Attempted reconnect with incomplete saved details.")
            failureCallback(ReconnectException("Incomplete saved reader details for reconnect."))
            return null
        }

        val lastConnectionType = try {
            ConnectionType.valueOf(lastConnectionTypeString)
        } catch (e: IllegalArgumentException) {
            Log.e("ReaderConnectionPersistence", "Invalid saved connection type: $lastConnectionTypeString")
            failureCallback(ReconnectException("Invalid saved connection type"))
            return null
        }

        Log.i("ReaderConnectionPersistence", "Attempting reconnect for $lastReaderSerial using discovery to find Reader object.")

        // Determine Discovery Configuration based on connection type
        val discoveryConfig: DiscoveryConfiguration = when (lastConnectionType) {
            ConnectionType.BLUETOOTH -> DiscoveryConfiguration.BluetoothDiscoveryConfiguration(timeoutSeconds, lastIsSimulated)
            ConnectionType.TAP_TO_PAY -> DiscoveryConfiguration.TapToPayDiscoveryConfiguration(lastIsSimulated)
            ConnectionType.INTERNET -> DiscoveryConfiguration.InternetDiscoveryConfiguration(timeoutSeconds, lastLocationId, lastIsSimulated)
            // No 'else' needed because we checked lastConnectionType is not null earlier
        }

        Log.d("ReaderConnectionPersistence", "Using DiscoveryConfiguration: ${discoveryConfig::class.java.simpleName}")

        // Use a mutable variable within the scope to hold the cancelable
        // This is needed because the activity holds the reference to cancel externally
        var discoveryCancelable: Cancelable? = null

        val discoveryListener = object : DiscoveryListener {
            var connectAttempted = false

            override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                if (connectAttempted || discoveryCancelable == null) {
                    // Already connecting or discovery was cancelled externally
                    // Check discoveryCancelable null status to ensure we don't proceed if externally cancelled
                    return
                }

                val targetReader = readers.firstOrNull { it.serialNumber == lastReaderSerial }

                if (targetReader != null) {
                    Log.i("ReaderConnectionPersistence", "Reconnect Discovery: Found target reader $lastReaderSerial.")
                    connectAttempted = true

                    // Cancel discovery immediately
                    discoveryCancelable?.cancel(object : Callback {
                        override fun onSuccess() {
                            Log.d("ReaderConnectionPersistence", "Discovery cancelled successfully before connecting.")
                            // Proceed with connection now
                            connectDiscoveredReader(
                                activity,
                                terminal,
                                targetReader,
                                lastConnectionType,
                                lastLocationId,
                                failureCallback
                            )
                        }

                        override fun onFailure(e: TerminalException) {
                            Log.w("ReaderConnectionPersistence", "Failed to cancel discovery: ${e.errorMessage}. Attempting connection anyway.")
                            // Still attempt connection even if cancel fails
                            connectDiscoveredReader(
                                activity,
                                terminal,
                                targetReader,
                                lastConnectionType,
                                lastLocationId,
                                failureCallback
                            )
                        }
                    })
                    // Connection happens inside the cancel callback now
                } else {
                    Log.d("ReaderConnectionPersistence", "Reconnect Discovery: Target reader $lastReaderSerial not found yet.")
                }
            }
        }

        Log.i("ReaderConnectionPersistence", "Starting discovery to find $lastReaderSerial...")
        // Start discovery and assign the cancelable to the local variable
        discoveryCancelable = terminal.discoverReaders(
            discoveryConfig,
            discoveryListener,
            object : Callback { // Callback for discovery initiation
                override fun onSuccess() {
                    Log.i("ReaderConnectionPersistence", "Reconnect discovery process initiated successfully.")
                    // Timeout logic could potentially be started here if desired
                }

                override fun onFailure(e: TerminalException) {
                    Log.e("ReaderConnectionPersistence", "Failed to START reconnect discovery: ${e.errorMessage}")
                    // Ensure that it is not cancel by user, because that's what we call as well at line 161
                    //if (e.errorCode != TerminalErrorCode.USER_ERROR_CANCEL) {
                    //    failureCallback(ReconnectException("Failed to start reader discovery", e))
                    //}
                }
            }
        )

        // Return the cancelable so MainActivity can manage it
        return discoveryCancelable
    }

    private fun connectDiscoveredReader(
        activity: MainActivity,
        terminal: Terminal,
        reader: Reader,
        connectionType: ConnectionType,
        locationId: String,
        failureCallback: (ReconnectException) -> Unit
    ) {
        val connectionConfig = when (connectionType) {
            ConnectionType.BLUETOOTH -> ConnectionConfiguration.BluetoothConnectionConfiguration(locationId, true, activity)
            ConnectionType.TAP_TO_PAY -> ConnectionConfiguration.TapToPayConnectionConfiguration(locationId, true, activity)
            ConnectionType.INTERNET -> ConnectionConfiguration.InternetConnectionConfiguration(true, activity)
            // No else needed due to prior checks
        }

        Log.i("ReaderConnectionPersistence", "Calling connectReader for discovered reader ${reader.serialNumber}")

        terminal.connectReader(
            reader,
            connectionConfig,
            object : ReaderCallback { // Callback for this specific connection attempt
                override fun onSuccess(connectedReader: Reader) {
                    Log.i("ReaderConnectionPersistence", "connectReader (for reconnect) successful for ${connectedReader.serialNumber}.")
                    activity.navigateTo(
                        ConnectedReaderFragment.TAG,
                        ConnectedReaderFragment.newInstance(isFromReconnect = true)
                    )
                    // MainActivity's listeners (passed as 'listener') will handle UI updates
                }

                override fun onFailure(e: TerminalException) {
                    Log.e("ReaderConnectionPersistence", "connectReader (for reconnect) failed: ${e.errorMessage}")
                    // Clear potentially stale details if connection fails
                    clearReaderConnectionDetails()
                    failureCallback(ReconnectException("Failed to connect to reader ${reader.serialNumber}", e))
                }
            }
        )
    }
}
