package com.stripe.example

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.example.NavigationListener
import com.stripe.example.fragment.discovery.DiscoveryMethod
import com.stripe.example.fragment.discovery.DiscoveryMethod.BLUETOOTH_SCAN
import com.stripe.example.fragment.discovery.DiscoveryMethod.INTERNET
import com.stripe.example.fragment.discovery.DiscoveryMethod.TAP_TO_PAY
import com.stripe.example.fragment.discovery.DiscoveryMethod.USB
import com.stripe.example.fragment.discovery.ReaderClickListener
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.Location
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.launch
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class DiscoverReadersActivity :
    AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var cancelDiscoveryButton: Button
    private var discoverCancelable: Cancelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover_readers)

        // Initialize views
        statusTextView = findViewById(R.id.statusTextView)
        cancelDiscoveryButton = findViewById(R.id.cancelDiscoveryButton)

        cancelDiscoveryButton.setOnClickListener {
            discoverCancelable?.cancel(object : Callback {
                override fun onSuccess() {
                    statusTextView.text = "Discovery cancelled."
                }

                override fun onFailure(e: TerminalException) {
                    statusTextView.text = "Error cancelling discovery: ${e.errorMessage}"
                }
            })
        }

        // onDiscoverReaders()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun onDiscoverReaders() {
        val isApplicationDebuggable = 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
        val config =
            DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSimulated = isApplicationDebuggable)

        // Save this cancelable to an instance variable
        discoverCancelable = Terminal.getInstance().discoverReaders(
            config,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    // Automatically connect to supported mobile readers
                }
            },
            object : Callback {
                override fun onSuccess() {
                    // Placeholder for handling successful operation
                }

                override fun onFailure(e: TerminalException) {
                    // Placeholder for handling exception
                }
            }
        )
    }

    override fun onStop() {
        super.onStop()

        // If you're leaving the activity or fragment without selecting a reader,
        // make sure you cancel the discovery process or the SDK will be stuck in
        // a discover readers phase
        discoverCancelable?.cancel(object : Callback {
            override fun onSuccess() {
                // Handle successful cancellation
            }

            override fun onFailure(e: TerminalException) {
                // Handle failure in cancellation
            }
        })
    }
}