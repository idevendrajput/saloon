package com.dr.saloon.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.navigation.fragment.findNavController
import com.dr.saloon.R
import com.dr.saloon.databinding.CustomAlertDialogBinding
import com.dr.saloon.databinding.FragmentSettingsBinding
import com.dr.saloon.utils.AppConstaints.ABOUT_US
import com.dr.saloon.utils.AppConstaints.CONTACT_US
import com.dr.saloon.utils.AppConstaints.CONTENT
import com.dr.saloon.utils.AppConstaints.PRIVACY_POLICY
import com.dr.saloon.utils.AppConstaints.REFUND_POLICY
import com.dr.saloon.utils.AppConstaints.TERMS_AND_CONDITIONS
import com.dr.saloon.utils.AppConstaints.THE_TITLE
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.Utils.Companion.getAboutUs
import com.dr.saloon.utils.Utils.Companion.getContactUs
import com.dr.saloon.utils.Utils.Companion.getPrivacyPolicy
import com.dr.saloon.utils.Utils.Companion.getReferPolicy
import com.dr.saloon.utils.Utils.Companion.getRefundPolicy
import com.dr.saloon.utils.Utils.Companion.getTermsAndConditions
import com.dr.saloon.utils.Utils.Companion.isNotificationOn
import com.dr.saloon.utils.Utils.Companion.setNotificationOn

class Settings : Fragment() {

    private lateinit var binding : FragmentSettingsBinding
    private lateinit var mContext : Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(layoutInflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadPreviousData()
        actions()

    }

    private fun actions() {

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.settings,true)
        }

        binding.editProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfile)
        }

        binding.aboutUs.setOnClickListener {
            val args = Bundle()
            args.putString(THE_TITLE, ABOUT_US)
            args.putString(CONTENT, getAboutUs(mContext))
            findNavController().navigate(R.id.policyViewer, args)
        }

        binding.contactUs.setOnClickListener {
            val args = Bundle()
            args.putString(THE_TITLE, CONTACT_US)
            args.putString(CONTENT, getContactUs(mContext))
            findNavController().navigate(R.id.policyViewer, args)
        }

        binding.privacyPolicy.setOnClickListener {
            val args = Bundle()
            args.putString(THE_TITLE, PRIVACY_POLICY)
            args.putString(CONTENT, getPrivacyPolicy(mContext))
            findNavController().navigate(R.id.policyViewer, args)
        }

        binding.refundPolicy.setOnClickListener {
            val args = Bundle()
            args.putString(THE_TITLE, REFUND_POLICY)
            args.putString(CONTENT, getRefundPolicy(mContext))
            findNavController().navigate(R.id.policyViewer, args)
        }

        binding.termsAndConditions.setOnClickListener {
            val args = Bundle()
            args.putString(THE_TITLE, TERMS_AND_CONDITIONS)
            args.putString(CONTENT, getTermsAndConditions(mContext))
            findNavController().navigate(R.id.policyViewer, args)
        }

        binding.logOut.setOnClickListener {
            logOut()
        }

        binding.switchNotification.setOnClickListener {
            setNotificationOn(mContext, binding.switchNotification.isChecked)
        }

    }

    private fun logOut() {

        val logOut = Dialog(mContext)
        val logOutBinding = CustomAlertDialogBinding.inflate(logOut.layoutInflater)
        logOut.setContentView(logOutBinding.root)
        logOut.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        logOut.show()

        logOutBinding.negativeButton.setOnClickListener {
            logOut.dismiss()
        }

        logOutBinding.positiveButton.setOnClickListener {
            SharedPref.logOut(mContext)
            logOut.dismiss()
            findNavController().navigate(R.id.home)
        }

    }

    private fun loadPreviousData() {

       binding.switchNotification.isChecked = isNotificationOn(mContext)


    }

}