package com.dr.saloon.fragments.salons

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.DialogReviewRepliesBinding
import com.dr.saloon.databinding.FragmentAllReviewsBinding
import com.dr.saloon.databinding.RowItemReviewsBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class SalonAllReview : Fragment() {

    private lateinit var binding: FragmentAllReviewsBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private lateinit var salonId : String
    private val allReviews = ArrayList<ModelReviews>()
    private val list = ArrayList<ModelReviews>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllReviewsBinding.inflate(layoutInflater)

        try {
            salonId = arguments?.getString(SALON_ID).toString()
        } catch (e : Exception) { }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            review()
            actions()
        } catch (e: Exception) {}

    }

    private fun actions() {
        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.salonAllReview, true)
        }
    }

    private fun review() {

        val list = ArrayList<ModelReviews>()

        val adapterReviews = AdapterReviews(list, salonId, object : ReviewView {
            override fun reviewView(
                reply: TextView,
                id: String,
                ratingBar: RatingBar,
                txtRating: TextView,
                itemView: View,
                userId: String
            ) {
                ratingBar.visibility = View.VISIBLE
                txtRating.visibility = View.VISIBLE
                reply.visibility = View.VISIBLE

                itemView.setOnClickListener {
                    reviewsReplies(id, salonId,userId)
                }
            }
        })

        val ref = db.collection(AppConstaints.COLLECTION_SALON)
            .document(salonId)
            .collection(AppConstaints.COLLECTION_REVIEWS)

        ref.orderBy(AppConstaints.FIELD_RATING, Query.Direction.DESCENDING)
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    binding.noData.visibility = View.GONE
                    binding.pb.visibility = View.GONE
                    return@addOnSuccessListener
                }
                var ratingCount = 0f
                for (d in it) {
                    try {
                        val r = d[AppConstaints.FIELD_RATING].toString().toFloat()
                        ratingCount += r
                        allReviews.add(ModelReviews(
                            d[AppConstaints.USER_ID].toString(),
                            d[AppConstaints.MESSAGE].toString(),
                            r,
                            d.id
                        ))
                    } catch (e : Exception) { }
                }

                val averageRating = ratingCount / it.size()

                binding.rating.rating = averageRating

                if (allReviews.size == 0) {
                    binding.noData.visibility = View.GONE
                    binding.pb.visibility = View.GONE
                    return@addOnSuccessListener
                } else {


                    for (i in 0..9) {
                       if (i < allReviews.size) {
                           list.add(ModelReviews(
                               allReviews[i].userId,
                               allReviews[i].message,
                               allReviews[i].rating,
                               allReviews[i].id
                           ))
                           adapterReviews.notifyItemInserted(list.size - 1)
                           binding.pb.visibility = View.GONE
                       }
                    }

                    binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                        if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                            binding.pb.visibility = View.VISIBLE
                            loadMore(10, 19, list, adapterReviews)
                        }
                    })

                    binding.rv.adapter = adapterReviews

                }

            }

    }

    private fun reviewsReplies(id: String, salonId: String, userId: String) {

        val replyDialog = BottomSheetDialog(mContext)
        val dBinding = DialogReviewRepliesBinding.inflate(replyDialog.layoutInflater)
        replyDialog.setContentView(dBinding.root)
        replyDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        replyDialog.show()

        val list = ArrayList<ModelReviews>()

        val adapterReviews = AdapterReviews(list, salonId, object: ReviewView {
            override fun reviewView(reply: TextView, id: String, ratingBar: RatingBar, txtRating: TextView, itemView: View, userId: String) {
                ratingBar.visibility = View.GONE
                txtRating.visibility = View.GONE
            }
        })

        if (userId == Utils.mUid(mContext)) {
            dBinding.etMessage.visibility = View.VISIBLE
            dBinding.send.visibility = View.VISIBLE
        } else {
            dBinding.etMessage.visibility = View.INVISIBLE
            dBinding.send.visibility = View.INVISIBLE
        }
        
        db.collection(AppConstaints.COLLECTION_SALON)
            .document(salonId)
            .collection(AppConstaints.COLLECTION_REVIEWS)
            .document(id)
            .collection(AppConstaints.COLLECTION_REVIEW_REPLIES)
            .orderBy(AppConstaints.TIMESTAMP_FIELD, Query.Direction.ASCENDING)
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    dBinding.noData.visibility = View.VISIBLE
                    dBinding.pb.visibility = View.GONE
                    return@addOnSuccessListener
                }
                dBinding.pb.visibility = View.GONE
                for (d in it) {
                    list.add(ModelReviews(
                        d[AppConstaints.USER_ID].toString(),
                        d[AppConstaints.MESSAGE].toString(),
                        0f,
                        d.id
                    ))
                    adapterReviews.notifyItemInserted(list.size - 1)
                }
            }

        dBinding.rv.adapter = adapterReviews

        dBinding.send.setOnClickListener {
            if (dBinding.etMessage.text.toString().trim().isNotEmpty()){

                lifecycleScope.launch {

                    val map = HashMap<String, Any>()
                    map[AppConstaints.USER_ID] = Utils.mUid(mContext)
                    map[AppConstaints.TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
                    map[AppConstaints.MESSAGE] = dBinding.etMessage.text.toString().trim()

                    val dId = UUID.randomUUID().toString()

                    list.add(ModelReviews(
                        Utils.mUid(mContext),
                        dBinding.etMessage.text.toString().trim(),
                        0f,
                        dId
                    ))
                    dBinding.noData.visibility = View.GONE
                    dBinding.etMessage.setText("")

                    adapterReviews.notifyItemInserted(list.size - 1)

                    db.collection(AppConstaints.COLLECTION_SALON)
                        .document(salonId)
                        .collection(AppConstaints.COLLECTION_REVIEWS)
                        .document(id)
                        .collection(AppConstaints.COLLECTION_REVIEW_REPLIES)
                        .document(dId)[map] = SetOptions.merge()

                }
            }
        }
    }

    interface ReviewView {
        fun reviewView(reply: TextView, id: String, ratingBar: RatingBar, txtRating: TextView, itemView: View, userId: String)
    }

    private fun loadMore(startIndex : Int, endIndex: Int, list: ArrayList<ModelReviews>, adapterReviews: AdapterReviews) {

        for (i in startIndex..endIndex) {
            if (i < allReviews.size) {
                list.add(ModelReviews(
                    allReviews[i].userId,
                    allReviews[i].message,
                    allReviews[i].rating,
                    allReviews[i].id
                ))
                adapterReviews.notifyItemInserted(list.size - 1)
            }
        }
        binding.pb.visibility = View.GONE
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                binding.pb.visibility = View.VISIBLE
                loadMore(endIndex + 1, endIndex + 10, list, adapterReviews)
            }
        })

    }

    private inner class AdapterReviews(private val list: ArrayList<ModelReviews>,val salonId: String, val reviewView: ReviewView) : RecyclerView.Adapter<AdapterReviews.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemCustomerReviewBinding: RowItemReviewsBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemCustomerReviewBinding =
                RowItemReviewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemCustomerReviewBinding.root, rowItemCustomerReviewBinding)
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {

            holder.rowItemCustomerReviewBinding.message.text = list[position].message
            holder.rowItemCustomerReviewBinding.txtRating.text = list[position].rating.toString()
            holder.rowItemCustomerReviewBinding.rating.rating = list[position].rating

            reviewView.reviewView(holder.rowItemCustomerReviewBinding.replies,
                list[position].id,
                holder.rowItemCustomerReviewBinding.rating,
                holder.rowItemCustomerReviewBinding.txtRating,
                holder.itemView, list[position].userId)

            db.collection(AppConstaints.USER_COLLECTION)
                .document(list[position].userId)
                .get().addOnSuccessListener {
                    try {
                        if (it.exists()) {
                            val name = it[AppConstaints.USER_NAME].toString()
                            val profileUrl = it[AppConstaints.PROFILE_URL].toString()
                            holder.rowItemCustomerReviewBinding.userName.text = name
                            holder.rowItemCustomerReviewBinding.profile.load(profileUrl)
                        } else {
                            val name = "User"
                            holder.rowItemCustomerReviewBinding.userName.text = name
                        }
                    } catch (e: Exception) {
                    }
                }

            db.collection(AppConstaints.COLLECTION_SALON)
                .document(salonId)
                .collection(AppConstaints.COLLECTION_REVIEWS)
                .document(list[position].id)
                .collection(AppConstaints.COLLECTION_REVIEW_REPLIES)
                .get().addOnSuccessListener {
                    if (it.size() == 0) {
                        holder.rowItemCustomerReviewBinding.replies.visibility = View.GONE
                    } else {
                        holder.rowItemCustomerReviewBinding.replies.visibility = View.VISIBLE
                    }
                }

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private inner class ModelReviews(val userId : String, val message : String, val rating : Float, val id : String)

}