package com.btcpaycc

import android.app.Activity
import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.*
import com.stripe.stripeterminal.external.models.*
import kotlinx.coroutines.*

class TapToPayHandler(
    private val activity: Activity,
    private val context: Context
) : ConnectionTokenProvider {

    var onSuccess: (() -> Unit)? = null
    var onFailure: ((String) -> Unit)? = null

    init {
        Terminal.initTerminal(
            context,
            LogLevel.VERBOSE,
            this,
            null
        )
    }

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        // Call your backend to get a connection token
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = fetchFromServer() // your implementation
                callback.onSuccess(token)
            } catch (e: Exception) {
                callback.onFailure(e)
            }
        }
    }

    fun startPayment(amount: Long) {
        val config = DiscoveryConfiguration(DiscoveryMethod.INTERNET, 0, false)
        Terminal.getInstance().discoverReaders(config, object : DiscoveryListener {
            override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                if (readers.isNotEmpty()) {
                    Terminal.getInstance().connectInternetReader(
                        readers[0],
                        ConnectionConfiguration.InternetConnectionConfiguration(),
                        object : ReaderCallback {
                            override fun onSuccess(reader: Reader) {
                                beginPayment(amount)
                            }

                            override fun onFailure(e: TerminalException) {
                                onFailure?.invoke(e.message ?: "Reader connection failed")
                            }
                        }
                    )
                }
            }
        })
    }

    private fun beginPayment(amount: Long) {
        val params = PaymentIntentParameters.Builder()
            .setAmount(amount)
            .setCurrency("usd")
            .build()

        Terminal.getInstance().createPaymentIntent(params,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    collectCard(paymentIntent)
                }

                override fun onFailure(e: TerminalException) {
                    onFailure?.invoke(e.message ?: "Failed to create payment intent")
                }
            })
    }

    private fun collectCard(intent: PaymentIntent) {
        Terminal.getInstance().collectPaymentMethod(intent,
            object : PaymentIntentCallback {
                override fun onSuccess(collectedIntent: PaymentIntent) {
                    process(collectedIntent)
                }

                override fun onFailure(e: TerminalException) {
                    onFailure?.invoke("Card collection failed: ${e.message}")
                }
            })
    }

    private fun process(intent: PaymentIntent) {
        Terminal.getInstance().processPayment(intent,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    onSuccess?.invoke()
                }

                override fun onFailure(e: TerminalException) {
                    onFailure?.invoke("Payment failed: ${e.message}")
                }
            })
    }

    private fun fetchFromServer(): String {
        // Replace with actual API call to your backend
        val url = URL("https://your-server.com/connection_token")
        return url.readText().replace("\"", "")
    }
}
