package com.dr.saloon.fragments.salons

import android.content.Context
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentSalonSearchBinding
import com.dr.saloon.databinding.RowItemTopSalonsBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.Utils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SalonSearch : Fragment() {

    private lateinit var binding : FragmentSalonSearchBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private var isLoading = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSalonSearchBinding.inflate(layoutInflater)
        binding.etSearch.isFocusableInTouchMode = true
        binding.etSearch.isFocusable = true
        binding.etSearch.requestFocus()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actions()
        salon()

    }

    private fun actions() {

        binding.back.setOnClickListener { findNavController().popBackStack(R.id.salonSearch,true) }

        binding.etSearch.addTextChangedListener {
            search(binding.etSearch.text.toString().trim().replace(" ","").lowercase())
        }
    }

    private fun salon() {

        val salonList = ArrayList<ModelSalon>()

        val adapter = AdapterSalon(salonList)

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(AppConstaints.FIELD_RATING, Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                }
                for (d in it) {
                    try {
                        val services = d[AppConstaints.SERVICES] as ArrayList<String>

                        var srvs = ""
                        for (i in 0 until services.size){
                            srvs +=  services[i].split('?')[0] + ","
                        }

                        val images = d[AppConstaints.IMAGE_URL] as ArrayList<String>

                        if (d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED) {
                            salonList.add(
                                ModelSalon(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    d[AppConstaints.LATITUDE].toString().toDouble(),
                                    d[AppConstaints.LONGITUDE].toString().toDouble(),
                                    d[AppConstaints.SALON_TYPE].toString(),
                                    d.id))
                            adapter.notifyItemInserted(salonList.size - 1)
                        }

                        binding.pb.visibility = View.GONE

                        isLoading = false

                        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                if (!isLoading) {
                                    binding.pb.visibility = View.VISIBLE
                                    loadMore(d, adapter, salonList)
                                    isLoading = true
                                }
                            }
                        })

                    } catch (e : Exception) {}
                }
            }

        binding.rv.adapter = adapter

    }

    private fun loadMore(startAfter : DocumentSnapshot, adapterSalon:  AdapterSalon, list: ArrayList<ModelSalon>) {

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(AppConstaints.FIELD_RATING, Query.Direction.DESCENDING)
            .startAfter(startAfter)
            .limit(10)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                }
                for (d in it) {
                    try {
                        val services = d[AppConstaints.SERVICES] as ArrayList<String>

                        var srvs = ""
                        for (i in 0 until services.size){
                            srvs +=  services[i].split('?')[0] + ","
                        }


                        val images = d[AppConstaints.IMAGE_URL] as ArrayList<String>

                        if (d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED) {
                            list.add(
                                ModelSalon(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    d[AppConstaints.LATITUDE].toString().toDouble(),
                                    d[AppConstaints.LONGITUDE].toString().toDouble(),
                                    d[AppConstaints.SALON_TYPE].toString(),
                                    d.id))
                            adapterSalon.notifyItemInserted(list.size - 1)
                        }

                        isLoading = list.size < 10

                        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                if (!isLoading) {
                                    binding.pb.visibility = View.VISIBLE
                                    loadMore(d, adapterSalon, list)
                                    isLoading = true
                                }
                            }
                        })

                        binding.pb.visibility = View.GONE
                    } catch (e : Exception) { }
                }
            }
    }

    private fun search(s : String) {

        val salonList = ArrayList<ModelSalon>()

        val adapter = AdapterSalon(salonList)

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(AppConstaints.FIELD_RATING, Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                }
                for (d in it) {
                    try {
                        if ((d[AppConstaints.SALON_NAME].toString().trim().replace(" ","").lowercase().contains(s) || d[AppConstaints.SALON_ADDRESS].toString().trim().replace(" ","").lowercase().contains(s)) && (d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED)) {

                            val services = d[AppConstaints.SERVICES] as ArrayList<String>

                            var srvs = ""
                            for (i in 0 until services.size){
                                srvs +=  services[i].split('?')[0] + ","
                            }

                            val images = d[AppConstaints.IMAGE_URL] as ArrayList<String>

                            salonList.add(
                                ModelSalon(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    d[AppConstaints.LATITUDE].toString().toDouble(),
                                    d[AppConstaints.LONGITUDE].toString().toDouble(),
                                    d[AppConstaints.SALON_TYPE].toString(),
                                    d.id))
                            adapter.notifyItemInserted(salonList.size - 1)
                            binding.pb.visibility = View.GONE

                        }
                    } catch (e : Exception) {}
                }
            }

        binding.rv.adapter = adapter

    }

    inner class AdapterSalon(private val list: ArrayList<ModelSalon>) : RecyclerView.Adapter<AdapterSalon.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemSalonByDefaultBinding: RowItemTopSalonsBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemSalonByDefaultBinding =
                RowItemTopSalonsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemSalonByDefaultBinding.root, rowItemSalonByDefaultBinding)
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {
            holder.rowItemSalonByDefaultBinding.salonName.text = list[position].salonName
            holder.rowItemSalonByDefaultBinding.rating.rating = list[position].rating
            holder.rowItemSalonByDefaultBinding.services.text = list[position].services
            holder.rowItemSalonByDefaultBinding.salonType.text = list[position].salonType
            holder.rowItemSalonByDefaultBinding.imageView.load(list[position].imageUrl)

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(AppConstaints.SALON_ID, list[position].id)
                findNavController().navigate(R.id.salonDetails, args)
            }

            try {
                val mylocation = Location("")
                mylocation.latitude =
                    SharedPref.getDouble(holder.itemView.context, AppConstaints.LATITUDE)
                mylocation.longitude =
                    SharedPref.getDouble(holder.itemView.context, AppConstaints.LONGITUDE)
                val dest_location = Location("")

                dest_location.latitude = list[position].latitude
                dest_location.longitude = list[position].longitude

                val distance = mylocation.distanceTo(dest_location) //in meters

                holder.rowItemSalonByDefaultBinding.distance.text = Utils.getFormattedFloatInString(
                    distance / 1000f,
                    2
                ).plus("Km...")
            } catch (e : Exception) {

            }

            when(list[position].salonType) {
                AppConstaints.MALE -> {
                    holder.rowItemSalonByDefaultBinding.salonType.text = "Men"
                    holder.rowItemSalonByDefaultBinding.salonType.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.man_16,
                        0,
                        0,
                        0
                    )
                }
                AppConstaints.FEMALE -> {
                    holder.rowItemSalonByDefaultBinding.salonType.text = "Women"
                    holder.rowItemSalonByDefaultBinding.salonType.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.woman_16,
                        0,
                        0,
                        0
                    )
                }
                AppConstaints.UNISEX -> {
                    holder.rowItemSalonByDefaultBinding.salonType.text = "Unisex"
                    holder.rowItemSalonByDefaultBinding.salonType.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.unisex_16,
                        0,
                        0,
                        0
                    )
                }
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    inner class ModelSalon(val salonName : String, val imageUrl : String, val rating : Float, val services : String,  val latitude : Double, val longitude : Double, val salonType : String , val id : String)



}