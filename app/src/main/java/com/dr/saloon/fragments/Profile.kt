package com.dr.saloon.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.dr.saloon.payments.PaymentActivity
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentProfileBinding
import com.dr.saloon.utils.Utils.Companion.getCurrentBalanceInString
import com.dr.saloon.utils.Utils.Companion.getEmail
import com.dr.saloon.utils.Utils.Companion.getProfileUrl
import com.dr.saloon.utils.Utils.Companion.getTotalAppointments
import com.dr.saloon.utils.Utils.Companion.getUserName

class Profile : Fragment() {

    private lateinit var binding : FragmentProfileBinding
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actions()

    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun actions() {

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.profile,true)
        }

        binding.settingAndPrivacy.setOnClickListener {
            findNavController().navigate(R.id.settings)
        }

        binding.myAppointments.setOnClickListener {
            findNavController().navigate(R.id.myAppointments)
        }

        binding.addBalance.setOnClickListener {
            startActivity(Intent(mContext, PaymentActivity::class.java))
        }

        binding.myPayment.setOnClickListener {
            findNavController().navigate(R.id.myPayments)
        }

        binding.referAndEarn.setOnClickListener {
            findNavController().navigate(R.id.referAndEarn)
        }

        binding.becomeAPartner.setOnClickListener {
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("https://play.google.com/store/apps/details?id=com.dr.vellaadminforvendor")
                startActivity(i)
            } catch (e: Exception) {}
        }

        binding.vellaMall.setOnClickListener {
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse("http://vellasalonmall.unaux.com/shop/")
                startActivity(i)
            } catch (e: Exception) {}
        }

    }

    private fun updateUi() {

        binding.userName.text = getUserName(mContext)
        binding.email.text = getEmail(mContext)
        binding.appointments.text = getTotalAppointments(mContext).toString()
        binding.profile.load(getProfileUrl(mContext))
        binding.balance.text = getCurrentBalanceInString(mContext).plus("₹")

    }

}