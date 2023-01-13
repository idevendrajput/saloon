package com.dr.saloon.refer

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dr.saloon.R
import com.dr.saloon.databinding.DialogTAndCBinding
import com.dr.saloon.databinding.FragmentReferAndEarnBinding
import com.dr.saloon.utils.Utils.Companion.getPhone
import com.dr.saloon.utils.Utils.Companion.getReferBalance
import com.dr.saloon.utils.Utils.Companion.getReferPolicy

class ReferAndEarn : Fragment() {

    lateinit var binding : FragmentReferAndEarnBinding
    lateinit var alertDialog : Dialog
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentReferAndEarnBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actions()
        updateUi()

    }

    private fun actions() {

        binding.copycodetxt.setOnClickListener{
            val clipboardManager =
                binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText(
                "text",
                binding.referralcode.text.toString()
            )
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(mContext, "Code Copied to Clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.refernowbtn.setOnClickListener{

            try {
                val intent = Intent(Intent.ACTION_SEND)
                intent.apply {
                    putExtra(Intent.EXTRA_TEXT,"https://play.google.com/store/apps/details?id=com.dr.saloon")
                    type = "text/plain"
                }
                startActivity(intent)

            } catch (e: Exception) { }

        }

        binding.termsandconditiontxt.setOnClickListener{
            alertDialog = Dialog(binding.root.context)

            val alertDialogBinding = DialogTAndCBinding.inflate(
                layoutInflater
            )
            alertDialog.setContentView(alertDialogBinding.root)
            alertDialogBinding.termsAndConditions.text = getReferPolicy(mContext)

            alertDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            alertDialog.show()
        }

        binding.myEarnings.setOnClickListener {
            findNavController().navigate(R.id.myEarnings)
        }

    }

    private fun updateUi() {

        binding.availableBalance.text = "Available Refer Balance: ₹".plus(String.format("%.2f",  getReferBalance(mContext)))
        binding.referralcode.text = getPhone(mContext)

    }

}