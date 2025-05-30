package com.stripe.example.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.edit
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.stripe.example.DiscoverReadersActivity
import com.stripe.example.NavigationListener
import com.stripe.example.R
import com.stripe.example.ReaderConnectionPersistence
import com.stripe.example.databinding.FragmentTerminalBinding
import com.stripe.example.fragment.discovery.DiscoveryMethod
import com.stripe.example.viewmodel.TerminalViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The `TerminalFragment` is the main [Fragment] shown in the app, and handles navigation to any
 * other [Fragment]s as necessary.
 */
class TerminalFragment : Fragment(R.layout.fragment_terminal) {

    companion object {
        const val TAG = "com.stripe.example.fragment.TerminalFragment"

        // A string to store if the simulated switch is set
        private const val SIMULATED_SWITCH = "simulated_switch"

        // A string to store the selected discovery method
        private const val DISCOVERY_METHOD = "discovery_method"
        private val discoveryMethods =
            listOf(
                DiscoveryMethod.BLUETOOTH_SCAN,
                DiscoveryMethod.INTERNET,
                DiscoveryMethod.TAP_TO_PAY,
                DiscoveryMethod.USB
            )

        fun getCurrentDiscoveryMethod(activity: Activity?): DiscoveryMethod {
            val pos = activity?.getSharedPreferences(TAG, Context.MODE_PRIVATE)
                ?.getInt(DISCOVERY_METHOD, 0) ?: 0

            return discoveryMethods[pos]
        }
    }

    private lateinit var viewModel: TerminalViewModel
    private lateinit var readerConnectionPersistence: ReaderConnectionPersistence

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        readerConnectionPersistence = ReaderConnectionPersistence(requireContext().applicationContext)

        arguments?.let {
            viewModel = TerminalViewModel(
                it.getSerializable(DISCOVERY_METHOD) as DiscoveryMethod,
                discoveryMethods,
                it.getBoolean(SIMULATED_SWITCH)
            )
        } ?: run {
            CoroutineScope(Dispatchers.IO).launch {
                val isSimulated = activity?.getSharedPreferences(
                    TAG,
                    Context.MODE_PRIVATE
                )?.getBoolean(SIMULATED_SWITCH, false) ?: false
                val discoveryMethod = activity?.getSharedPreferences(
                    TAG,
                    Context.MODE_PRIVATE
                )?.getInt(DISCOVERY_METHOD, 0) ?: 0
                viewModel =
                    TerminalViewModel(discoveryMethods[discoveryMethod], discoveryMethods, isSimulated)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inflate the layout for this fragment
        val viewBinding = requireNotNull(
            DataBindingUtil.bind<FragmentTerminalBinding>(view)
        )
        viewBinding.lifecycleOwner = viewLifecycleOwner
        viewBinding.viewModel = viewModel

        // Set the device type spinner
        viewBinding.discoveryMethodSpinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            discoveryMethods
        )

        // Link up the discovery button
        viewBinding.discoverButton.setOnClickListener {
            readerConnectionPersistence.saveDiscoverySimulationStatus(viewModel.simulated)
            (activity as? NavigationListener)?.onRequestDiscovery(
                viewModel.simulated,
                viewModel.discoveryMethod,
            )
        }

        // Link tap to pay button
        viewBinding.discoverTaptopay.setOnClickListener {
            val intent = Intent(requireContext(), DiscoverReadersActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.let {
            it.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit {
                putBoolean(SIMULATED_SWITCH, viewModel.simulated)
                putInt(DISCOVERY_METHOD, viewModel.discoveryMethodPosition)
            }
        }
    }
}
