package com.dr.saloon.fragments.appointments

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.saloon.R
import com.dr.saloon.database.AppointmentEntity
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.databinding.*
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_CHARGE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_DATE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_REASON
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS_CANCELLED
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS_DONE
import com.dr.saloon.utils.AppConstaints.COLLECTION_APPOINTMENTS
import com.dr.saloon.utils.AppConstaints.COLLECTION_SALON
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLOR_GREEN
import com.dr.saloon.utils.AppConstaints.COLOR_RED
import com.dr.saloon.utils.AppConstaints.COLOR_SECONDARY_1
import com.dr.saloon.utils.AppConstaints.DEFAULT_DATE_FORMAT
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.RESPONSE
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.dr.saloon.utils.AppConstaints.SERVICE_COST
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getCancellationCharge
import com.dr.saloon.utils.Utils.Companion.getFormattedFloatInString
import com.dr.saloon.utils.Utils.Companion.getPercentage
import com.dr.saloon.utils.Utils.Companion.stringToArrayList
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MyAppointments : Fragment() {

    private lateinit var mContext: Context
    private lateinit var binding: FragmentMyAppointmentsBinding
    val db = FirebaseFirestore.getInstance()
    private lateinit var room: MyDatabase
    private lateinit var d : Dialog
    private lateinit var appointmentCancelDialog: Dialog
    private lateinit var appointmentCancelBinding: DialogCancelAppointmentBinding
    private lateinit var appointmentDetailsDialog : DialogAppointmentDetailsBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMyAppointmentsBinding.inflate(layoutInflater)
        room = MyDatabase.getDatabase(mContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actions()

    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rv.removeAllViewsInLayout()
        } catch (e : Exception) { }
    }

    private fun actions() {

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.myAppointments, true)
        }

    }

    private fun loadData() {

        room.appointmentsDao().getAllAppointments()
            .observeForever{

                val list = it as ArrayList<AppointmentEntity>

                binding.pb.visibility = View.GONE
                if (list.isEmpty()) {
                    binding.noData.visibility = View.VISIBLE
                    return@observeForever
                }

                binding.rv.adapter = AdapterMyAppointments(list)

            }
    }

    private inner class AdapterMyAppointments(val list: ArrayList<AppointmentEntity>) :
        RecyclerView.Adapter<AdapterMyAppointments.AppointmentHolder>() {

        inner class AppointmentHolder(
            itemView: View,
            val rowItemMyAppointmentsBinding: RowItemAppointmentsBinding
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterMyAppointments.AppointmentHolder {
            val binding = RowItemAppointmentsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return AppointmentHolder(binding.root, binding)
        }

        override fun onBindViewHolder(
            holder: AdapterMyAppointments.AppointmentHolder,
            position: Int
        ) {
            try {

                holder.rowItemMyAppointmentsBinding.dateTime.text = SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH).format(Date(list[position].dateTime))
                holder.rowItemMyAppointmentsBinding.price.text = "Amount Paid: ₹".plus(list[position].amountPaid)
                holder.rowItemMyAppointmentsBinding.response.text = list[position].response

                when (list[position].appointmentStatus) {
                    APPOINTMENT_STATUS_CANCELLED -> {
                        holder.rowItemMyAppointmentsBinding.response.setTextColor(Color.parseColor(
                            COLOR_RED))
                        holder.rowItemMyAppointmentsBinding.price.paintFlags = holder.rowItemMyAppointmentsBinding.price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    APPOINTMENT_STATUS_DONE -> {
                        holder.rowItemMyAppointmentsBinding.response.setTextColor(Color.parseColor(
                            COLOR_GREEN))
                    }
                    else -> {
                        holder.rowItemMyAppointmentsBinding.response.setTextColor(Color.parseColor(
                            COLOR_SECONDARY_1))
                    }
                }

                val servicesList = stringToArrayList(list[position].services)
                var services = ""
                for (z in servicesList) {
                    services += z.split('?')[0] + ", "
                }

                holder.rowItemMyAppointmentsBinding.serviceName.text = services

                db.collection(COLLECTION_SALON)
                    .document(list[position].salonId)
                    .get().addOnSuccessListener {
                        try {
                            val salonName = it[SALON_NAME].toString()

                            if (it.exists()) {
                                holder.rowItemMyAppointmentsBinding.salonName.text = salonName
                            } else {
                                holder.rowItemMyAppointmentsBinding.salonName.text = "Salon no longer available"
                            }

                            holder.rowItemMyAppointmentsBinding.salonName.text = salonName

                            holder.itemView.setOnClickListener {
                                detailsDialog(
                                    list[position].dateTime,
                                    salonName,
                                    stringToArrayList(list[position].services),
                                    list[position].amountPaid,
                                    list[position].appliedReferBonus,
                                    list[position].totalAmount,
                                    list[position].documentId,
                                    list[position].appointmentStatus,
                                    list[position].response
                                )
                            }
                        } catch (e : Exception) {}
                    }

            } catch (e: Exception) {}
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private fun detailsDialog(date : Long, salonName : String, services : ArrayList<String>,
                              amountPaid : Float ,
                              appliedReferralBonus: Float,
                              totalCost : Float, id: String, status : String, response: String) {

        d = Dialog(mContext)
        appointmentDetailsDialog = DialogAppointmentDetailsBinding.inflate(d.layoutInflater)
        d.setContentView(appointmentDetailsDialog.root)
        d.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        d.setCancelable(false)
        d.show()

        if (appliedReferralBonus <= 0f) {
            appointmentDetailsDialog.appliedReferBonus.visibility = View.GONE
            appointmentDetailsDialog.txtAppliedReferIncome.visibility = View.GONE
        } else {
            appointmentDetailsDialog.appliedReferBonus.visibility = View.VISIBLE
            appointmentDetailsDialog.txtAppliedReferIncome.visibility = View.VISIBLE
        }

        val list = ArrayList<ModelServicesInAppointment>()

        for (x in services) {
            list.add(
                ModelServicesInAppointment(
                    try {
                        x.split('?')[0]
                    } catch (e: Exception) { "" },
                    try {
                        x.split('?')[2].toFloat()
                    } catch (e: Exception) { 0f },
                    try {
                        x.split('?')[1]
                    } catch (e: Exception) { "" }
                )
            )
        }

        appointmentDetailsDialog.rvSelectedServices.adapter = AdapterServices(list)

        appointmentDetailsDialog.salonName.text = salonName
        appointmentDetailsDialog.amountPaid.text =   amountPaid.toString().plus("₹")
        appointmentDetailsDialog.appliedReferBonus.text = appliedReferralBonus.toString().plus("₹")
        appointmentDetailsDialog.date.text = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(date))
        appointmentDetailsDialog.time.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(date))
        appointmentDetailsDialog.response.text = response

        appointmentDetailsDialog.done.setOnClickListener {
             d.dismiss()
        }

        appointmentDetailsDialog.close.setOnClickListener {
            d.dismiss()
        }

        appointmentDetailsDialog.cancel.setOnClickListener {
            d.dismiss()
            appointmentCancelDialog(totalCost, id)
        }

        db.collection(COLLECTION_APPOINTMENTS)
            .document(id)
            .get().addOnSuccessListener {
                try {
                    val serviceCost = it[SERVICE_COST].toString()
                    appointmentDetailsDialog.totalServiceCost.text = serviceCost.plus("₹")
                    if (status == APPOINTMENT_STATUS_CANCELLED) {
                        val mCancellationCharge = it[APPOINTMENT_CANCELLATION_CHARGE].toString()
                        appointmentDetailsDialog.cancellationCharge.text = mCancellationCharge.plus("₹")
                        appointmentDetailsDialog.cancellationDate.text = SimpleDateFormat(
                            DEFAULT_DATE_FORMAT, Locale.ENGLISH).format(Date(it[APPOINTMENT_CANCELLATION_DATE].toString().toLong()))
                    }
                } catch (e: Exception) { }
            }

        if (status == APPOINTMENT_STATUS_CANCELLED) {

            appointmentDetailsDialog.cancel.visibility = View.GONE
            appointmentDetailsDialog.cancellationCharge.visibility = View.VISIBLE
            appointmentDetailsDialog.txtCancellationCharge.visibility = View.VISIBLE
            appointmentDetailsDialog.txtCancellationDate.visibility = View.VISIBLE
            appointmentDetailsDialog.cancellationDate.visibility = View.VISIBLE

        }
        else
        {
            appointmentDetailsDialog.cancel.visibility = View.VISIBLE
            appointmentDetailsDialog.cancellationCharge.visibility = View.GONE
            appointmentDetailsDialog.txtCancellationCharge.visibility = View.GONE
            appointmentDetailsDialog.txtCancellationDate.visibility = View.GONE
            appointmentDetailsDialog.cancellationDate.visibility = View.GONE
        }
        if (status == APPOINTMENT_STATUS_DONE) {
            appointmentDetailsDialog.cancel.visibility = View.GONE
        }

    }

    private fun appointmentCancelDialog(totalAmount: Float, appointmentId: String) {

        appointmentCancelDialog = Dialog(mContext)
        appointmentCancelBinding = DialogCancelAppointmentBinding.inflate(appointmentCancelDialog.layoutInflater)
        appointmentCancelDialog.setContentView(appointmentCancelBinding.root)
        appointmentCancelDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        appointmentCancelDialog.setCancelable(false)
        appointmentCancelDialog.show()

        appointmentCancelBinding.close.setOnClickListener {
            appointmentCancelDialog.dismiss()
        }

        appointmentCancelBinding.cancellationCharge.text = "Cancellation Charge: ".plus(getFormattedFloatInString(getPercentage(totalAmount, getCancellationCharge(mContext)), 2).plus("₹"))

        appointmentCancelBinding.confirm.setOnClickListener {

            if (appointmentCancelBinding.reason.text.toString().trim().isEmpty()) {
                appointmentCancelBinding.reason.error = "Please describe the reason of cancellation"
                return@setOnClickListener
            }

            showLoadingOnCancel(true)

            val map = HashMap<String, Any>()
            map[APPOINTMENT_STATUS] = APPOINTMENT_STATUS_CANCELLED
            map[APPOINTMENT_CANCELLATION_REASON] = appointmentCancelBinding.reason.text.toString().trim()
            map[APPOINTMENT_CANCELLATION_DATE] = System.currentTimeMillis()
            map[APPOINTMENT_CANCELLATION_CHARGE] = getPercentage(totalAmount, getCancellationCharge(mContext))
            map[RESPONSE] = "Your appointment has been cancelled."

            db.collection(COLLECTION_APPOINTMENTS)
                .document(appointmentId)
                .set(map, SetOptions.merge())
                .addOnSuccessListener {
                    db.collection(COLLECTION_APPOINTMENTS)
                        .document(appointmentId)
                        .get().addOnSuccessListener { d->
                            showLoadingOnCancel(false)
                            appointmentCancelDialog.dismiss()
                            Utils.countBalance(mContext)
                            try {
                                CoroutineScope(IO)
                                    .launch {
                                        room.appointmentsDao().updateAppointment(
                                            AppointmentEntity(
                                                d.getDate(TIMESTAMP_FIELD)?.time as Long,
                                                (d[SERVICES] as ArrayList<String>).toString(),
                                                d[AppConstaints.SALON_ID].toString(),
                                                d[AppConstaints.TOTAL_AMOUNT_PAID].toString().toFloat(),
                                                d[AppConstaints.TOTAL_AMOUNT].toString().toFloat(),
                                                d[AppConstaints.APPLIED_REFER_BALANCE].toString().toFloat(),
                                                d[APPOINTMENT_STATUS].toString(),
                                                d[RESPONSE].toString(),
                                                d.id
                                            )
                                        )
                                    }
                            } catch (e: Exception) {}
                        }
                }
        }
    }

    private fun showLoadingOnCancel(isShow: Boolean) {
        if (isShow) {
            appointmentCancelBinding.confirm.isEnabled = false
            appointmentCancelBinding.confirm.text = ""
            appointmentCancelBinding.pb.visibility = View.VISIBLE
        } else {
            appointmentCancelBinding.confirm.isEnabled = true
            appointmentCancelBinding.confirm.text = "Confirm Cancellation"
            appointmentCancelBinding.pb.visibility = View.INVISIBLE
        }
    }

    private inner class AdapterServices(val list : ArrayList<ModelServicesInAppointment>): RecyclerView.Adapter<AdapterServices.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val dBinding : RowItemSelectedServicesListBookAppointmentBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterServices.ServiceHolder {
            val dBinding = RowItemSelectedServicesListBookAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(holder: AdapterServices.ServiceHolder, position: Int) {
            holder.dBinding.servicePrice.text = "₹".plus(list[position].serviceCost.toString())
            holder.dBinding.serviceName.text = list[position].serviceName
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

    private inner class ModelServicesInAppointment(val serviceName: String, val serviceCost: Float, val serviceId: String)

}