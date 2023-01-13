package com.dr.saloon

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.dr.saloon.databinding.ActivityMainBinding
import com.dr.saloon.databinding.DialogPopUpAdViewBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_CHARGE
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CANCELLATION_CHARGE_COMMISSION
import com.dr.saloon.utils.AppConstaints.APPOINTMENT_CHARGE
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS_DONE
import com.dr.saloon.utils.AppConstaints.BALANCE
import com.dr.saloon.utils.AppConstaints.CASHFREE_API_KEY
import com.dr.saloon.utils.AppConstaints.CASHFREE_SECRET_KEY
import com.dr.saloon.utils.AppConstaints.COLLECTION_APPOINTMENTS
import com.dr.saloon.utils.AppConstaints.INTENT_TYPE
import com.dr.saloon.utils.AppConstaints.INTENT_TYPE_NOTIFICATION
import com.dr.saloon.utils.AppConstaints.POP_UP_AD
import com.dr.saloon.utils.AppConstaints.REFER_POLICY
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.countBalance
import com.dr.saloon.utils.Utils.Companion.countReferRewards
import com.dr.saloon.utils.Utils.Companion.getAuthStatus
import com.dr.saloon.utils.Utils.Companion.getDateKey
import com.dr.saloon.utils.Utils.Companion.getUserRef
import com.dr.saloon.utils.Utils.Companion.isNotificationOn
import com.dr.saloon.utils.Utils.Companion.mUid
import com.dr.saloon.utils.Utils.Companion.setCancellationCharge
import com.dr.saloon.utils.Utils.Companion.setCancellationChargeCommission
import com.dr.saloon.utils.Utils.Companion.setCashFreeApi
import com.dr.saloon.utils.Utils.Companion.setCashFreeSecret
import com.dr.saloon.utils.Utils.Companion.setReferPolicy
import com.dr.saloon.utils.Utils.Companion.setTotalAppointments
import com.dr.saloon.utils.Utils.Companion.updateLoginSlider
import com.dr.saloon.utils.Utils.Companion.updateWishList
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var binding : ActivityMainBinding
    private lateinit var navController: NavController
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

       // Firebase.initialize(this)

        try {
            versionControls()
            if (isNotificationOn(this)) {
                FirebaseMessaging.getInstance().subscribeToTopic("all")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
            }
        } catch (e: Exception) {}

        binding.bottomNavigation.visibility = View.GONE

        navHostFragment = supportFragmentManager.findFragmentById(binding.navHostFragment.id) as NavHostFragment
        navController = navHostFragment.navController
        setUpBottomNavigation()
        onDestinationChange()

        val executor = Executors.newSingleThreadExecutor()

        executor.execute {
            updateLoginSlider(this)
            if (getAuthStatus(this) == AUTH_STATUS_DONE) {
                try {
                    if (isNotificationOn(this)) {
                        FirebaseMessaging.getInstance().subscribeToTopic(mUid(this))
                    } else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(mUid(this))
                    }
                } catch (e: Exception) {}

            }
        }
    }

    private fun onDestinationChange() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.home-> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.wishList-> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.gallery-> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.topSalon-> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                R.id.profile-> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else-> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }

    private fun setUpBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun versionControls() {

        val pInfo =  packageManager.getPackageInfo(packageName, 0)
        val version = pInfo.versionName

        db.collection("Versions")
            .document(version)
            .get().addOnSuccessListener {
                try {
                    if (it.exists()) {
                        val isDisabled = it["isDisabled"] as Boolean
                        val isShowUpdateDialog = it["isShowUpdateAlert"] as Boolean
                        if (isDisabled) {
                            AlertDialog.Builder(this)
                                .setTitle("Update Alert!")
                                .setMessage("A newer version of this application is available. Please update for better experience.")
                                .setCancelable(false)
                                .setNeutralButton("Update"){_,_->
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.dr.saloon")
                                    startActivity(intent)
                                }.create().show()
                        }
                        if (isShowUpdateDialog) {
                            AlertDialog.Builder(this)
                                .setTitle("Update Alert!")
                                .setMessage("A newer version of this application is available. Please update for better experience.")
                                .setNeutralButton("Update"){_,_->
                                    val intent = Intent(Intent.ACTION_VIEW)
                                    intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.dr.saloon")
                                    startActivity(intent)
                                }.create().show()
                        }
                    }
                } catch (e : Exception) {}
            }
    }

}