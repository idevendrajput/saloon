package com.dr.saloon.fragments.salons

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentAllSalonsBinding
import com.dr.saloon.databinding.RowItemSalonBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.FEMALE
import com.dr.saloon.utils.AppConstaints.FIELD_RATING
import com.dr.saloon.utils.AppConstaints.MALE
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.STARTING_PRICE
import com.dr.saloon.utils.AppConstaints.UNISEX
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.Utils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.collections.ArrayList

class AllSalon : Fragment() {

    private lateinit var mContext : Context
    private lateinit var binding : FragmentAllSalonsBinding
    private val db = FirebaseFirestore.getInstance()
    private var collapseLayoutInitialWidth = 0
    private var isCollapse = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View  {
        binding = FragmentAllSalonsBinding.inflate(layoutInflater)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        salonFemale()

        binding.collapseLayout.doOnPreDraw {
            collapseLayoutInitialWidth = binding.collapseLayout.width
            animControls()
        }

    }

    private fun animControls() {

        binding.scrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > oldScrollY) {
                hideSideBar()
            }
            if (scrollY < oldScrollY) {
                showSideBar()
            }
        }

        binding.CollapseSideBar.setOnClickListener {
            hideSideBar()
        }

        binding.collapseLayout.setOnClickListener {
            showSideBar()
        }

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.allSalon, true)
        }

        binding.search.setOnClickListener {
            if (binding.etSearch.visibility == View.GONE) {
                binding.etSearch.visibility = View.VISIBLE
            }
        }
    }

    private fun hideSideBar() {
        if (!isCollapse) {
            Utils.collapseWidthWithAnim(binding.collapseLayout, binding.collapseLayout.width, 30)
            for (i in 0 until  binding.collapseLayout.childCount){
                binding.collapseLayout.getChildAt(i).visibility = View.GONE
            }
        }
        isCollapse = true
    }

    private fun showSideBar() {
        if (isCollapse) {
            Utils.expendWidthWithAnim(
                binding.collapseLayout,
                binding.collapseLayout.width,
                collapseLayoutInitialWidth
            )
            for (i in 0 until  binding.collapseLayout.childCount){
                binding.collapseLayout.getChildAt(i).visibility = View.VISIBLE
            }
        }
        isCollapse = false
    }
    
    private fun salonFemale() {

        sideBarItemSelection(0)

        val serviceList = ArrayList<ModelSalon>()

        val adapterServices = AdapterSalon(serviceList)

        serviceList.clear()
        binding.rv.removeAllViews()
        binding.noData.visibility = View.GONE

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(FIELD_RATING, Query.Direction.DESCENDING)
            .whereEqualTo(AppConstaints.SALON_TYPE, AppConstaints.FEMALE)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.GONE
                    return@addOnSuccessListener
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
                            serviceList.add(ModelSalon(d[AppConstaints.SALON_NAME].toString(),
                                images[0], d[FIELD_RATING].toString().toFloat(),
                                d[STARTING_PRICE].toString().toInt(),
                                srvs,
                                d[AppConstaints.LATITUDE].toString().toDouble(),
                                d[AppConstaints.LONGITUDE].toString().toDouble(),
                                d.id))
                            adapterServices.notifyItemInserted(serviceList.size - 1)
                        }

                        if (serviceList.size == 0) {
                            binding.noData.visibility = View.VISIBLE
                        } else {
                            binding.noData.visibility = View.GONE
                        }
                        binding.pb.visibility = View.GONE
                    } catch (e : Exception) {}
                }
            }

        
        binding.rv.adapter = adapterServices

        binding.etSearch.addTextChangedListener {
            search(FEMALE, binding.etSearch.text.toString())
        }

    }

    private fun salonMale() {

        sideBarItemSelection(1)

        val serviceList = ArrayList<ModelSalon>()

        val adapterServices = AdapterSalon(serviceList)

        serviceList.clear()
        binding.rv.removeAllViews()
        binding.noData.visibility = View.GONE

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(FIELD_RATING, Query.Direction.DESCENDING)
            .whereEqualTo(AppConstaints.SALON_TYPE, AppConstaints.MALE)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.GONE
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
                            serviceList.add(ModelSalon(d[AppConstaints.SALON_NAME].toString(),
                                images[0], d[FIELD_RATING].toString().toFloat(),
                                d[STARTING_PRICE].toString().toInt(),
                                srvs,
                                d[AppConstaints.LATITUDE].toString().toDouble(),
                                d[AppConstaints.LONGITUDE].toString().toDouble(),
                                d.id))
                            adapterServices.notifyItemInserted(serviceList.size - 1)
                        }

                        if (serviceList.size == 0) {
                            binding.noData.visibility = View.VISIBLE
                        } else {
                            binding.noData.visibility = View.GONE
                        }
                        binding.pb.visibility = View.GONE
                    } catch (e : Exception) {}
                }
            }

        binding.rv.adapter = adapterServices

        binding.etSearch.addTextChangedListener {
            search(MALE, binding.etSearch.text.toString())
        }

    }

    private fun salonUnisex() {
        
        sideBarItemSelection(2)
        
        val serviceList = ArrayList<ModelSalon>()
        
        val adapterServices = AdapterSalon(serviceList)
        
        serviceList.clear()
        binding.rv.removeAllViews()
        binding.noData.visibility = View.GONE
        
        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(FIELD_RATING, Query.Direction.DESCENDING)
            .whereEqualTo(AppConstaints.SALON_TYPE, AppConstaints.UNISEX)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.GONE
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
                            serviceList.add(ModelSalon(d[AppConstaints.SALON_NAME].toString(),
                                images[0], d[FIELD_RATING].toString().toFloat(),
                                d[STARTING_PRICE].toString().toInt(),
                                srvs,
                                d[AppConstaints.LATITUDE].toString().toDouble(),
                                d[AppConstaints.LONGITUDE].toString().toDouble(),
                                d.id))
                            adapterServices.notifyItemInserted(serviceList.size - 1)
                        }

                        if (serviceList.size == 0) {
                            binding.noData.visibility = View.VISIBLE
                        } else {
                            binding.noData.visibility = View.GONE
                        }
                        binding.pb.visibility = View.GONE
                    } catch (e : Exception) {}
                }
            }

        binding.rv.adapter = adapterServices

        binding.etSearch.addTextChangedListener {
            search(UNISEX, binding.etSearch.text.toString())
        }
    }

    private fun sideBarItemSelection(selectedItemPosition : Int) {
        val v = binding.collapseLayout
        for (i in 0..2) {
            val view = v.getChildAt(i) as TextView
            view.setTextColor(ColorStateList.valueOf(Color.parseColor(AppConstaints.COLOR_GRAY_LIGHT)))
            view.setOnClickListener {
                when(i) {
                    0-> {
                        salonFemale()
                        binding.title.text = "Salon for\nWomen"
                        binding.genderLogo.setImageResource(R.drawable.woman_128)
                    }
                    1-> {
                        salonMale()
                        binding.title.text = "Salon for\nMen"
                        binding.genderLogo.setImageResource(R.drawable.man_128)
                    }
                    2-> {
                        salonUnisex()
                        binding.title.text = "Unisex\nSalon"
                        binding.genderLogo.setImageResource(R.drawable.unisex_128)
                    }
                }
                sideBarItemSelection(i)
            }
        }
        (v.getChildAt(selectedItemPosition) as TextView).setTextColor(
            ColorStateList.valueOf(
                Color.parseColor(
                    AppConstaints.COLOR_WHITE
                )))
    }

    private fun search(serviceType : String, s : String) {

        if (s.isEmpty()) {
            when(serviceType) {
                FEMALE-> salonFemale()
                MALE-> salonMale()
                UNISEX-> salonUnisex()
            }
            return
        }

        val serviceList = ArrayList<ModelSalon>()

        val adapterServices = AdapterSalon(serviceList)

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(AppConstaints.DEFAULT_ORDER_FIELD, Query.Direction.DESCENDING)
            .whereEqualTo(AppConstaints.SALON_TYPE, serviceType)
            .get()
            .addOnSuccessListener {
                for (d in it) {
                    try {
                        if (d[SALON_NAME].toString().trim().replace(" ","").lowercase().contains(s.trim().replace(" ","").lowercase()) && d[AppConstaints.SALON_REVIEW_STATUS].toString() == AppConstaints.SALON_REVIEW_STATUS_APPROVED){

                            val services = d[AppConstaints.SERVICES] as ArrayList<String>

                            var srvs = ""
                            for (i in 0 until services.size){
                                srvs +=  services[i].split('?')[0] + ","
                            }
                            val images = d[AppConstaints.IMAGE_URL] as ArrayList<String>

                            serviceList.add(ModelSalon(d[SALON_NAME].toString(),
                               images[0], d[FIELD_RATING].toString().toFloat(),
                                d[STARTING_PRICE].toString().toInt(),srvs,
                                d[AppConstaints.LATITUDE].toString().toDouble(),
                                d[AppConstaints.LONGITUDE].toString().toDouble(),
                                d.id))
                            adapterServices.notifyItemInserted(serviceList.size - 1)
                        }
                        if (serviceList.size == 0) {
                            binding.noData.visibility = View.VISIBLE
                        } else {
                            binding.noData.visibility = View.GONE
                        }
                        binding.pb.visibility = View.GONE
                    } catch (e : Exception) {}
                }
            }

        binding.rv.adapter = adapterServices

    }

    inner class AdapterSalon(private val list: ArrayList<ModelSalon>) : RecyclerView.Adapter<AdapterSalon.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemSalonBinding: RowItemSalonBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemSalonBinding =
                RowItemSalonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemSalonBinding.root, rowItemSalonBinding)
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {

            val typeface  = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
            holder.rowItemSalonBinding.salonName.typeface = typeface
            holder.rowItemSalonBinding.services.typeface = typeface
            holder.rowItemSalonBinding.distance.typeface = typeface
            holder.rowItemSalonBinding.book.typeface = typeface
            holder.rowItemSalonBinding.startingPrice.typeface = typeface
            holder.rowItemSalonBinding.salonName.text = list[position].salonName
            holder.rowItemSalonBinding.rating.rating = list[position].rating
            holder.rowItemSalonBinding.startingPrice.text = list[position].startingPrice.toString() + "₹"
            holder.rowItemSalonBinding.services.text = list[position].services
            holder.rowItemSalonBinding.imageView.load(list[position].imageUrl)

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

                holder.rowItemSalonBinding.distance.text = Utils.getFormattedFloatInString(
                    distance / 1000f,
                    2
                ).plus("Km...")
            } catch (e : Exception) { }

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(AppConstaints.SALON_ID, list[position].id)
                findNavController().navigate(R.id.salonDetails, args)
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    inner class ModelSalon(val salonName : String, val imageUrl : String, val rating : Float, val startingPrice : Int, val services : String, val latitude : Double, val longitude : Double , val id : String)

}