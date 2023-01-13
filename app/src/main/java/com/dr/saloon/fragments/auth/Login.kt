package com.dr.saloon.fragments.auth

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.dr.saloon.R
import com.dr.saloon.database.LoginSliderEntity
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.databinding.DialogPhoneAuthBinding
import com.dr.saloon.databinding.FragmentLoginBinding
import com.dr.saloon.utils.AppConstaints.APP_VERSION
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS_DONE
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS_INFO
import com.dr.saloon.utils.AppConstaints.BALANCE
import com.dr.saloon.utils.AppConstaints.COLLECTION_LOGIN_SLIDER
import com.dr.saloon.utils.AppConstaints.EMAIL
import com.dr.saloon.utils.AppConstaints.ERROR_ENTER_PHONE
import com.dr.saloon.utils.AppConstaints.ERROR_ENTER_VERIFICATION_CODE
import com.dr.saloon.utils.AppConstaints.GENDER
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.IP_ADDRESS
import com.dr.saloon.utils.AppConstaints.LAST_TIME_UPDATE
import com.dr.saloon.utils.AppConstaints.LATITUDE
import com.dr.saloon.utils.AppConstaints.LONGITUDE
import com.dr.saloon.utils.AppConstaints.PHONE
import com.dr.saloon.utils.AppConstaints.PROFILE_URL
import com.dr.saloon.utils.AppConstaints.SLIDER_TAG
import com.dr.saloon.utils.AppConstaints.SOMETHING_WENT_WRONG
import com.dr.saloon.utils.AppConstaints.THE_TITLE
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.SharedPref.Companion.setDouble
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getIpAddress
import com.dr.saloon.utils.Utils.Companion.hideKeyBoard
import com.dr.saloon.utils.Utils.Companion.setCurrentBalance
import com.dr.saloon.utils.Utils.Companion.setNotificationOn
import com.dr.saloon.utils.Utils.Companion.setPlaceName
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Login : Fragment() {

    private lateinit var binding : FragmentLoginBinding
    private lateinit var mContext : Context
    private lateinit var verificationDialog : BottomSheetDialog
    private lateinit var phoneAuthDialogBinding : DialogPhoneAuthBinding
    private var sliderHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var viewPager2 : ViewPager2

    private var mVerificationId: String? = null
    private var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null
    private val db = FirebaseFirestore.getInstance()
    private var mAuth = Firebase.auth
    private lateinit var sliderRunnable : Runnable

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            sliderHandler.removeCallbacks(sliderRunnable)
        } catch (e: Exception) {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentTypeFace()
        slider()
        binding.login.setOnClickListener {
            binding.login.isEnabled = false
            verifyDialog()
        }

    }

    private fun verifyDialog() {

        verificationDialog = BottomSheetDialog(mContext)
        phoneAuthDialogBinding = DialogPhoneAuthBinding.inflate(verificationDialog.layoutInflater)
        verificationDialog.setContentView(phoneAuthDialogBinding.root)
        verificationDialog.setCancelable(false)
        verificationDialog.show()


        phoneAuthDialogBinding.close.setOnClickListener {
            binding.login.isEnabled = true
            verificationDialog.dismiss()
        }

        phoneAuthDialogBinding.phoneNumber.addTextChangedListener {
            if (phoneAuthDialogBinding.phoneNumber.text.toString().length >= 10){
                hideKeyBoard(phoneAuthDialogBinding.phoneNumber, mContext)
            }
        }

        phoneAuthDialogBinding.verificationCode.addTextChangedListener {
            if (phoneAuthDialogBinding.verificationCode.text.toString().length >= 6){
                hideKeyBoard(phoneAuthDialogBinding.verificationCode, mContext)
            }
        }

        phoneAuthDialogBinding.getVerificationCode.setOnClickListener {
            if (phoneAuthDialogBinding.phoneNumber.text.toString().trim().isEmpty()){
                phoneAuthDialogBinding.phoneNumber.error = "Enter phone number"
                return@setOnClickListener
            }
             if (phoneAuthDialogBinding.phoneNumber.text.toString().trim().length != 10){
                phoneAuthDialogBinding.phoneNumber.error = "Invalid phone number"
                return@setOnClickListener
            }
            showLoadingPhone(true)
            phoneAuthDialogBinding.phoneNumber.isFocusableInTouchMode = false
            phoneAuthDialogBinding.phoneNumber.isFocusable = false
            startPhoneNumberVerification("+91"+phoneAuthDialogBinding.phoneNumber.text.toString())
        }

        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                phoneAuthDialogBinding.verificationCode.setText(credential.smsCode.toString())
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Snackbar.make(binding.root," ${p0.message}", Snackbar.LENGTH_SHORT).show()
                showLoadingVerify(false)
            }

            override fun onCodeSent(verificationId: String, p1: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, p1)
                showLoadingVerify(false)
                updateDialogUi()

                mVerificationId = verificationId

                phoneAuthDialogBinding.verify.setOnClickListener {

                    if (phoneAuthDialogBinding.phoneNumber.text.toString().trim().isEmpty()) {
                        phoneAuthDialogBinding.phoneNumber.error = ERROR_ENTER_PHONE
                        return@setOnClickListener
                    }

                    if (phoneAuthDialogBinding.verificationCode.text.toString().trim().isEmpty()) {
                        phoneAuthDialogBinding.verificationCode.error = ERROR_ENTER_VERIFICATION_CODE
                        return@setOnClickListener
                    }

                    showLoadingVerify(true)

                    verifyPhoneNumberWithCode(mVerificationId!!,phoneAuthDialogBinding.verificationCode.text.toString())
                }
            }

            override fun onCodeAutoRetrievalTimeOut(p0: String) {
                super.onCodeAutoRetrievalTimeOut(p0)
            }
        }
//        phoneAuthDialogBinding.verify.setOnClickListener {
//            if (phoneAuthDialogBinding.verificationCode.text.toString().trim().isEmpty()){
//                phoneAuthDialogBinding.verificationCode.error = "Enter verification code"
//                return@setOnClickListener
//            }
//            showLoadingVerify(true)
//            // Verify OTP
//        }

    }

    private fun showLoadingPhone(isShow : Boolean) {
        if (isShow) {
            phoneAuthDialogBinding.phoneNumber.isFocusable = false
            phoneAuthDialogBinding.phoneNumber.isFocusableInTouchMode = false
            phoneAuthDialogBinding.getVerificationCode.icon = null
            phoneAuthDialogBinding.getVerificationCode.text = ""
            phoneAuthDialogBinding.getVerificationCode.isEnabled = false
            phoneAuthDialogBinding.pb1.visibility = View.VISIBLE
        } else {
            phoneAuthDialogBinding.phoneNumber.isFocusable = true
            phoneAuthDialogBinding.phoneNumber.isFocusableInTouchMode = true
            phoneAuthDialogBinding.getVerificationCode.icon = ContextCompat.getDrawable(mContext,
                R.drawable.key_24
            )
            phoneAuthDialogBinding.getVerificationCode.text = "Get Verification Code"
            phoneAuthDialogBinding.getVerificationCode.isEnabled = true
            phoneAuthDialogBinding.pb1.visibility = View.GONE
        }
    }

    private fun showLoadingVerify(isShow : Boolean) {
        if (isShow) {
            phoneAuthDialogBinding.verify.icon = null
            phoneAuthDialogBinding.verify.text = ""
            phoneAuthDialogBinding.verify.isEnabled = false
            phoneAuthDialogBinding.pb2.visibility = View.VISIBLE
        } else {
            phoneAuthDialogBinding.verify.icon = ContextCompat.getDrawable(mContext,
                R.drawable.shield_check_24
            )
            phoneAuthDialogBinding.verify.text = "Verify"
            phoneAuthDialogBinding.verify.isEnabled = true
            phoneAuthDialogBinding.pb2.visibility = View.GONE
        }
    }

    private fun updateDialogUi() {
         phoneAuthDialogBinding.getVerificationCode.visibility = View.GONE
         phoneAuthDialogBinding.parentPhone.visibility = View.GONE
         phoneAuthDialogBinding.pb1.visibility = View.GONE
         phoneAuthDialogBinding.txtPhoneNumber.text = "Verify"
         phoneAuthDialogBinding.verify.visibility = View.VISIBLE
         phoneAuthDialogBinding.parentOTP.visibility = View.VISIBLE
    }

    private fun slider()  {

        val room = MyDatabase.getDatabase(mContext)

        viewPager2 = binding.viewPager2

        val sliderItems = room.loginSliderDao().getAllSlider() as ArrayList<LoginSliderEntity>

        val adapter = SliderAdapter(sliderItems)

        if (sliderItems.isEmpty()) {
            db.collection(COLLECTION_LOGIN_SLIDER)
                .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
                .get().addOnSuccessListener {
                    try {
                        if (it.size() == 0){
                            return@addOnSuccessListener
                        }
                        for (d in it){
                            sliderItems.add(
                                LoginSliderEntity(
                                    d[THE_TITLE].toString(),
                                    d[SLIDER_TAG].toString(),
                                    d[IMAGE_URL].toString()
                                )
                            )
                            adapter.notifyItemInserted(sliderItems.size - 1)
                        }
                    } catch (e: Exception) { }
                }
         }

         try {
             viewPager2.adapter = adapter
             viewPager2.clipToPadding = false
             viewPager2.clipChildren = false
             viewPager2.offscreenPageLimit = 3
             viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

             TabLayoutMediator(binding.indicatorDots, viewPager2
             ) { _, _ -> }.attach()

             sliderRunnable = Runnable {
                 if (viewPager2.currentItem == sliderItems.size-1) {
                     viewPager2.setCurrentItem(0,true)
                     return@Runnable
                 }
                 viewPager2.currentItem = viewPager2.currentItem + 1

             }

             viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                 override fun onPageSelected(position: Int) {
                     super.onPageSelected(position)
                     sliderHandler.removeCallbacks(sliderRunnable)
                     sliderHandler.postDelayed(sliderRunnable, 2000)
                 }
             })
         } catch (e: Exception) {}

    }

    inner class SliderAdapter(private val sliderItems: ArrayList<LoginSliderEntity>) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
            return SliderViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.row_item_slider_login, parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
            val typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
            val title = holder.itemView.findViewById<TextView>(R.id.title)
            val tag = holder.itemView.findViewById<TextView>(R.id.tag)
            tag.typeface = typeface
            title.text = sliderItems[position].title
            tag.text = sliderItems[position].tag

            holder.itemView.findViewById<ImageView>(R.id.imageView).load(sliderItems[position].imageUrl)

        }

        override fun getItemCount(): Int {
            return sliderItems.size
        }

        inner class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
//
//        private val runnable = Runnable {
//            viewPager2.setCurrentItem(0,true)
//        }

    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        // [START start_phone_auth]
        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity((activity as AppCompatActivity)) // Activity (for callback binding)
            .setCallbacks(mCallbacks!!) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        // [END start_phone_auth]
    }

    private fun verifyPhoneNumberWithCode(verificationId: String, code: String) {
        // [START verify_with_code]
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {

        updateDialogUi()
        showLoadingVerify(true)

        mAuth.signInWithCredential(credential)
            .addOnSuccessListener {
                SharedPref.setData(mContext,PHONE,phoneAuthDialogBinding.phoneNumber.text.toString())
                pushData()
            }
            .addOnFailureListener {
                showLoadingPhone(false)
                showLoadingVerify(false)
                Snackbar.make(phoneAuthDialogBinding.root, SOMETHING_WENT_WRONG, Snackbar.LENGTH_SHORT)
                    .show()
            }
    }

    private fun pushData() {

        SharedPref.setData(mContext, AUTH_STATUS , AUTH_STATUS_INFO)
        SharedPref.setData(mContext, PHONE,phoneAuthDialogBinding.phoneNumber.text.toString())

        showLoadingVerify(true)

        db.collection(USER_COLLECTION)
            .document(phoneAuthDialogBinding.phoneNumber.text.toString().trim())
            .get().addOnSuccessListener { d->
                if (d.exists()) {
                    Utils.setUserName(mContext, d[USER_NAME].toString())
                    Utils.setEmail(mContext, d[EMAIL].toString())
                    Utils.setAuthStatus(mContext, AUTH_STATUS_DONE)
                    Utils.setProfileUrl(mContext, d[PROFILE_URL].toString())
                    Utils.setGender(mContext, d[GENDER].toString())
                    setCurrentBalance(mContext,if (d[BALANCE] != null) d[BALANCE].toString().toFloat() else 0f)

                    updateLoginInfo(d)
                } else {
                    SharedPref.setFloat(mContext, BALANCE,0.0f)
                    SharedPref.setData(mContext, AUTH_STATUS, AUTH_STATUS_INFO)
                    findNavController().navigate(R.id.informations)
                }
                verificationDialog.dismiss()
            }
    }

    private fun updateLoginInfo(d : DocumentSnapshot) {

        setNotificationOn(mContext, true)

        val pInfo: PackageInfo =
            (activity as AppCompatActivity).packageManager.getPackageInfo((activity as AppCompatActivity).packageName, 0)
        val version = pInfo.versionName

        val map = HashMap<String, Any>()
        map[APP_VERSION] = version
        map[IP_ADDRESS] = try { getIpAddress() } catch (e : Exception) { "Not Available" }
        map[LAST_TIME_UPDATE] = FieldValue.serverTimestamp()

        if (d[LATITUDE] != null && d[LONGITUDE] != null) {
            Executors.newSingleThreadExecutor().execute {
                setAddress(d[LATITUDE].toString().toDouble(), d[LONGITUDE].toString().toDouble())
            }
        }

        db.collection(USER_COLLECTION)
            .document(phoneAuthDialogBinding.phoneNumber.text.toString().trim())
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                if (d[LATITUDE] == null || d[LONGITUDE] == null) {
                    findNavController().navigate(R.id.search)
                    return@addOnSuccessListener
                }
                findNavController().navigate(R.id.home)
            }
    }

    private fun setAddress(lat: Double, lng: Double) {
        val geocoder = Geocoder( mContext, Locale.getDefault())
        var add = ""
        add = try {

            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val obj: Address = addresses[0]

            obj.locality

        } catch (e: IOException) {
            "Nearby me"
        }
        setPlaceName(mContext, add)
        setDouble(mContext, LATITUDE, lat)
        setDouble(mContext, LONGITUDE, lng)
    }

    private fun fragmentTypeFace() {
        val typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
        binding.login.typeface = typeface
    }

    inner class ModelSlider(val title : String, val tag : String, val image : Int)

}