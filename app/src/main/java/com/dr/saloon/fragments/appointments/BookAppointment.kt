package com.dr.saloon.fragments.appointments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bumptech.glide.util.Util
import com.dr.saloon.payments.PaymentActivity
import com.dr.saloon.R
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.database.SelectedServicesDao
import com.dr.saloon.database.SelectedServicesEntity
import com.dr.saloon.databinding.*
import com.dr.saloon.notifications.Notification
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.APPLIED_REFER_BALANCE
import com.dr.saloon.utils.AppConstaints.APPLY_REFERRAL_INCOME
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CHARGE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS_PENDING
import com.dr.saloon.utils.AppConstaints.COLLECTION_APPOINTMENTS
import com.dr.saloon.utils.AppConstaints.COLLECTION_SALON
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLOR_SECONDARY_1
import com.dr.saloon.utils.AppConstaints.DATE_TIME
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.IS_SALON_AVAILABLE
import com.dr.saloon.utils.AppConstaints.RESPONSE
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.dr.saloon.utils.AppConstaints.SERVICE_COST
import com.dr.saloon.utils.AppConstaints.SERVICE_ID
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.SOMETHING_WENT_WRONG
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.TOTAL_AMOUNT
import com.dr.saloon.utils.AppConstaints.TOTAL_AMOUNT_PAID
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.dr.saloon.utils.DataStore
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.countBalance
import com.dr.saloon.utils.Utils.Companion.getAppointmentCharge
import com.dr.saloon.utils.Utils.Companion.getAppointmentChargeInPercent
import com.dr.saloon.utils.Utils.Companion.getCurrentBalance
import com.dr.saloon.utils.Utils.Companion.getPercentage
import com.dr.saloon.utils.Utils.Companion.getReferBalance
import com.dr.saloon.utils.Utils.Companion.getUserName
import com.dr.saloon.utils.Utils.Companion.mUid
import com.dr.saloon.utils.Utils.Companion.setCurrentBalance
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.tooltip.Tooltip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class BookAppointment : Fragment() {

    private lateinit var binding : FragmentBookAppointmentBinding
    private lateinit var mContext : Context
    private val db = FirebaseFirestore.getInstance()
    private lateinit var d : Dialog
    private lateinit var salonId : String
    private lateinit var allServiceDialog: BottomSheetDialog
    private lateinit var confirmationDialogBinding : DialogAppointmentConfirmationBinding
    private lateinit var timingErrorDialog: Dialog
    private lateinit var timingErrorBinding: DialogTimingErrorMessageBinding
    private lateinit var dataStore : DataStore
    private lateinit var room : MyDatabase
    private lateinit var selectedServiceId: String
    private var salonOpenTiming = 0
    private var salonCloseTiming = 0
    private var appointmentChargePercentage = 0f
    private lateinit var salonName: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        room = MyDatabase.getDatabase(mContext)
        dataStore = DataStore(mContext)
        binding = FragmentBookAppointmentBinding.inflate(layoutInflater)
        salonId = arguments?.getString(SALON_ID).toString()
        CoroutineScope(IO)
            .launch {
                room.selectedServices().delete()
            }
        selectedServiceId = arguments?.getString(SERVICE_ID).toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSalonData()
    }

    private fun loadSalonData() {

        db.collection(COLLECTION_SALON)
            .document(salonId)
            .get().addOnSuccessListener {
                try {

                    salonName = it[SALON_NAME].toString()
                    val services = it[SERVICES] as ArrayList<String>
                    binding.text.text = "Book appointment at\n$salonName"

                    appointmentChargePercentage = if (it[APPOINTMENT_CHARGE] != null) it[APPOINTMENT_CHARGE].toString().toFloat() else getAppointmentChargeInPercent(mContext)

                    binding.card.visibility = View.GONE
                    if (it[IS_SALON_AVAILABLE] == null) {
                        binding.card.visibility = View.VISIBLE
                        binding.pay.visibility = View.GONE
                    }

                    val timing = it[AppConstaints.SALON_TIMING] as ArrayList<String>

                    var openOn = timing[0]
                    var closeOn = timing[1]

                    salonOpenTiming = openOn.toInt()
                    salonCloseTiming = closeOn.toInt()

                    timingErrorDialog()

                    openOn = if (openOn.toInt()  >= 720) {
                        Utils.getFormattedTimeWithHourAndMinute(openOn.toInt() - 720).plus("PM")
                    } else {
                        Utils.getFormattedTimeWithHourAndMinute(openOn.toInt()).plus("AM")
                    }

                    closeOn = if (closeOn.toInt()  >= 720) {
                        Utils.getFormattedTimeWithHourAndMinute(closeOn.toInt() - 720).plus("PM")
                    } else {
                        Utils.getFormattedTimeWithHourAndMinute(closeOn.toInt()).plus("AM")
                    }

                    val time = "$openOn - $closeOn"

                    timingErrorBinding.salonTiming.text = "$salonName's Working hour $time"

                    loadServices(services)
                    actions()

                } catch (e : Exception) { }
            }
    }

    private fun confirmationDialog(date : Long) {

        d = Dialog(mContext)
        confirmationDialogBinding = DialogAppointmentConfirmationBinding.inflate(d.layoutInflater)
        d.setContentView(confirmationDialogBinding.root)
        d.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        d.setCancelable(false)
        d.show()
        confirmationDialogSelectedServices()

        val selectedServices = room.selectedServices().getAllSelectedService() as ArrayList<SelectedServicesEntity>

        var totalCharge = 0f
        var referApplicableCharge = 0f

        for (x in selectedServices) {
            totalCharge += x.serviceCost
            referApplicableCharge += if (x.isApplyReferIncome) x.serviceCost else 0f
        }

        val serviceChargeInPercent = 100f - appointmentChargePercentage

        val finalServiceCharge = getPercentage(serviceChargeInPercent, totalCharge)
        val appointmentCharge = totalCharge - finalServiceCharge

        val availableReferBonus = getReferBalance(mContext)
        val referralBonusCutOff = if (referApplicableCharge - availableReferBonus <= 0f) referApplicableCharge else availableReferBonus

        val totalAmountToPay = totalCharge - referralBonusCutOff

        if (referApplicableCharge == 0f) {
            confirmationDialogBinding.availableReferBonus.visibility = View.GONE
            confirmationDialogBinding.txtAvailableReferBonus.visibility = View.GONE
            confirmationDialogBinding.txtAppliedReferIncome.visibility = View.GONE
            confirmationDialogBinding.appliedReferBonus.visibility = View.GONE
        } else {
            confirmationDialogBinding.availableReferBonus.visibility = View.VISIBLE
            confirmationDialogBinding.txtAvailableReferBonus.visibility = View.VISIBLE
            confirmationDialogBinding.txtAppliedReferIncome.visibility = View.VISIBLE
            confirmationDialogBinding.appliedReferBonus.visibility = View.VISIBLE
        }

        confirmationDialogBinding.salonName.text = salonName
        confirmationDialogBinding.servicecost.text = finalServiceCharge.toString().plus("₹")
        confirmationDialogBinding.appliedReferBonus.text = referralBonusCutOff.toString().plus("₹")
        confirmationDialogBinding.availableReferBonus.text = getReferBalance(mContext).toString().plus("₹")
        confirmationDialogBinding.date.text = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(date))
        confirmationDialogBinding.time.text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(date))
        confirmationDialogBinding.availablebalance.text = getCurrentBalance(mContext).toString().plus("₹")
        confirmationDialogBinding.appointmentcharge.text = appointmentCharge.toString().plus("₹")
        confirmationDialogBinding.subTotal.text = "Total: ".plus(totalCharge.toString().plus("₹"))
        confirmationDialogBinding.totalamount.text = totalAmountToPay.toString().plus("₹")


        if (totalAmountToPay > getCurrentBalance(mContext)) {
            confirmationDialogBinding.alertLayout.visibility = View.VISIBLE
        }

        confirmationDialogBinding.confirmAppointment.setOnClickListener {
            if (totalAmountToPay > getCurrentBalance(mContext)) {
                Snackbar.make(confirmationDialogBinding.root, "Your wallet balance is low. Please add balance", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDialogLoading(true)

            setAppointment(finalServiceCharge, totalCharge ,
                totalAmountToPay, date, referralBonusCutOff, appointmentCharge)
        }
        confirmationDialogBinding.addbalance.setOnClickListener {
            startActivity(Intent(mContext, PaymentActivity::class.java))
        }
        confirmationDialogBinding.close.setOnClickListener {
            d.dismiss()
        }
    }

    private fun confirmationDialogSelectedServices() {

        confirmationDialogBinding.rvServices.adapter = AdapterServicesConfirmationDialog(room.selectedServices().getAllSelectedService() as ArrayList<SelectedServicesEntity>)

    }

    private fun showDialogLoading(isShow: Boolean) {
        if (isShow) {
            confirmationDialogBinding.pb.visibility = View.VISIBLE
            confirmationDialogBinding.confirmAppointment.text = ""
            confirmationDialogBinding.confirmAppointment.icon = null
            confirmationDialogBinding.confirmAppointment.isEnabled = false
        } else {
            confirmationDialogBinding.pb.visibility   = View.GONE
            confirmationDialogBinding.confirmAppointment.text = "Confirm Appointment"
            confirmationDialogBinding.confirmAppointment.icon = ContextCompat.getDrawable(mContext,R.drawable.confirmation_24)
            confirmationDialogBinding.confirmAppointment.isEnabled = true
        }
    }

    private fun setAppointment(serviceCost: Float, totalCharge : Float, totalAmountToPay : Float, date: Long, referralBonusCutOff: Float, appointmentCharge : Float) {

        val selectedServices = ArrayList<String>()
        val selectedServiceInRoom = room.selectedServices().getAllSelectedService() as ArrayList<SelectedServicesEntity>

        for (x in selectedServiceInRoom) {
            val s = x.serviceName + "?" + x.serviceId + "?" + x.serviceCost
            selectedServices.add(s)
        }

        val map = HashMap<String, Any>()
        map[APPOINTMENT_CHARGE] = appointmentCharge
        map[SERVICE_COST] = serviceCost
        map[TOTAL_AMOUNT] = totalCharge
        map[TOTAL_AMOUNT_PAID] = totalAmountToPay
        map[DATE_TIME] = date
        map[APPLIED_REFER_BALANCE] = referralBonusCutOff
        map[SERVICES] = selectedServices
        map[SALON_ID] = salonId
        map[RESPONSE] = "Appointment request sent successfully"
        map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
        map[USER_ID] = mUid(mContext)
        map[APPOINTMENT_STATUS] = APPOINTMENT_STATUS_PENDING
        val pInfo: PackageInfo =
            (activity as AppCompatActivity).packageManager.getPackageInfo((activity as AppCompatActivity).packageName, 0)
        val version = pInfo.versionName
        map[AppConstaints.APP_VERSION] = version
        map[AppConstaints.IP_ADDRESS] = try {
            Utils.getIpAddress()
        } catch (e : Exception) { "Not Available" }

        db.collection(COLLECTION_APPOINTMENTS)
            .document()
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                setCurrentBalance(mContext, getCurrentBalance(mContext) - totalAmountToPay)
                Utils.countReferRewards(mContext)
                Executors.newSingleThreadExecutor().execute {
                    countBalance(mContext)
                }
                Notification.SendNotificationToTopic(
                    "New Appointment",
                    getUserName(mContext)+" want to schedule an appointment in $salonName",
                    salonId,
                    activity
                )
                d.dismiss()
                appointmentSuccessfulDialog()
            }
            .addOnFailureListener {
                showDialogLoading(false)
                d.dismiss()
                Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
            }
    }

    private fun appointmentSuccessfulDialog() {

        val asDialog = Dialog(mContext)
        val asBinding = DialogAppointmentSuccessfulBinding.inflate(asDialog.layoutInflater)
        asDialog.setContentView(asBinding.root)
        asDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        asDialog.setCancelable(false)
        asDialog.show()

        asBinding.done.setOnClickListener {
            asDialog.dismiss()
            findNavController().popBackStack(R.id.bookAppointment, true)
        }

    }

    private fun actions() {

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.bookAppointment, true)
        }

        binding.calendar.minDate = System.currentTimeMillis()

        binding.calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val ft = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            val dx = ft.parse("$year/${month+1}/$dayOfMonth").time
            binding.calendar.date = dx
        }

        binding.pay.setOnClickListener {

            val selectedServices = room.selectedServices().getAllSelectedService() as ArrayList<SelectedServicesEntity>

            if (selectedServices.isEmpty()) {
                Snackbar.make(binding.root, "Please choose a service", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dx = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(binding.calendar.date))

            val myDate = "$dx ${binding.timePicker1.hour}:${binding.timePicker1.minute}"
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            val date = sdf.parse(myDate)
            val selectedTimeInMillis = date.time

            val pickedTime = binding.timePicker1.hour * 60 + binding.timePicker1.minute

            if (pickedTime !in salonOpenTiming..salonCloseTiming) {
                timingErrorDialog.show()
                return@setOnClickListener
            }

            if (selectedTimeInMillis < System.currentTimeMillis()) {
                Snackbar.make(binding.root, "Choose correct time", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            confirmationDialog(selectedTimeInMillis)
        }
    }

    private fun timingErrorDialog() {
         timingErrorDialog = Dialog(mContext)
         timingErrorBinding = DialogTimingErrorMessageBinding.inflate(timingErrorDialog.layoutInflater)
         timingErrorDialog.setContentView(timingErrorBinding.root)
         timingErrorDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
         timingErrorBinding.done.setOnClickListener {
             timingErrorDialog.dismiss()
         }
    }

    private fun loadServices(services: ArrayList<String>) {

        allServiceDialog = BottomSheetDialog(mContext)
        allServiceDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val services1 = ArrayList<ModelSplitService>()

        for (i in 0 until services.size) {
            val s = services[i].split('?')
            services1.add(
                ModelSplitService(
                    try {
                        s[0]
                    } catch (e: Exception) {
                        "Not Found"
                    }, try {
                        s[1]
                    } catch (e: Exception) {
                        "Not Found"
                    },
                    try {
                        s[2]
                    } catch (e: Exception) {
                        "0"
                    }
                )
            )
        }

        val serviceList = ArrayList<SelectedServicesEntity>()

        val adapterServices = AdapterServices(serviceList)

        for (i in 0 until services1.size) {
            db.collection(COLLECTION_SERVICES)
                .document(services1[i].serviceId)
                .get().addOnSuccessListener { d ->
                    try {
                        if (d.exists()) {
                            serviceList.add(
                                SelectedServicesEntity(
                                    d.id,
                                    services1[i].price.toFloat(),
                                    d[SERVICE_NAME].toString(),
                                    d[IMAGE_URL].toString(),
                                    try {
                                        d[APPLY_REFERRAL_INCOME] as Boolean
                                    } catch (e: Exception) { false }
                                )
                            )
                            adapterServices.notifyItemInserted(serviceList.size - 1)

                            // Pre Selected Service putting into roomDB

                            if (d.id == selectedServiceId) {
                                CoroutineScope(IO)
                                    .launch {
                                        try {
                                            room.selectedServices().insertService(
                                                SelectedServicesEntity(
                                                    d.id,
                                                    services1[i].price.toFloat(),
                                                    d[SERVICE_NAME].toString(),
                                                    d[IMAGE_URL].toString(),
                                                    try {
                                                        d[APPLY_REFERRAL_INCOME] as Boolean
                                                    } catch (e: Exception) { false }
                                                )
                                            )
                                        } catch (e: Exception) {
                                            room.selectedServices().updateService(
                                                SelectedServicesEntity(
                                                    d.id,
                                                    services1[i].price.toFloat(),
                                                    d[SERVICE_NAME].toString(),
                                                    d[IMAGE_URL].toString(),
                                                    try {
                                                        d[APPLY_REFERRAL_INCOME] as Boolean
                                                    } catch (e: Exception) { false }
                                                )
                                            )
                                        }
                                    }
                            }

                        }
                    } catch (e: Exception) {
                    }
                }
        }

        binding.rvServices.adapter = adapterServices
        
        selectedServices()

    }

    private fun selectedServices() {

        room.selectedServices().getAllSelectedServiceLive().observe(viewLifecycleOwner
        ) {
            binding.rvSelectedServices.adapter = AdapterSelectedServices(it as ArrayList<SelectedServicesEntity>)
        }

    }

    inner class AdapterServices(private val list: ArrayList<SelectedServicesEntity>) :
        RecyclerView.Adapter<AdapterServices.ServiceHolder>() {

        inner class ServiceHolder(
            itemView: View,
            val rowItemSalonInsideHorizontalBinding: RowItemServicesAppointmentsBinding
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemSalonInsideHorizontalBinding =
                RowItemServicesAppointmentsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ServiceHolder(
                rowItemSalonInsideHorizontalBinding.root,
                rowItemSalonInsideHorizontalBinding
            )
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {

            holder.rowItemSalonInsideHorizontalBinding.serviceName.text = list[position].serviceName
            holder.rowItemSalonInsideHorizontalBinding.price.text = "₹ ${list[position].serviceCost}"
            holder.rowItemSalonInsideHorizontalBinding.imageView.load(list[position].imageUrl)

            holder.rowItemSalonInsideHorizontalBinding.imgReferIncomeApplied.visibility = if (list[position].isApplyReferIncome)  View.VISIBLE else View.GONE

            holder.rowItemSalonInsideHorizontalBinding.imgReferIncomeApplied.setOnClickListener {
                Tooltip.Builder(it)
                    .setCornerRadius(5f)
                    .setBackgroundColor(Color.parseColor(COLOR_SECONDARY_1))
                    .setTextColor(Color.WHITE)
                    .setText("Referral Bonus Applied")
                    .show()
            }

            val service  = room.selectedServices().getServiceById(list[position].serviceId)
            var isSelected: Boolean

            if (service.isNotEmpty()){
                isSelected = true
                holder.rowItemSalonInsideHorizontalBinding.card.strokeWidth = 5
            } else {
                isSelected = false
                holder.rowItemSalonInsideHorizontalBinding.card.strokeWidth = 0
            }

            holder.itemView.setOnClickListener {
                if (!isSelected) {
                    CoroutineScope(IO)
                        .launch {
                            try {
                                room.selectedServices().insertService(
                                    list[position]
                                )
                            } catch (e: Exception) {
                                room.selectedServices().updateService(
                                    list[position]
                                )
                            }
                        }
                    holder.rowItemSalonInsideHorizontalBinding.card.strokeWidth = 5
                    isSelected = !isSelected
                    return@setOnClickListener
                }
                CoroutineScope(IO)
                    .launch {
                        room.selectedServices().delete(list[position].serviceId)
                    }
                holder.rowItemSalonInsideHorizontalBinding.card.strokeWidth = 0
                isSelected = !isSelected
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private inner class AdapterServicesConfirmationDialog(private val list: ArrayList<SelectedServicesEntity>) :
        RecyclerView.Adapter<AdapterServicesConfirmationDialog.ServiceHolder>() {

        inner class ServiceHolder(
            itemView: View,
            val rowItemSalonInsideHorizontalBinding: RowItemServicesAppointmentsBinding
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemSalonInsideHorizontalBinding =
                RowItemServicesAppointmentsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ServiceHolder(
                rowItemSalonInsideHorizontalBinding.root,
                rowItemSalonInsideHorizontalBinding
            )
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {

            holder.rowItemSalonInsideHorizontalBinding.serviceName.text = list[position].serviceName
            holder.rowItemSalonInsideHorizontalBinding.price.text = "₹ ${list[position].serviceCost}"
            holder.rowItemSalonInsideHorizontalBinding.imageView.load(list[position].imageUrl)

            holder.rowItemSalonInsideHorizontalBinding.imgReferIncomeApplied.visibility = if (list[position].isApplyReferIncome)  View.VISIBLE else View.GONE

            holder.rowItemSalonInsideHorizontalBinding.imgReferIncomeApplied.setOnClickListener {
                Tooltip.Builder(it)
                    .setCornerRadius(5f)
                    .setBackgroundColor(Color.parseColor(COLOR_SECONDARY_1))
                    .setTextColor(Color.WHITE)
                    .setText("Referral Bonus Applied")
                    .show()
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private inner class ModelSplitService(
        val serviceName: String,
        val serviceId: String,
        val price: String
    )

    private inner class AdapterSelectedServices(private val list: ArrayList<SelectedServicesEntity>) :
        RecyclerView.Adapter<AdapterSelectedServices.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val dBinding : RowItemSelectedServicesListBookAppointmentBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterSelectedServices.ServiceHolder {
            val dBinding = RowItemSelectedServicesListBookAppointmentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(
            holder: AdapterSelectedServices.ServiceHolder,
            position: Int
        ) {
            holder.dBinding.servicePrice.text = "₹".plus(list[position].serviceCost)
            holder.dBinding.serviceName.text = list[position].serviceName
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

}