package com.dr.saloon.notifications

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bumptech.glide.Glide
import com.dr.saloon.R
import com.dr.saloon.databinding.DialogNotificationDetailsBinding
import com.dr.saloon.databinding.FragmentNotificationsBinding
import com.dr.saloon.databinding.RowItemNotificationsBinding
import com.dr.saloon.utils.AppConstaints.COLLECTION_NOTIFICATIONS
import com.dr.saloon.utils.AppConstaints.DEFAULT_DATE_FORMAT
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.MESSAGE
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.Utils.Companion.mUid
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class Notifications : Fragment() {

    private lateinit var binding : FragmentNotificationsBinding
    private lateinit var mContext: Context
    private val allNotifications = ArrayList<NotificationModel>()
    private val list = ArrayList<NotificationModel>()
    private val adapterNotifications = AdapterNotifications(list)
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUi()
        actions()

    }

    override fun onResume() {
        super.onResume()
        try {
            loadData()
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rv.removeAllViewsInLayout()
        } catch (e: Exception) {}
    }

    private fun loadData() {

        binding.pb.visibility = View.VISIBLE

        db.collection(COLLECTION_NOTIFICATIONS)
            .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .get().addOnSuccessListener {
                try {

                    if (it.size() == 0){
                        binding.noData.visibility = View.VISIBLE
                        binding.pb.visibility = View.GONE
                        return@addOnSuccessListener
                    }

                    for ((v, d) in it.withIndex()) {
                        try {
                            if ((d[USER_ID] != null && d[USER_ID].toString() == mUid(mContext)) || d[USER_ID] == null){
                                var imageUrl = ""
                                if (d[IMAGE_URL] != null && d[IMAGE_URL].toString().trim().isNotEmpty()) {
                                    imageUrl = d[IMAGE_URL].toString().trim()
                                }
                                allNotifications.add(
                                    NotificationModel(
                                        d[MESSAGE].toString(),
                                        imageUrl,
                                        SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH).format(d.getDate(
                                            TIMESTAMP_FIELD) as Date),
                                        d.id)
                                )
                            }
                        } catch (e: Exception) {}

                        if (v >= it.size()-1){
                            if (allNotifications.size == 0) {
                                binding.noData.visibility = View.VISIBLE
                                binding.pb.visibility = View.GONE
                                return@addOnSuccessListener
                            }

                            for (x in 0..9) {
                                try {

                                    if (x < allNotifications.size){
                                        list.add(
                                            NotificationModel(
                                                allNotifications[x].message,
                                                allNotifications[x].imageUrl,
                                                allNotifications[x].date,
                                                allNotifications[x].id
                                            )
                                        )
                                        adapterNotifications.notifyItemInserted(list.size - 1)
                                    }

                                    binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                                        if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                            binding.pb.visibility = View.VISIBLE
                                            loadMore(10, 19)
                                        }
                                    })
                                    binding.pb.visibility = View.GONE
                                } catch (e : Exception) {}
                            }

                            binding.rv.adapter = adapterNotifications

                        }

                    }

                } catch (e: Exception) {}
            }


    }

    private fun loadMore(startIndex : Int, endIndex : Int) {
        for (x in startIndex..endIndex) {
            try {
                if (x < allNotifications.size){
                    list.add(
                        NotificationModel(
                            allNotifications[x].message,
                            allNotifications[x].imageUrl,
                            allNotifications[x].date,
                            allNotifications[x].id
                        )
                    )
                    adapterNotifications.notifyItemInserted(list.size - 1)
                }

                binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                        binding.pb.visibility = View.VISIBLE
                        loadMore(endIndex+1, endIndex+10)
                    }
                })
            } catch (e: Exception) {}
        }
    }

    private fun actions() {
        binding.back.setOnClickListener { findNavController().popBackStack(R.id.notifications,true) }
    }

    private fun updateUi() {

        binding.title.typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")

    }

    inner class AdapterNotifications(val list : ArrayList<NotificationModel>) : RecyclerView.Adapter<AdapterNotifications.NotificationHolder>() {

        inner class NotificationHolder(itemView: View, val rowItemNotificationsBinding: RowItemNotificationsBinding) : RecyclerView.ViewHolder(itemView)

         override fun onCreateViewHolder(
             parent: ViewGroup,
             viewType: Int
         ): NotificationHolder {
             val binding = RowItemNotificationsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
             return NotificationHolder(binding.root, binding)
         }

         override fun onBindViewHolder(holder: NotificationHolder, position: Int) {
              holder.rowItemNotificationsBinding.message.text = list[position].message
              holder.rowItemNotificationsBinding.time.text = list[position].date
              if (list[position].imageUrl.isNotEmpty()) {
                  Glide.with(holder.itemView.context)
                      .load(list[position].imageUrl).into(holder.rowItemNotificationsBinding.image)
              }
             holder.itemView.setOnClickListener {
                 notificationDetailsDialog(
                     list[position].date,
                     list[position].imageUrl,
                     list[position].message
                 )
             }
         }

         override fun getItemCount(): Int {
             return list.size
         }

     }

    private fun notificationDetailsDialog(date: String, imageUrl: String, message: String) {

        val d = Dialog(mContext)
        val dBinding = DialogNotificationDetailsBinding.inflate(d.layoutInflater)
        d.setContentView(dBinding.root)
        d.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        d.show()

        dBinding.dateTime.text = date
        dBinding.message.text = message

        dBinding.close.setOnClickListener {
            d.dismiss()
        }

        if (imageUrl.isEmpty()) {
            dBinding.imageView.visibility = View.GONE
            return
        }
        dBinding.imageView.load(imageUrl)

    }

    inner class NotificationModel(val message : String, val imageUrl : String, val date : String, val id : String)

}

