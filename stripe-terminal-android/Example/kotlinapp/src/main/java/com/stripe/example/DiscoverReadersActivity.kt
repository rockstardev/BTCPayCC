package com.stripe.example

import android.Manifest
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.*
import com.stripe.stripeterminal.external.models.*
import com.stripe.stripeterminal.external.models.ConnectionConfiguration.TapToPayConnectionConfiguration

class DiscoverReadersActivity : AppCompatActivity(), TapToPayReaderListener {

    private lateinit var readerStatusTextView: TextView
    private lateinit var connectReaderButton: Button
    private lateinit var paymentLayout: LinearLayout
    private lateinit var amountInput: EditText
    private lateinit var chargeButton: Button
    private lateinit var locationStatusTextView: TextView
    private lateinit var testModeCheckBox: CheckBox

    private var discoverCancelable: Cancelable? = null
    private var connectedReader: Reader? = null
    private var currentPaymentIntent: PaymentIntent? = null
    private var selectedLocationId: String? = null // fallback if reader has no location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover_readers)

        // UI bindings
        readerStatusTextView = findViewById(R.id.readerStatusTextView)
        connectReaderButton = findViewById(R.id.connectReaderButton)
        paymentLayout = findViewById(R.id.paymentLayout)
        amountInput = findViewById(R.id.amountInput)
        chargeButton = findViewById(R.id.chargeButton)
        locationStatusTextView = findViewById(R.id.locationStatusTextView)
        testModeCheckBox = findViewById(R.id.testModeCheckBox)

        connectReaderButton.setOnClickListener {
            val isSim = testModeCheckBox.isChecked;
            val config = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(isSim)
            val test = Terminal.getInstance().supportsReadersOfType(DeviceType.TAP_TO_PAY_DEVICE, config)

            Toast.makeText(this, test.isSupported.toString(), Toast.LENGTH_SHORT).show()
            if (test.isSupported) {
                connectToTapToPayReader(isSim)
            }
        }

        chargeButton.setOnClickListener {
            val amount = amountInput.text.toString().toDoubleOrNull()
            if (amount != null) {
                createAndCollectPayment(amount)
            } else {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        updateReaderStatus()
        updateLocationStatus()
    }

    private fun updateReaderStatus() {
        val reader = Terminal.getInstance().connectedReader
        if (reader != null) {
            connectedReader = reader
            readerStatusTextView.text = "Reader connected: ${reader.serialNumber}"
            paymentLayout.visibility = View.VISIBLE
            connectReaderButton.visibility = View.GONE
        } else {
            readerStatusTextView.text = "No reader connected"
            paymentLayout.visibility = View.GONE
            connectReaderButton.visibility = View.VISIBLE
        }
    }

    private fun updateLocationStatus() {
        Terminal.getInstance().listLocations(
            ListLocationsParameters.Builder().build(),
            object : LocationListCallback {
                override fun onSuccess(locations: List<Location>, hasMore: Boolean) {
                    if (locations.isNotEmpty()) {
                        val firstLocation = locations.first()
                        selectedLocationId = firstLocation.id
                        runOnUiThread {
                            locationStatusTextView.text = "Location: ${firstLocation.displayName ?: firstLocation.id}"
                        }
                    } else {
                        runOnUiThread {
                            locationStatusTextView.text = "No locations found."
                        }
                    }
                }

                override fun onFailure(e: TerminalException) {
                    runOnUiThread {
                        locationStatusTextView.text = "Location fetch failed: ${e.errorMessage}"
                    }
                }
            }
        )
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun connectToTapToPayReader(isSim:Boolean) {
        val config = DiscoveryConfiguration.TapToPayDiscoveryConfiguration(
            isSimulated = isSim
        )

        discoverCancelable = Terminal.getInstance().discoverReaders(
            config,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    if (readers.isNotEmpty()) {
                        val reader = readers.first()

                        // Fallback: if no location on reader, use a predefined one
                        val locationId = reader.location?.id ?: selectedLocationId
                        if (locationId == null) {
                            Toast.makeText(
                                this@DiscoverReadersActivity,
                                "No location found for reader and no fallback set.",
                                Toast.LENGTH_LONG
                            ).show()
                            return
                        }

                        val connectionConfig = TapToPayConnectionConfiguration(
                            locationId = locationId,
                            tapToPayReaderListener = this@DiscoverReadersActivity,
                            autoReconnectOnUnexpectedDisconnect = true
                        )

                        Terminal.getInstance().connectReader(
                            reader,
                            connectionConfig,
                            object : ReaderCallback {
                                override fun onSuccess(reader: Reader) {
                                    discoverCancelable = null
                                    connectedReader = reader
                                    updateReaderStatus()
                                }

                                override fun onFailure(e: TerminalException) {
                                    readerStatusTextView.text =
                                        "Connection failed: ${e.errorMessage}"
                                }
                            }
                        )
                    }
                }
            },
            object : Callback {
                override fun onSuccess() {
                    readerStatusTextView.text = "Searching for readers..."
                }

                override fun onFailure(e: TerminalException) {
                    readerStatusTextView.text = "Discovery failed: ${e.errorMessage}"
                }
            }
        )
    }

    private fun createAndCollectPayment(amount: Double) {
        val amountInCents = (amount * 100).toLong()

        createPaymentIntentFromBackend(amountInCents) { paymentIntent ->
            currentPaymentIntent = paymentIntent

            Terminal.getInstance().collectPaymentMethod(paymentIntent,
                object : PaymentIntentCallback {
                    override fun onSuccess(collectedIntent: PaymentIntent) {
//                        Terminal.getInstance().processPayment(collectedIntent,
//                            object : PaymentIntentCallback {
//                                override fun onSuccess(processedIntent: PaymentIntent) {
//                                    Toast.makeText(
//                                        this@DiscoverReadersActivity,
//                                        "Payment succeeded!",
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                }
//
//                                override fun onFailure(e: TerminalException) {
//                                    Toast.makeText(
//                                        this@DiscoverReadersActivity,
//                                        "Processing failed: ${e.errorMessage}",
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                }
//                            })
                    }

                    override fun onFailure(e: TerminalException) {
                        Toast.makeText(
                            this@DiscoverReadersActivity,
                            "Collection failed: ${e.errorMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }

    // Replace this with real backend logic
    private fun createPaymentIntentFromBackend(amount: Long, callback: (PaymentIntent) -> Unit) {
        Toast.makeText(this, "Mocking PaymentIntent for $amount", Toast.LENGTH_SHORT).show()
//        val dummyIntent = PaymentIntent.fromJson(mapOf(
//            "id" to "pi_mock",
//            "amount" to amount,
//            "currency" to "usd"
//        ))
//        callback(dummyIntent)
    }

    override fun onStop() {
        super.onStop()
        discoverCancelable?.cancel(object : Callback {
            override fun onSuccess() {
                readerStatusTextView.text = "Discovery cancelled"
            }

            override fun onFailure(e: TerminalException) {
                readerStatusTextView.text = "Failed to cancel discovery: ${e.errorMessage}"
            }
        })
    }

}
