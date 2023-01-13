package com.dr.saloon.fragments.salons

import android.content.Context
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentSalonByLocationBinding
import com.dr.saloon.databinding.RowItemTopSalonsBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.LATITUDE
import com.dr.saloon.utils.AppConstaints.LONGITUDE
import com.dr.saloon.utils.AppConstaints.PLACE_NAME
import com.dr.saloon.utils.Utils.Companion.getFormattedFloatInString
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class SalonByLocation : Fragment() {

    private lateinit var mContext : Context
    private lateinit var binding : FragmentSalonByLocationBinding
    private var latitude = .0
    private var longitude = .0
    private val db = FirebaseFirestore.getInstance()
    private lateinit var placeName : String
    private val allSalons = ArrayList<ModelSalon>()
    private val salonList = ArrayList<ModelSalon>()
    private var isLoading = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSalonByLocationBinding.inflate(layoutInflater)
        latitude = arguments?.getDouble(LATITUDE) as Double
        longitude = arguments?.getDouble(LONGITUDE) as Double
        placeName = arguments?.getString(PLACE_NAME).toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        actions()
    }

    private fun updateUi() {
        binding.location.text = placeName
        binding.location.typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")

    }

    override fun onResume() {
        super.onResume()
        updateUi()
        salon()
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rv.removeAllViewsInLayout()
        } catch (e : Exception) {}
    }

    private fun actions() {
        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.salonByLocation, true)
        }
        binding.location.setOnClickListener {
            if (placeName == "Nearby Me"){
                findNavController().navigate(R.id.searchCities)
                return@setOnClickListener
            }
            findNavController().popBackStack(R.id.salonByLocation, true)
        }
    }

    private fun salon() {

        binding.pb.visibility = View.VISIBLE
        allSalons.clear()
        salonList.clear()

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(LATITUDE, Query.Direction.DESCENDING).get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                var v = 0

                for (d in it) {
                    try {

                        v++
                        val mylocation = Location("")
                        mylocation.latitude = latitude
                        mylocation.longitude = longitude
                        val dest_location = Location("")
                        dest_location.latitude = d[LATITUDE].toString().toDouble()
                        dest_location.longitude = d[LONGITUDE].toString().toDouble()

                        val distance = (mylocation.distanceTo(dest_location) / 1000).toDouble()
                        
                        if (distance <= 150.0 && (d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED)){

                            val services = d[AppConstaints.SERVICES] as ArrayList<String>

                            var srvs = ""
                            for (i in 0 until services.size){
                                srvs +=  services[i].split('?')[0] + ","
                            }

                            val images = d[AppConstaints.IMAGE_URL] as ArrayList<String>

                            allSalons.add(
                                ModelSalon(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    distance,
                                    d[AppConstaints.SALON_TYPE].toString(),
                                    d.id))

                        }

                        Collections.sort(allSalons, Comparator<ModelSalon?> { o1, o2 ->
                            (o1?.distance as Double).compareTo((o2?.distance as Double))
                        } as Comparator<ModelSalon?>?)

                        if (v >= it.size()){

                            if (allSalons.size == 0){
                                binding.pb.visibility = View.GONE
                                binding.noData.visibility = View.VISIBLE
                                return@addOnSuccessListener
                            }

                            val adapter = AdapterSalon(salonList)

                            for (i in 0..9) {
                               if (i < allSalons.size) {
                                   salonList.add(
                                       ModelSalon(
                                           allSalons[i].salonName,
                                           allSalons[i].imageUrl,
                                           allSalons[i].rating,
                                           allSalons[i].services,
                                           allSalons[i].distance,
                                           allSalons[i].salonType,
                                           allSalons[i].id))
                                   adapter.notifyItemInserted(salonList.size - 1)
                               }
                            }

                            binding.pb.visibility = View.GONE
                            isLoading = false

                            binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { view, _, scrollY, _, _ ->
                                if (scrollY == view.getChildAt(0).measuredHeight - view.measuredHeight) {
                                    if (!isLoading) {
                                        binding.pb.visibility = View.VISIBLE
                                        loadMore(adapter, 10, 19)
                                        isLoading = true
                                    }
                                }
                            })

                            binding.rv.adapter = adapter
                        }

                    } catch (e : Exception) {}
                }
            }
    }

    private fun loadMore(adapter : AdapterSalon, startIndex : Int, endIndex: Int) {

        for (i in startIndex..endIndex) {

            try {

                if (i < allSalons.size) {
                    salonList.add(
                        ModelSalon(
                            allSalons[i].salonName,
                            allSalons[i].imageUrl,
                            allSalons[i].rating,
                            allSalons[i].services,
                            allSalons[i].distance,
                            allSalons[i].salonType,
                            allSalons[i].id))
                    adapter.notifyItemInserted(salonList.size - 1)
                }

                binding.pb.visibility = View.GONE
                isLoading = false

                binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                    if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                        binding.pb.visibility = View.VISIBLE
                        loadMore(adapter, endIndex+1, endIndex+10)
                        isLoading = true
                    }
                })

            } catch (e : Exception) {}

        }
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

            holder.rowItemSalonByDefaultBinding.distance.text = getFormattedFloatInString(list[position].distance.toFloat(),3).plus("Km...")

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(AppConstaints.SALON_ID, list[position].id)
                findNavController().navigate(R.id.salonDetails, args)
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

    inner class ModelSalon(val salonName : String, val imageUrl : String, val rating : Float, val services : String, val distance : Double, val salonType : String , val id : String)


}