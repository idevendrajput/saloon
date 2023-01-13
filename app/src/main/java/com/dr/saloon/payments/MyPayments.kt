package com.dr.saloon.payments

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentMyAppointmentsBinding
import com.dr.saloon.databinding.RowItemPaymentsBinding
import com.dr.saloon.utils.AppConstaints.AMOUNT
import com.dr.saloon.utils.AppConstaints.COLLECTION_PAYMENT_ORDERS
import com.dr.saloon.utils.AppConstaints.DEFAULT_DATE_FORMAT
import com.dr.saloon.utils.AppConstaints.ORDER_ID
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS_SUCCESS
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.Utils.Companion.mUid
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MyPayments : Fragment() {

    private lateinit var binding: FragmentMyAppointmentsBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAppointmentsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actions()

    }

    private fun actions() {

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.myPayments, true)
        }

    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rv.removeAllViewsInLayout()
        } catch (e: Exception) {}
    }

    private fun loadData() {

        val list = ArrayList<ModelPayments>()
        val adapter = AdapterPayments(list)

        db.collection(COLLECTION_PAYMENT_ORDERS)
            .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .whereEqualTo(USER_ID, mUid(mContext))
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                for (d in it) {
                    try {
                        if ((d[PAYMENT_STATUS]
                                ?: d[PAYMENT_STATUS].toString()) == PAYMENT_STATUS_SUCCESS
                        )
                        list.add(ModelPayments(
                            d[AMOUNT].toString(),
                            d[ORDER_ID].toString(),
                            SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH).format(d.getDate(
                                TIMESTAMP_FIELD) as Date),
                            d[PAYMENT_STATUS].toString(),
                            d.id
                        ))
                        adapter.notifyItemInserted(list.size - 1)

                        binding.pb.visibility = View.GONE
                    } catch (e : Exception) {}
                }
            }

        binding.rv.adapter = adapter

    }

    private inner class AdapterPayments(val list: ArrayList<ModelPayments>) : RecyclerView.Adapter<AdapterPayments.PaymentHolder>() {

        inner class PaymentHolder(itemView: View, val rowItemMyPaymentsBinding: RowItemPaymentsBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterPayments.PaymentHolder {
            val rowItemPaymentsBinding = RowItemPaymentsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
             return PaymentHolder(rowItemPaymentsBinding.root, rowItemPaymentsBinding)
        }

        override fun onBindViewHolder(holder: AdapterPayments.PaymentHolder, position: Int) {

           val typeface = Typeface.createFromAsset(
                    (activity as AppCompatActivity).assets,
                    "fonts/futura-medium-bt.ttf"
                )
            holder.rowItemMyPaymentsBinding.amount.typeface = typeface

            holder.rowItemMyPaymentsBinding.amount.text = "Amount: ".plus(list[position].amount).plus("₹")
            holder.rowItemMyPaymentsBinding.dateTime.text = "Date: ".plus(list[position].dateTime)
            holder.rowItemMyPaymentsBinding.orderId.text = "OrderId: ".plus(list[position].orderId)
            holder.rowItemMyPaymentsBinding.paymentStatus.text = "Payment Status: ".plus(list[position].status)

        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

    private inner class ModelPayments(
        val amount: String,
        val orderId : String,
        val dateTime: String,
        val status : String,
        val id : String
    )

}