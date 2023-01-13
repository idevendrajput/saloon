package com.dr.saloon.fragments.auth

import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentInformationBinding 
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.APP_VERSION
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS_DONE
import com.dr.saloon.utils.AppConstaints.BALANCE
import com.dr.saloon.utils.AppConstaints.CONTROL_COLLECTION
import com.dr.saloon.utils.AppConstaints.EMAIL
import com.dr.saloon.utils.AppConstaints.ENTER_EMAIL_ERROR
import com.dr.saloon.utils.AppConstaints.ENTER_NAME_ERROR
import com.dr.saloon.utils.AppConstaints.ERROR_INVALID_EMAIL
import com.dr.saloon.utils.AppConstaints.FEMALE
import com.dr.saloon.utils.AppConstaints.FIELD_USER_CODE
import com.dr.saloon.utils.AppConstaints.GENDER
import com.dr.saloon.utils.AppConstaints.MALE
import com.dr.saloon.utils.AppConstaints.PHONE
import com.dr.saloon.utils.AppConstaints.PROFILE_URL
import com.dr.saloon.utils.AppConstaints.REFER_VALUE
import com.dr.saloon.utils.AppConstaints.SOMETHING_WENT_WRONG
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.UPLINK_UID
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.dr.saloon.utils.AppConstaints.UTILS_DOCUMENT
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getRandomInt
import com.dr.saloon.utils.Utils.Companion.mUid
import com.dr.saloon.utils.Utils.Companion.setAuthStatus
import com.dr.saloon.utils.Utils.Companion.setCurrentBalance
import com.dr.saloon.utils.Utils.Companion.setEmail
import com.dr.saloon.utils.Utils.Companion.setGender
import com.dr.saloon.utils.Utils.Companion.setNotificationOn
import com.dr.saloon.utils.Utils.Companion.setProfileUrl
import com.dr.saloon.utils.Utils.Companion.setUserName
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException

class Informations : Fragment() {
    
    lateinit var binding : FragmentInformationBinding
    private val db = FirebaseFirestore.getInstance()
    lateinit var mContext : Context
    private var imageUri: Uri? = null
    private lateinit var imageUrl : String
    private var gender = MALE
    private lateinit var phoneNumber: String
    private var uplinkUid = ""

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
        binding.imageView.setImageURI(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInformationBinding.inflate(layoutInflater)
        phoneNumber = mUid(mContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions()
        setTypeFace()
        changeProfiles()
    }

//    @Throws(IOException::class)
//    fun getDownsizedImageBytes(
//        fullBitmap: Bitmap?,
//        scaleWidth: Int,
//        scaleHeight: Int
//    ): ByteArray  {
//        val scaledBitmap =
//            Bitmap.createScaledBitmap(fullBitmap!!, scaleWidth, scaleHeight, true)
//
//        // 2. Instantiate the downsized image content as a byte[]
//        val baos = ByteArrayOutputStream()
//        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
//        return baos.toByteArray()
//    }

    private fun actions() {

        binding.camera.setOnClickListener {
            Dexter.withContext(mContext)
                .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        getContent.launch("image/*")
                    }
                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                    ) {}
                }).check()
        }

        binding.imageView.setOnClickListener {
            Dexter.withContext(mContext)
                .withPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        getContent.launch("image/*")
                    }
                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                    }
                    override fun onPermissionRationaleShouldBeShown(
                        p0: PermissionRequest?,
                        p1: PermissionToken?
                    ) {}
                }).check()
        }

        binding.add.setOnClickListener {

            if (binding.etFullName.text.toString().trim().isEmpty()) {
                binding.etFullName.error = ENTER_NAME_ERROR
                return@setOnClickListener
            }

            if (binding.etemail.text.toString().trim().isEmpty()) {
                binding.etemail.error = ENTER_EMAIL_ERROR
                return@setOnClickListener
            }

            if (!binding.etemail.text.toString().trim().contains("@")){
                binding.etemail.error = ERROR_INVALID_EMAIL
                return@setOnClickListener
            }

            imageUrl = "https://firebasestorage.googleapis.com/v0/b/vella-salon.appspot.com/o/user_male.png?alt=media&token=74dca5ac-004b-41ac-9203-bc98f05813a5"
            if (binding.female.isChecked){
                gender = FEMALE
                imageUrl = "https://firebasestorage.googleapis.com/v0/b/vella-salon.appspot.com/o/user_female.png?alt=media&token=68df6751-aaa3-4358-a5db-a7fb4deb613d"
            }

            showLoading(true)

            if (binding.etreferralcode.text.toString().isNotEmpty()) {
                db.collection(USER_COLLECTION)
                    .document(binding.etreferralcode.text.toString())
                    .get().addOnSuccessListener {
                        if (!it.exists()) {
                            binding.etreferralcode.error = "Please Enter a Valid Referral Code"
                            showLoading(false)
                            return@addOnSuccessListener
                        }
                        uplinkUid = it.id
                        if (imageUri == null) {
                            putData()
                        } else {
                            uploadImage()
                        }
                    }
                return@setOnClickListener
            }

            if (imageUri == null) {
                putData()
            } else {
                uploadImage()
            }
        }
    }

    private fun uploadImage() {

        try {

            val reference = FirebaseStorage.getInstance().reference

            val file = imageUri as Uri
            val riversRef = reference.child("images/${file.lastPathSegment}")
            val uploadTask = riversRef.putFile(file)

            uploadTask.continueWithTask { task->

                if (!task.isSuccessful) {
                    throw task.exception!!
                }

                riversRef.downloadUrl.addOnCompleteListener { uploadTask ->
                    if (uploadTask.isSuccessful) {
                        imageUrl = uploadTask.result.toString()
                        putData()
                    } else {
                        showLoading(false)
                        Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                    }

                }
            }.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    showLoading(false)
                    Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                }
            }

        } catch (ioEx: IOException) {
            showLoading(false)
            Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
        }

    }

    private fun putData() {

        db.collection(CONTROL_COLLECTION)
            .document(UTILS_DOCUMENT)
            .get().addOnSuccessListener {

                val referValue = try { it[REFER_VALUE].toString().toFloat() } catch (e: Exception) { 0f }

                showLoading(true)
                setNotificationOn(mContext, true)

                val pInfo: PackageInfo =
                    (activity as AppCompatActivity).packageManager.getPackageInfo((activity as AppCompatActivity).packageName, 0)
                val version = pInfo.versionName

                val map = HashMap<String,Any>()
                map[USER_NAME] = binding.etFullName.text.toString().trim()
                map[EMAIL] = binding.etemail.text.toString().trim()
                map[PHONE] = phoneNumber
                map[PROFILE_URL] = imageUrl
                map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
                map[GENDER] = gender
                map[BALANCE] = 0f
                map[REFER_VALUE] = referValue
                map[UPLINK_UID] = uplinkUid
                map[APP_VERSION] = version
                map[AppConstaints.IP_ADDRESS] = try {
                    Utils.getIpAddress()
                } catch (e : Exception) { "Not Available" }
                map[AppConstaints.LAST_TIME_UPDATE] = FieldValue.serverTimestamp()

                db.collection(USER_COLLECTION)
                    .document(phoneNumber)
                    .set(map, SetOptions.merge())
                    .addOnSuccessListener {
                        try {
                            showLoading(false)
                            setUserName(mContext,  binding.etFullName.text.toString())
                            setEmail(mContext, binding.etemail.text.toString().trim())
                            setAuthStatus(mContext, AUTH_STATUS_DONE)
                            setProfileUrl(mContext, imageUrl)
                            setGender(mContext, gender)
                            setCurrentBalance(mContext, 0f)
                            findNavController().navigate(R.id.search)
                        } catch (e : Exception ) { }
                    }.addOnFailureListener {
                        showLoading(false)
                        Snackbar.make(binding.root, SOMETHING_WENT_WRONG, Snackbar.LENGTH_SHORT).show()
                    }

            }

    }

    private fun showLoading(isShow : Boolean) {
        if (isShow) {
            binding.pb.visibility = View.VISIBLE
            binding.add.text = ""
            binding.add.icon = null
            binding.add.isEnabled = false
        } else {
             binding.pb.visibility   = View.GONE
             binding.add.text = "Submit & Continue"
             binding.add.icon = ContextCompat.getDrawable(mContext,R.drawable.shield_check_24)
             binding.add.isEnabled = true
        }
    }

    private fun setTypeFace() {

        val typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
        binding.title.typeface = typeface
        binding.add.typeface = typeface
        binding.etemail.typeface = typeface
        binding.etFullName.typeface = typeface
        binding.female.typeface = typeface
        binding.male.typeface = typeface

    }

    private fun changeProfiles() {
        if (imageUri == null) {
            binding.male.setOnClickListener {
                if (binding.male.isChecked) {
                    binding.imageView.setImageResource(R.drawable.user_male)
                } else {
                    if (!binding.female.isChecked) {
                        binding.male.isChecked = true
                    }
                }
            }
            binding.female.setOnClickListener {
                if (binding.female.isChecked) {
                    binding.imageView.setImageResource(R.drawable.user_female)
                } else {
                    if (!binding.male.isChecked) {
                        binding.female.isChecked = true
                    }
                }
            }
        }
    }

}