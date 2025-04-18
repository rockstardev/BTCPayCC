package com.stripe.example.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.stripe.example.R

interface NameEmailFormResultListener {
    fun onNameEmailFormResult(originTag: String)
}

class NameEmailFormFragment : Fragment(R.layout.fragment_name_email_form) {

    private var listener: NameEmailFormResultListener? = null
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var submitButton: Button
    private var originTag: String = ORIGIN_CONNECTED // Default

    companion object {
        private const val ARG_ORIGIN_TAG = "origin_tag"
        const val ORIGIN_CONNECTED = "connected"
        const val ORIGIN_KEYPAD = "keypad"

        fun newInstance(originTag: String): NameEmailFormFragment {
            val fragment = NameEmailFormFragment()
            val args = Bundle()
            args.putString(ARG_ORIGIN_TAG, originTag)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is NameEmailFormResultListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement NameEmailFormResultListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            originTag = it.getString(ARG_ORIGIN_TAG, ORIGIN_CONNECTED)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nameInput = view.findViewById(R.id.nameInput)
        emailInput = view.findViewById(R.id.emailInput)
        submitButton = view.findViewById(R.id.submitButton)
        val skipButton = view.findViewById<Button>(R.id.skipButton)

        submitButton.setOnClickListener {
            val name = nameInput.text.toString()
            val email = emailInput.text.toString()

            // Here you would typically save the name and email
            // Notify the listener
            listener?.onNameEmailFormResult(originTag)
        }
        skipButton.setOnClickListener {
            // Notify the listener even when skipped, so MainActivity can navigate back
            listener?.onNameEmailFormResult(originTag)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
