package com.dr.saloon.payments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.cashfree.pg.CFPaymentService
import com.dr.saloon.R
import com.dr.saloon.databinding.ActivityPaymentBinding
import com.dr.saloon.notifications.Notification
import com.dr.saloon.utils.AppConstaints.AMOUNT
import com.dr.saloon.utils.AppConstaints.COLLECTION_PAYMENT_ORDERS
import com.dr.saloon.utils.AppConstaints.IP_ADDRESS
import com.dr.saloon.utils.AppConstaints.ORDER_ID
import com.dr.saloon.utils.AppConstaints.PAYMENT_METHOD
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS_PENDING
import com.dr.saloon.utils.AppConstaints.PAYMENT_STATUS_SUCCESS
import com.dr.saloon.utils.AppConstaints.REMARK
import com.dr.saloon.utils.AppConstaints.SOMETHING_WENT_WRONG
import com.dr.saloon.utils.AppConstaints.THE_TITLE
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getCashFreeApi
import com.dr.saloon.utils.Utils.Companion.getCashFreeSecret
import com.dr.saloon.utils.Utils.Companion.getCurrentBalance
import com.dr.saloon.utils.Utils.Companion.getEmail
import com.dr.saloon.utils.Utils.Companion.getIpAddress
import com.dr.saloon.utils.Utils.Companion.getPhone
import com.dr.saloon.utils.Utils.Companion.getUserName
import com.dr.saloon.utils.Utils.Companion.mUid
import com.dr.saloon.utils.Utils.Companion.setCurrentBalance
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.json.JSONException
import org.json.JSONObject

class PaymentActivity : AppCompatActivity() {
    
    private lateinit var binding : ActivityPaymentBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var loading : Dialog
    private var amount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        actions()
        addingToWalletDialog()

    }


    private fun actions() {

        binding.back.setOnClickListener {
            finish()
        }

        binding.amount.addTextChangedListener {
            amount = if (binding.amount.text.toString().trim().isNotEmpty()) binding.amount.text.toString().trim().toInt() else 0
            binding.pay.text = "Pay ${amount}₹"
        }

        binding.pay.setOnClickListener {
             if (amount == 0) {
                 binding.amount.error = "Please enter a amount"
                 return@setOnClickListener
             }
            var remark = "Adding Payment For Appointment"
            if (binding.remark.text.toString().trim().isNotEmpty()){
                remark = binding.remark.text.toString().trim()
            }
            showLoading(true)
            createOrderInCashFreeServer(amount.toString(), remark)
        }
    }

    private fun createOrderInCashFreeServer(amount: String, title : String) {

        val orderId = "order_id_".plus(System.currentTimeMillis())

        val jsonObjectRequest = object : StringRequest(
            Method.POST,"https://api.cashfree.com/api/v2/cftoken/order/",
            {
                try {
                    val obj = JSONObject(it)
                    val token = obj.get("cftoken").toString()
                    createOrderInFirestore(orderId, token, amount, title)
                } catch (e : Exception) {
                    showLoading(false)
                    Toast.makeText(this, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                }
            },   {
                showLoading(false)
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val credentials = getCashFreeApi(this@PaymentActivity) + ":" + getCashFreeSecret(this@PaymentActivity)
                val base64EncodedCredentials: String =
                    Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
                val headers: HashMap<String, String> = HashMap()
                headers["Authorization"] = "Basic $base64EncodedCredentials"
                headers["Content-Type"] = "application/json"
                headers["x-client-id"] = Utils.getCashFreeApi(this@PaymentActivity)
                headers["x-client-secret"] = Utils.getCashFreeSecret(this@PaymentActivity)
                return headers
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = JSONObject()
                params.put("orderAmount", amount.toInt())
                params.put("orderId", orderId)
                params.put("orderCurrency", "INR")
                return params.toString().toByteArray(charset = Charsets.UTF_8)
            }
        }

        val queue = Volley.newRequestQueue(this)
        queue.add(jsonObjectRequest)

    }

    private fun createOrderInFirestore(orderId: String, token : String, amount: String, title: String) {

        val map = HashMap<String, Any>()
        map[AMOUNT] = amount.toFloat()
        map[ORDER_ID] = orderId
        map["token"] = token
        map[THE_TITLE] = title
        map[REMARK] = binding.remark.text.toString().trim().ifEmpty { " " }
        map[PAYMENT_METHOD] = "CashFree"
        map[IP_ADDRESS] = getIpAddress()
        map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
        map[USER_ID] = mUid(this)
        map[PAYMENT_STATUS] = PAYMENT_STATUS_PENDING

        db.collection(COLLECTION_PAYMENT_ORDERS)
            .document(orderId)
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                startCashFree(token, orderId, amount)
            }
    }

    private fun startCashFree(token: String, orderId: String, amount: String) {

        val params = HashMap<String,String>()
        params[CFPaymentService.PARAM_APP_ID] = Utils.getCashFreeApi(this)
        params[CFPaymentService.PARAM_ORDER_ID] = orderId
        params[CFPaymentService.PARAM_ORDER_CURRENCY] = "INR"
        params[CFPaymentService.PARAM_ORDER_AMOUNT] = amount
        params[CFPaymentService.PARAM_CUSTOMER_NAME] = getUserName(this)
        params[CFPaymentService.PARAM_CUSTOMER_EMAIL] = getEmail(this)
        params[CFPaymentService.PARAM_CUSTOMER_PHONE] = getPhone(this)

        val methods = arrayOf("UPI", "Other")

        AlertDialog.Builder(this)
            .setTitle("Choose Payment Method")
            .setItems(methods){_,i->
                when(i) {
                    0-> CFPaymentService.getCFPaymentServiceInstance().upiPayment(this, params, token, "PROD")
                    1-> CFPaymentService.getCFPaymentServiceInstance().doPayment(this, params, token, "PROD")
                }
            }.create().show()

    }

    private fun showLoading(isShow : Boolean) {
        if (isShow) {
            binding.pay.text = ""
            binding.pay.isEnabled = false
            binding.pay.icon = null
            binding.pb.visibility = View.VISIBLE
        } else {
            binding.pay.text = "Pay Now"
            binding.pay.isEnabled = true
            binding.pay.icon = ContextCompat.getDrawable(this, R.drawable.payment_24)
            binding.pb.visibility = View.GONE
        }
    }

    private fun addingToWalletDialog() {

        loading = Dialog(this)
        loading.setContentView(R.layout.adding_balance_to_wallet_dialog)
        loading.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CFPaymentService.REQ_CODE && data != null) {
            val bundle = data.extras
            if (bundle != null) {
                val json = JSONObject()
                val keys = bundle.keySet()
                for (key in keys) {
                    try {
                        json.put(key, JSONObject.wrap(bundle[key]))
                    } catch (e: JSONException) { }
                }
                try {

                    val status = json.get("txStatus").toString()
                    val orderId = json.get("orderId").toString()

                    if (status.equals("success", ignoreCase = true)) {

                        loading.show()

                        val map = HashMap<String,Any>()
                        map[PAYMENT_STATUS] = PAYMENT_STATUS_SUCCESS

                        db.collection(COLLECTION_PAYMENT_ORDERS)
                            .document(orderId)
                            .get().addOnSuccessListener {
                                try {
                                    val amnt = it[AMOUNT].toString().toFloat()
                                    Notification.SendNotificationToTopic("$amnt₹ Payment Successful", "$amnt₹ Payment Successful by ${getUserName(this)}","admin", this)
                                    setCurrentBalance(this, getCurrentBalance(this) + amnt)
                                } catch (e : Exception) { }
                            }


                        db.collection(COLLECTION_PAYMENT_ORDERS)
                            .document(orderId)
                            .set(map, SetOptions.merge()).addOnSuccessListener {
                                try {
                                    loading.dismiss()
                                    Snackbar.make(binding.root, "Payment Added Successfully", Snackbar.LENGTH_SHORT).show()
                                } catch (e : Exception) {}
                                finish()
                            }
                    } else {
                        showLoading(false)
                        db.collection(COLLECTION_PAYMENT_ORDERS)
                            .document(orderId).delete()
                        Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                    }
                } catch (e : Exception) {
                    Toast.makeText(this, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}