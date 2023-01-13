package com.dr.saloon.utils

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import coil.load
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.dr.saloon.R
import com.dr.saloon.database.AppointmentEntity
import com.dr.saloon.database.LoginSliderEntity
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.database.WishlistEntity
import com.dr.saloon.databinding.DialogPopUpAdViewBinding
import com.dr.saloon.utils.AppConstaints.ABOUT_US
import com.dr.saloon.utils.AppConstaints.AMOUNT
import com.dr.saloon.utils.AppConstaints.APPLIED_REFER_BALANCE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_CHARGE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_CHARGE_COMMISSION
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CHARGE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_STATUS
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS
import com.dr.saloon.utils.AppConstaints.BALANCE
import com.dr.saloon.utils.AppConstaints.CASHFREE_API_KEY
import com.dr.saloon.utils.AppConstaints.CASHFREE_SECRET_KEY
import com.dr.saloon.utils.AppConstaints.COLLECTION_APPOINTMENTS
import com.dr.saloon.utils.AppConstaints.COLLECTION_LOGIN_SLIDER
import com.dr.saloon.utils.AppConstaints.COLLECTION_PAYMENT_ORDERS
import com.dr.saloon.utils.AppConstaints.CONTACT_US
import com.dr.saloon.utils.AppConstaints.DAILY_USE_LIMIT
import com.dr.saloon.utils.AppConstaints.EMAIL
import com.dr.saloon.utils.AppConstaints.GENDER
import com.dr.saloon.utils.AppConstaints.IS_DATA_INITIALIZED
import com.dr.saloon.utils.AppConstaints.IS_NOTIFICATION_ON
import com.dr.saloon.utils.AppConstaints.IS_TASK_EARNING_SETTLED
import com.dr.saloon.utils.AppConstaints.MY_LATITUDE
import com.dr.saloon.utils.AppConstaints.MY_LONGITUDE
import com.dr.saloon.utils.AppConstaints.ORDER_ID
import com.dr.saloon.utils.AppConstaints.PAYMENT_API
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS_SUCCESS
import com.dr.saloon.utils.AppConstaints.PHONE
import com.dr.saloon.utils.AppConstaints.PLACE_NAME
import com.dr.saloon.utils.AppConstaints.PRIVACY_POLICY
import com.dr.saloon.utils.AppConstaints.PROFILE_URL
import com.dr.saloon.utils.AppConstaints.RAZORPAY_API_KEY
import com.dr.saloon.utils.AppConstaints.RAZORPAY_SECRET_KEY
import com.dr.saloon.utils.AppConstaints.REFERRAL_BALANCE
import com.dr.saloon.utils.AppConstaints.REFER_POLICY
import com.dr.saloon.utils.AppConstaints.REFER_VALUE
import com.dr.saloon.utils.AppConstaints.REFUND_POLICY
import com.dr.saloon.utils.AppConstaints.RESPONSE
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.dr.saloon.utils.AppConstaints.SERVICE_ID
import com.dr.saloon.utils.AppConstaints.SHIPPING_POLICY
import com.dr.saloon.utils.AppConstaints.TERMS_AND_CONDITIONS
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.TOTAL_AMOUNT
import com.dr.saloon.utils.AppConstaints.TOTAL_AMOUNT_PAID
import com.dr.saloon.utils.AppConstaints.TOTAL_APPOINTMENTS
import com.dr.saloon.utils.AppConstaints.UPLINK_UID
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList

class Utils {

    companion object {

        private val calendar: Calendar = Calendar.getInstance()
        private val db = FirebaseFirestore.getInstance()
        private val KEY_ARTICLE_READS = "articleReads${getDateKey()}"

        fun getUserRef(mContext: Context) : DocumentReference {
            return db.collection(USER_COLLECTION).document(mUid(mContext))
        }

        fun formatTimer (sec : Long) : String {

            val s = String.format("%02d",sec%60)
            val m = String.format("%02d",(sec/60)%60)

            return "$m : $s"
        }

        fun mUid(mContext : Context) : String {
            return SharedPref.getData(mContext, PHONE).toString()
        }

        fun getAuthStatus(mContext: Context) : String {
            return SharedPref.getData(mContext,AUTH_STATUS).toString()
        }

        fun getCurrentBalance(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, BALANCE)
        }

        fun getTotalAppointments(mContext: Context) : Int {
            return SharedPref.getInt(mContext, TOTAL_APPOINTMENTS)
        }

        fun setCancellationCharge(mContext: Context, chargeInPercent : Float) {
            SharedPref.setFloat(mContext, APPOINTMENT_CANCELLATION_CHARGE, chargeInPercent)
        }

        fun getCancellationCharge(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, APPOINTMENT_CANCELLATION_CHARGE)
        }

        fun setCancellationChargeCommission(mContext: Context, commissionInPercent : Float) {
            SharedPref.setFloat(mContext, APPOINTMENT_CANCELLATION_CHARGE_COMMISSION, commissionInPercent)
        }

        fun getCancellationChargeCommission(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, APPOINTMENT_CANCELLATION_CHARGE_COMMISSION)
        }

        fun setCurrentBalance(mContext: Context, balance : Float) {
            SharedPref.setFloat(mContext, BALANCE, balance)
        }

        fun setReferPolicy(mContext: Context, referPolicy : String) {
            SharedPref.setData(mContext, REFER_POLICY, referPolicy)
        }

        fun getReferPolicy(mContext: Context) : String {
            return SharedPref.getData(mContext, REFER_POLICY).toString()
        }

        fun setReferBalance(mContext: Context, referBalance : Float) {
            SharedPref.setFloat(mContext, REFERRAL_BALANCE, referBalance)
        }

        fun setTotalAppointments(mContext: Context, totalAppointments : Int) {
            SharedPref.setInt(mContext, TOTAL_APPOINTMENTS, totalAppointments)
        }

        fun getCurrentBalanceInString(mContext: Context) : String {
            return String.format("%.2f",SharedPref.getFloat(mContext, BALANCE))
        }

        fun getReferBalance(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, REFERRAL_BALANCE)
        }

        fun setPlaceName(mContext: Context, place : String)  {
            SharedPref.setData(mContext, PLACE_NAME, place)
        }

        fun getPlaceName(mContext: Context) : String {
            return SharedPref.getData(mContext, PLACE_NAME).toString()
        }

        fun getFormattedFloatInString(value : Float, decimals : Int) : String {
            return String.format("%.$decimals"+"f",value)
        }

        fun getFormattedTimeWithHourAndMinute(minute: Int) : String {
            val m = minute%60
            val h = (minute - (minute%60)) / 60
            return "${String.format("%02d", if (h == 0) 12 else h)}:${String.format("%02d", m)}"
        }

        fun getArticleReadInSeconds(mContext: Context) : Int {
            return SharedPref.getInt(mContext, KEY_ARTICLE_READS)
        }

        fun updateArticleRead(mContext: Context,increment : Int) {
            SharedPref.setInt(mContext, KEY_ARTICLE_READS, SharedPref.getInt(mContext,
                KEY_ARTICLE_READS) + increment)
        }

        fun getCurrentMinuteOfDay(): Int {

            val now = Calendar.getInstance()
            val hour = now[Calendar.HOUR_OF_DAY]
            val minute = now[Calendar.MINUTE]

            return hour * 60 + minute

        }

        fun getCashFreeApi(mContext: Context) : String {
            return SharedPref.getData(mContext, CASHFREE_API_KEY).toString()
        }

        fun setCashFreeApi(mContext: Context, mc : String)  {
            SharedPref.setData(mContext, CASHFREE_API_KEY, mc)
        }

        fun getCashFreeSecret(mContext: Context) : String {
            return SharedPref.getData(mContext, CASHFREE_SECRET_KEY).toString()
        }

        fun setCashFreeSecret(mContext: Context, mc : String)  {
            SharedPref.setData(mContext, CASHFREE_SECRET_KEY, mc)
        }

        fun getDailyUsesLimit(mContext: Context) : Int {
            return  SharedPref.getInt(mContext, DAILY_USE_LIMIT)
        }

        fun getDateKey() : String {
            return "${calendar.get(Calendar.YEAR)}${calendar.get(Calendar.MONTH)}${calendar.get(Calendar.DAY_OF_MONTH)}"
        }

        fun setAuthStatus(mContext: Context, status : String)  {
            SharedPref.setData(mContext, AUTH_STATUS, status)
        }

        fun getPercentage(int : Int, percentage : Int) : Int {
            return int*percentage / 100
        }

        fun getPercentage(float : Float, percentage : Float) : Float {
            return float*percentage / 100f
        }

        fun getRandomInt(min : Int, max : Int) : Int {
            return ThreadLocalRandom.current().nextInt(min, max);
        }

        fun getPaymentAPI(mContext: Context) : String {
            return SharedPref.getData(mContext, PAYMENT_API).toString()
        }

        fun setPaymentAPI(mContext: Context, api : String)  {
           SharedPref.setData(mContext, PAYMENT_API,api)
        }

        fun isUplinkAvailable(mContext: Context) : Boolean {
            return SharedPref.getData(mContext, UPLINK_UID).toString().trim().isNotEmpty()
        }

        private fun isTaskEarningSettled(mContext: Context) : Boolean {
            return SharedPref.getBoolean(mContext, IS_TASK_EARNING_SETTLED)
        }

       private fun setTaskEarningSettled(mContext: Context)  {
           SharedPref.setBoolean(mContext, IS_TASK_EARNING_SETTLED, true)
        }

        fun setRazorpayAPIKey(mContext : Context, key : String) {
            SharedPref.setData(mContext, RAZORPAY_API_KEY, key)
        }

        fun getRazorpayAPIKey(mContext: Context) : String {
            return SharedPref.getData(mContext, RAZORPAY_API_KEY).toString()
        }

        fun setRazorpaySecretKey(mContext : Context, key : String) {
            SharedPref.setData(mContext, RAZORPAY_SECRET_KEY, key)
        }

        fun getRazorpaySecretKey(mContext: Context) : String {
            return SharedPref.getData(mContext, RAZORPAY_SECRET_KEY).toString()
        }

        // get UserInformation

        fun getUserName(mContext: Context) : String {
            return SharedPref.getData(mContext, USER_NAME).toString()
        }

        fun getEmail(mContext: Context) : String {
            return SharedPref.getData(mContext, EMAIL).toString()
        }

        fun getPhone(mContext: Context) : String {
            return SharedPref.getData(mContext, PHONE).toString()
        }

        fun getGender(mContext: Context) : String {
            return SharedPref.getData(mContext, GENDER).toString()
        }

        fun setUserName(mContext: Context, name : String)  {
            SharedPref.setData(mContext, USER_NAME, name)
        }

        fun setEmail(mContext: Context, email : String)   {
            SharedPref.setData(mContext, EMAIL, email)
        }

        fun setPhone(mContext: Context, phone : String)   {
            SharedPref.setData(mContext, PHONE, phone)
        }

        fun setGender(mContext: Context, gender : String)  {
            SharedPref.setData(mContext, GENDER, gender)
        }

        fun setProfileUrl(mContext: Context, profileUrl : String)  {
            SharedPref.setData(mContext, PROFILE_URL, profileUrl)
        }

        fun getProfileUrl(mContext: Context) : String {
            return SharedPref.getData(mContext, PROFILE_URL).toString()
        }

        fun setLatitude(mContext: Context, latitude: Float)  {
            SharedPref.setFloat(mContext, MY_LATITUDE, latitude)
        }

        fun getLatitude(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, MY_LATITUDE)
        }

        fun setLongitude(mContext: Context, latitude: Float)  {
            SharedPref.setFloat(mContext, MY_LONGITUDE, latitude)
        }

        fun getLongitude(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, MY_LONGITUDE)
        }

        fun logOut(mContext: Context) {
            SharedPref.deleteKey(mContext, AUTH_STATUS)
        }

        // Privacy

        fun getAboutUs(mContext: Context) : String {
            return SharedPref.getData(mContext, ABOUT_US).toString()
        }
        fun getContactUs(mContext: Context) : String {
            return SharedPref.getData(mContext, CONTACT_US).toString()
        }
        fun getPrivacyPolicy(mContext: Context) : String {
            return SharedPref.getData(mContext, PRIVACY_POLICY).toString()
        }
        fun getTermsAndConditions(mContext: Context) : String {
            return SharedPref.getData(mContext, TERMS_AND_CONDITIONS).toString()
        }
        fun getRefundPolicy(mContext: Context) : String {
            return SharedPref.getData(mContext, REFUND_POLICY).toString()
        }
        fun getShippingPolicy(mContext: Context) : String {
            return SharedPref.getData(mContext, SHIPPING_POLICY).toString()
        }

        fun setAboutUs(mContext: Context, data : String)   {
            SharedPref.setData(mContext, ABOUT_US, data).toString()
        }
        fun setContactUs(mContext: Context, data : String)  {
            SharedPref.setData(mContext, CONTACT_US, data).toString()
        }
        fun setPrivacyPolicy(mContext: Context, data : String)  {
            SharedPref.setData(mContext, PRIVACY_POLICY, data).toString()
        }

        fun setTermsAndConditions(mContext: Context, data : String)  {
            SharedPref.setData(mContext, TERMS_AND_CONDITIONS, data).toString()
        }

        fun setRefundPolicy(mContext: Context, data : String)  {
            SharedPref.setData(mContext, REFUND_POLICY, data).toString()
        }

        fun setShippingPolicy(mContext: Context, data : String)  {
            SharedPref.setData(mContext, SHIPPING_POLICY, data).toString()
        }

        fun setDataInitialized(mContext: Context) {
            SharedPref.setBoolean(mContext, IS_DATA_INITIALIZED, true)
        }

        fun isDataInitialized(mContext: Context) : Boolean {
           return SharedPref.getBoolean(mContext, IS_DATA_INITIALIZED)
        }

        fun hideKeyBoard(view : View, context: Context) : Boolean {
            try {
                val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                return inputMethodManager.hideSoftInputFromWindow(view.windowToken,0)
            } catch (e : RuntimeException) { }
            return false
        }

        fun changeHeightWithAnim(view : View, currentHeight : Int, newHeight : Int) {

            val heightAnimator = ValueAnimator.ofInt(currentHeight, newHeight).setDuration(500)

            heightAnimator.addUpdateListener { anim->
                val value = anim.animatedValue as Int
                view.layoutParams.height = value
                view.requestLayout()
            }

            val animSet = AnimatorSet()
            animSet.interpolator = AccelerateDecelerateInterpolator()
            animSet.play(heightAnimator)
            animSet.start()

        }

        fun collapseWidthWithAnim(view : View, currentWidth : Int, newWidth : Int) {

            val heightAnimator = ValueAnimator.ofInt(currentWidth, newWidth).setDuration(200)

            heightAnimator.addUpdateListener { anim->
                val value = anim.animatedValue as Int
                view.layoutParams.width = value
                view.requestLayout()
            }

            val animSet = AnimatorSet()
            animSet.interpolator = AccelerateDecelerateInterpolator()
            animSet.play(heightAnimator)
            animSet.start()

        }

        fun expendWidthWithAnim(view : View, currentWidth : Int, newWidth : Int) {

            val heightAnimator = ValueAnimator.ofInt(currentWidth, newWidth).setDuration(200)

            heightAnimator.addUpdateListener { anim->
                val value = anim.animatedValue as Int
                view.layoutParams.width = value
                view.requestLayout()
            }

            val animSet = AnimatorSet()
            animSet.interpolator = AccelerateDecelerateInterpolator()
            animSet.play(heightAnimator)
            animSet.start()
        }

        fun getIpAddress() : String {
            try {
                val interfaces: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs: List<InetAddress> = Collections.list(intf.getInetAddresses())
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr: String = addr.hostAddress
                            //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                            val isIPv4 = sAddr.indexOf(':') < 0
                            if (isIPv4) return sAddr
                        }
                    }
                }
            } catch (ignored: Exception) {
            } // for now eat exceptions

            return ""
        }
        
        fun setBalance(mContext: Context, balance: Float) {
            SharedPref.setFloat(mContext, BALANCE, balance)
        }

        fun getBalance(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, BALANCE)
        }

        fun isNotificationOn(mContext: Context) : Boolean {
           return SharedPref.getBoolean(mContext, IS_NOTIFICATION_ON)
        }

        fun setNotificationOn(mContext: Context, isOn : Boolean) {
            return SharedPref.setBoolean(mContext, IS_NOTIFICATION_ON, isOn)
        }

        fun setAppointmentCharge(mContext: Context, charge : Float) {
            SharedPref.setFloat(mContext, APPOINTMENT_CHARGE, charge)
        }

        fun getAppointmentCharge(mContext: Context, serviceCost : Float) : Float {
            return getPercentage(serviceCost, getAppointmentChargeInPercent(mContext))
        }

        fun getAppointmentChargeInPercent(mContext: Context) : Float {
            return SharedPref.getFloat(mContext, APPOINTMENT_CHARGE)
        }

        fun getScreenWidth(): Int {
            return Resources.getSystem().displayMetrics.widthPixels
        }

        fun getScreenHeight(): Int {
            return Resources.getSystem().displayMetrics.heightPixels
        }

        fun countBalance(mContext: Context) {

            db.collection(COLLECTION_PAYMENT_ORDERS)
                .whereEqualTo(USER_ID, mUid(mContext))
                .get().addOnSuccessListener {paymentsQuery->

                    db.collection(COLLECTION_APPOINTMENTS)
                        .whereEqualTo(USER_ID, mUid(mContext))
                        .get().addOnSuccessListener {appointmentQuery->

                            var totalAppointmentAmount = 0f
                            var totalRecharges = 0f

                            for (i in paymentsQuery) {
                                totalRecharges += if (i[PAYMENT_STATUS].toString() == PAYMENT_STATUS_SUCCESS) if(i[AMOUNT] != null) i[AMOUNT].toString().toFloat() else 0f else 0f
                            }

                            for (i in appointmentQuery) {
                                totalAppointmentAmount += if (i[APPOINTMENT_STATUS] != null) if (i[APPOINTMENT_STATUS].toString() != AppConstaints.APPOINTMENT_STATUS_CANCELLED) if(i[TOTAL_AMOUNT_PAID] != null) i[TOTAL_AMOUNT_PAID].toString().toFloat() else 0f else 0f else 0f
                                totalAppointmentAmount += if (i[APPOINTMENT_STATUS] != null) if (i[APPOINTMENT_STATUS].toString() == AppConstaints.APPOINTMENT_STATUS_CANCELLED) if(i[APPOINTMENT_CANCELLATION_CHARGE] != null) i[APPOINTMENT_CANCELLATION_CHARGE].toString().toFloat() else 0f else 0f else 0f
                                totalAppointmentAmount += if (i[APPOINTMENT_STATUS] != null) if (i[APPOINTMENT_STATUS].toString() == AppConstaints.APPOINTMENT_STATUS_CANCELLED) if(i[APPLIED_REFER_BALANCE] != null) i[APPLIED_REFER_BALANCE].toString().toFloat() else 0f else 0f else 0f
                            }

                            val balance = totalRecharges - totalAppointmentAmount

                            setCurrentBalance(mContext, balance)

                            val map = HashMap<String,Any>()
                            map[BALANCE] = balance

                            db.collection(USER_COLLECTION)
                                .document(mUid(mContext))[map] = SetOptions.merge()
                            loadAppointments(mContext)

                        }
                }
        }

        fun countReferRewards(mContext: Context) {

            db.collection(USER_COLLECTION)
                .whereEqualTo(UPLINK_UID, mUid(mContext))
                .get().addOnSuccessListener { r->
                    db.collection(COLLECTION_APPOINTMENTS)
                        .whereEqualTo(USER_ID, mUid(mContext))
                        .get().addOnSuccessListener { a->

                            var referRewards = 0f

                            for (i in r) {
                                referRewards += i[REFER_VALUE].toString().toFloat()
                            }

                            for (i in a) {
                                referRewards -= try { i[APPLIED_REFER_BALANCE].toString().toFloat() } catch (e: Exception) { 0f }
                            }

                            Log.d("--->", referRewards.toString())
                            setReferBalance(mContext, referRewards)

                        }
                }

        }

        private fun loadAppointments(mContext: Context) {

            val room = MyDatabase.getDatabase(mContext)

            db.collection(COLLECTION_APPOINTMENTS)
                .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .get().addOnSuccessListener {
                    for ((i,d) in it.withIndex()) {
                        try {

                            if (d[USER_ID].toString() == mUid(mContext)) {
                                CoroutineScope(IO)
                                    .launch {
                                        try {
                                            room.appointmentsDao().insertAppointment(
                                                AppointmentEntity(
                                                    d.getDate(TIMESTAMP_FIELD)?.time as Long,
                                                    if(d[SERVICES] != null) (d[SERVICES] as ArrayList<String>).toString() else "",
                                                    d[SALON_ID].toString(),
                                                    d[TOTAL_AMOUNT_PAID].toString().toFloat(),
                                                    d[TOTAL_AMOUNT].toString().toFloat(),
                                                    d[APPLIED_REFER_BALANCE].toString().toFloat(),
                                                    d[APPOINTMENT_STATUS].toString(),
                                                    d[RESPONSE].toString(),
                                                    d.id
                                                )
                                            )
                                        } catch (e : Exception) {
                                            try {
                                                room.appointmentsDao().updateAppointment(
                                                    AppointmentEntity(
                                                        d.getDate(TIMESTAMP_FIELD)?.time as Long,
                                                        if(d[SERVICES] != null) (d[SERVICES] as ArrayList<String>).toString() else "",
                                                        d[SALON_ID].toString(),
                                                        d[TOTAL_AMOUNT_PAID].toString().toFloat(),
                                                        d[TOTAL_AMOUNT].toString().toFloat(),
                                                        d[APPLIED_REFER_BALANCE].toString().toFloat(),
                                                        d[APPOINTMENT_STATUS].toString(),
                                                        d[RESPONSE].toString(),
                                                        d.id
                                                    )
                                                )
                                            } catch (e: Exception) { }
                                        } catch (e: Exception) { }
                                    }
                            }

                            if (i == it.size()-1){
                                checkPendingPayment(mContext)
                            }

                        } catch (e : Exception) { }
                    }
                }
        }

        private fun checkPendingPayment(mContext: Context) {

            db.collection(COLLECTION_PAYMENT_ORDERS)
                .whereEqualTo(USER_ID, mUid(mContext))
                .get().addOnSuccessListener {
                    for (d in it) {
                        try {
                            val orderId = d[ORDER_ID].toString()
                            if (d[AppConstaints.PAYMENT_METHOD] != null && d[PAYMENT_STATUS].toString() != AppConstaints.PAYMENT_STATUS_PENDING) {
                                checkCashFree(orderId, mContext)
                            }
                        } catch (e : Exception) { }
                    }
                }
        }

        private fun checkCashFree(orderId: String, mContext: Context) {

            val jsonObjectRequest = object : StringRequest(
                Method.POST,"https://api.cashfree.com/api/v1/order/info",
                Response.Listener {

                    try {

                        val obj = JSONObject(it)
                        val status = obj.get("orderStatus").toString()

                        if (status.equals("paid", ignoreCase = true)){

                            val map = HashMap<String, Any>()
                            map[PAYMENT_STATUS] = AppConstaints.PAYMENT_STATUS_SUCCESS
                            db.collection(COLLECTION_PAYMENT_ORDERS)
                                .document(orderId)[map] = SetOptions.merge()

                        } else {
                            db.collection(COLLECTION_PAYMENT_ORDERS)
                                .document(orderId).delete()
                        }

                    } catch (e : Exception) { }

                }, Response.ErrorListener { }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val credentials =   getCashFreeApi(mContext) + ":" + getCashFreeSecret(mContext)
                    val base64EncodedCredentials: String =
                        Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                    val headers: HashMap<String, String> = HashMap()
                    headers["Authorization"] = "Basic $base64EncodedCredentials"
                    headers["Content-Type"] = "application/x-www-form-urlencoded"
                    return headers
                }

                override fun getBodyContentType(): String {
                    return "application/json"
                }

                override fun getBody(): ByteArray {
                    val params = JSONObject()
                    params.put("appId",  getCashFreeApi(mContext))
                    params.put("secretKey", getCashFreeSecret(mContext))
                    params.put("orderId", orderId)
                    return params.toString().toByteArray(charset = Charsets.UTF_8)
                }
            }

            val queue = Volley.newRequestQueue(mContext)
            queue.add(jsonObjectRequest)
        }

        fun updateLoginSlider(mContext: Context) {

            val room = MyDatabase.getDatabase(mContext)

            db.collection(COLLECTION_LOGIN_SLIDER)
                .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .get().addOnSuccessListener {
                    if (it.size() == 0){
                        return@addOnSuccessListener
                    }

                    for (d in it) {
                        CoroutineScope(IO)
                            .launch {
                            try {
                                room.loginSliderDao().insertSlider(
                                    LoginSliderEntity(
                                        d[AppConstaints.THE_TITLE].toString(),
                                        d[AppConstaints.SLIDER_TAG].toString(),
                                        d[AppConstaints.IMAGE_URL].toString()
                                    )
                                )
                            } catch (e: Exception) {
                                room.loginSliderDao().updateSlider(
                                    LoginSliderEntity(
                                        d[AppConstaints.THE_TITLE].toString(),
                                        d[AppConstaints.SLIDER_TAG].toString(),
                                        d[AppConstaints.IMAGE_URL].toString()
                                    )
                                )
                            }

                        }
                    }

                }

        }

        fun updateWishList(mContext: Context) {

            val room = MyDatabase.getDatabase(mContext)

            db.collection(USER_COLLECTION)
                .document(mUid(mContext))
                .collection(AppConstaints.COLLECTION_WISH_LIST)
                .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .get().addOnSuccessListener {
                    if (it.size() == 0) {
                        return@addOnSuccessListener
                    }
                    for (d in it) {
                        try {
                            CoroutineScope(IO)
                                .launch {
                                try {
                                    room.wishlistDao().insertWishlist(
                                        WishlistEntity(
                                            (d.getDate(TIMESTAMP_FIELD) as Date).time,
                                            d[SALON_ID].toString(),
                                            d[SERVICE_ID].toString(),
                                            d[AppConstaints.DATA_TYPE].toString(),
                                            d.id
                                        )
                                    )
                                } catch (e: Exception) {
                                    room.wishlistDao().updateWishlist(
                                        WishlistEntity(
                                            (d.getDate(TIMESTAMP_FIELD) as Date).time,
                                            d[SALON_ID].toString(),
                                            d[SERVICE_ID].toString(),
                                            d[AppConstaints.DATA_TYPE].toString(),
                                            d.id
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
        }

        fun drawFavAnim(mContext: Context, view : ConstraintLayout, color : String) {

            try {

                val image = ImageView(mContext)
                image.setImageResource(R.drawable.ic_baseline_favorite_24)
                image.imageTintList = ColorStateList.valueOf(Color.parseColor(color))
                view.addView(image)

                val lp = image.layoutParams as ConstraintLayout.LayoutParams
                lp.height = 200
                lp.width = 200
                lp.startToStart = ConstraintSet.PARENT_ID
                lp.endToEnd = ConstraintSet.PARENT_ID
                lp.topToTop = ConstraintSet.PARENT_ID
                lp.bottomToBottom = ConstraintSet.PARENT_ID
                image.layoutParams = lp

                val animZoomIn = AnimationUtils.loadAnimation(mContext,
                    R.anim.zoom_in)
                image.startAnimation(animZoomIn)

                animZoomIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}

                    override fun onAnimationEnd(animation: Animation?) {
                        val animZoomOut = AnimationUtils.loadAnimation(mContext,
                            R.anim.zoom_out)
                        image.startAnimation(animZoomOut)
                        animZoomOut.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                android.os.Handler(Looper.getMainLooper())
                                    .postDelayed({
                                        view.removeView(image)
                                    }, 600)
                            }
                            override fun onAnimationRepeat(animation: Animation?) {}

                        })
                    }

                    override fun onAnimationRepeat(animation: Animation?) {}

                })

            } catch (e: Exception) {}

        }

        fun updateConstraintData(mContext: Context) {

            getUserRef(mContext)
                .get().addOnSuccessListener {d->
                    try {
                        val name = d[AppConstaints.USER_NAME].toString()
                        Utils.setUserName(mContext, name)

                        val email = d[AppConstaints.EMAIL].toString()
                        Utils.setEmail(mContext, email)

                        val phone = d[AppConstaints.PHONE].toString()
                        Utils.setPhone(mContext, phone)

                        val profileUrl = d[AppConstaints.PROFILE_URL].toString()
                        Utils.setProfileUrl(mContext, profileUrl)

                        val gender = d[AppConstaints.GENDER].toString()
                        Utils.setGender(mContext, gender)

                        val balance = if (d[BALANCE] != null) d[BALANCE].toString().toFloat() else 0f
                        setBalance(mContext, balance)

                    } catch (e : Exception) { }
                }

            db.collection(COLLECTION_APPOINTMENTS)
                .whereEqualTo(USER_ID, mUid(mContext))
                .get().addOnSuccessListener {
                    setTotalAppointments(mContext, it.size())
                }

            db.collection(AppConstaints.CONTROL_COLLECTION)
                .document(AppConstaints.UTILS_DOCUMENT)
                .get().addOnSuccessListener {

                     setAboutUs(mContext, it[ABOUT_US].toString())
                     setContactUs(mContext, it[CONTACT_US].toString())
                     setPrivacyPolicy(mContext, it[PRIVACY_POLICY].toString())
                     setTermsAndConditions(
                        mContext,
                        it[TERMS_AND_CONDITIONS].toString()
                    )

                    if (getAuthStatus(mContext) == AppConstaints.AUTH_STATUS_DONE) {
                        if (it[AppConstaints.POP_UP_AD] != null && !SharedPref.getBoolean(mContext, "Ad${getDateKey()}")) {
                            popUpAd(it[AppConstaints.POP_UP_AD].toString(), mContext)
                        }
                    }

                    setReferPolicy(mContext, it[REFER_POLICY].toString())
                    setCancellationChargeCommission(mContext, it[APPOINTMENT_CANCELLATION_CHARGE_COMMISSION].toString().toFloat())
                    setCancellationCharge(mContext, it[APPOINTMENT_CANCELLATION_CHARGE].toString().toFloat())
                    setAppointmentCharge(mContext, it[APPOINTMENT_CHARGE].toString().toFloat())
                    setRefundPolicy(mContext, it[REFUND_POLICY].toString())
                    setShippingPolicy(mContext, it[SHIPPING_POLICY].toString())
                    setCashFreeApi(mContext,it[CASHFREE_API_KEY].toString())
                    setCashFreeSecret(mContext, it[CASHFREE_SECRET_KEY].toString())
//                    val razorpayKey = it[RAZORPAY_API_KEY].toString()
//                    val keySecret = it[RAZORPAY_SECRET_KEY].toString()
//                    setRazorpayAPIKey(this, razorpayKey)
//                    setRazorpaySecretKey(this, keySecret)
                    try {

                    } catch (e : Exception){}
                }
        }

        private fun popUpAd(imageUrl: String, mContext: Context) {

            val popUpDialog = Dialog(mContext)
            val popUpBinding = DialogPopUpAdViewBinding.inflate(popUpDialog.layoutInflater)
            popUpDialog.setContentView(popUpBinding.root)
            popUpDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            popUpDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            popUpBinding.imageView.load(imageUrl)

            popUpBinding.close.setOnClickListener {
                SharedPref.setBoolean(mContext, "Ad${getDateKey()}", true)
                popUpDialog.dismiss()
            }
            popUpDialog.show()

        }

        fun stringToArrayList(input: String): ArrayList<String> {
            var list = ArrayList<String>()
            try {
                list = ArrayList(input.replace("[", "").replace("]", "")
                .split(','))
            } catch (e: Exception) { }
            return list
        }

    }

}