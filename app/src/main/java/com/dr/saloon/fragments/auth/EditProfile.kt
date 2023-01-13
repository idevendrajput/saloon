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
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentEditProfileBinding 
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.EMAIL
import com.dr.saloon.utils.AppConstaints.PROFILE_URL
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getEmail
import com.dr.saloon.utils.Utils.Companion.getProfileUrl
import com.dr.saloon.utils.Utils.Companion.getUserName
import com.dr.saloon.utils.Utils.Companion.mUid
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
import java.io.ByteArrayOutputStream
import java.io.IOException

class EditProfile : Fragment() {

    lateinit var binding : FragmentEditProfileBinding
    private val db = FirebaseFirestore.getInstance()
    lateinit var mContext : Context
    private var imageUri: Uri? = null
    private var imageUrl : String = ""

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
        binding = FragmentEditProfileBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions()
        setTypeFace()
        loadPreviousData()
    }

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
                binding.etFullName.error = AppConstaints.ENTER_NAME_ERROR
                return@setOnClickListener
            }
            if (binding.etemail.text.toString().trim().isEmpty()) {
                binding.etemail.error = AppConstaints.ENTER_EMAIL_ERROR
                return@setOnClickListener
            }
            if (!binding.etemail.text.toString().trim().contains("@")){
                binding.etemail.error = AppConstaints.ERROR_INVALID_EMAIL
                return@setOnClickListener
            }

            showLoading(true)

            if (imageUri == null) {
                putData()
            } else {
                uploadImage()
            }
        }

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.editProfile, true)
        }
    }

    private fun loadPreviousData() {

        try {
            binding.etFullName.setText(getUserName(mContext))
            binding.etemail.setText(getEmail(mContext))
            binding.imageView.load(getProfileUrl(mContext))
        } catch (e :Exception) {}

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
                        Toast.makeText(mContext, AppConstaints.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                    }

                }
            }.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    showLoading(false)
                    Toast.makeText(mContext, AppConstaints.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                }
            }

        } catch (ioEx: IOException) {
            showLoading(false)
            Toast.makeText(mContext, AppConstaints.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
        }
    }

    private fun putData() {

        showLoading(true)
        Utils.setNotificationOn(mContext, true)

        val pInfo: PackageInfo =
            (activity as AppCompatActivity).packageManager.getPackageInfo((activity as AppCompatActivity).packageName, 0)
        val version = pInfo.versionName

        Utils.setProfileUrl(mContext, imageUrl.ifEmpty { getProfileUrl(mContext) } )

        val map = HashMap<String,Any>()
        map[USER_NAME] = binding.etFullName.text.toString().trim()
        map[EMAIL] = binding.etemail.text.toString().trim()
        map[PROFILE_URL] = imageUrl.ifEmpty { getProfileUrl(mContext) }
        map[AppConstaints.APP_VERSION] = version
        map[AppConstaints.IP_ADDRESS] = try {
            Utils.getIpAddress()
        } catch (e : Exception) { "Not Available" }
        map[AppConstaints.LAST_TIME_UPDATE] = FieldValue.serverTimestamp()

        db.collection(USER_COLLECTION)
            .document(mUid(mContext))
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                try {
                    showLoading(false)
                    Utils.setUserName(mContext, binding.etFullName.text.toString())
                    Utils.setEmail(mContext, binding.etemail.text.toString().trim())
                    findNavController().navigate(R.id.home)
                } catch (e : Exception ) { }
            }.addOnFailureListener {
                showLoading(false)
                Snackbar.make(binding.root,
                    AppConstaints.SOMETHING_WENT_WRONG, Snackbar.LENGTH_SHORT).show()
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
            binding.add.text = "Done & Continue"
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
    }

}