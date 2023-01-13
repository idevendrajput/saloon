package com.dr.saloon.fragments.salons

import android.content.Context
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.dr.saloon.R
import com.dr.saloon.SliderAdapter
import com.dr.saloon.SliderItems
import com.dr.saloon.databinding.FragmentTopSalonBinding
import com.dr.saloon.databinding.RowItemTopSalonsBinding
import com.dr.saloon.fragments.services.AdapterTopSalonShimmer
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_TYPE
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.Utils
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TopSalon : Fragment() {

    private lateinit var binding : FragmentTopSalonBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable : Runnable
    private var isLoading  = true

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
        binding = FragmentTopSalonBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setViewPagerHeight()
        actions()
        slider(binding.viewPager2)
        try {

        } catch (e: Exception) {}

    }

    override fun onPause() {
        super.onPause()
        try {
            binding.viewPager2.removeAllViewsInLayout()
        } catch (e : Exception) { }

    }

    override fun onResume() {
        super.onResume()
        try {
            salon()
        } catch (e: Exception) {}
    }

    private fun setViewPagerHeight() {
        val lp = binding.viewPager2.layoutParams as ConstraintLayout.LayoutParams
        lp.height = ((Utils.getScreenWidth() - 80) / 3)
    }

    private fun actions() {
        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.topSalon,true) }

        binding.search.setOnClickListener {
            findNavController().navigate(R.id.salonSearch)
        }

    }


    
    private fun salon() {

        // Shimmer Effect
        val adapterShimmer = AdapterTopSalonShimmer()
        binding.rv.adapter = adapterShimmer

        // Real Data

        val salonList = ArrayList<ModelSalon>()

        val adapterServices = AdapterSalon(salonList)

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(AppConstaints.FIELD_RATING, Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener {
                for (d in it) {
                    try {
                         val services = d[AppConstaints.SERVICES] as ArrayList<String>

                         var srvs = ""
                         for (i in 0 until services.size){
                             srvs +=  services[i].split('?')[0] + ","
                         }

                         val images = d[IMAGE_URL] as ArrayList<String>

                        if (d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED) {
                            salonList.add(
                                ModelSalon(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    d[AppConstaints.LATITUDE].toString().toDouble(),
                                    d[AppConstaints.LONGITUDE].toString().toDouble(),
                                    d[SALON_TYPE].toString(),
                                    d.id))
                            adapterServices.notifyItemInserted(salonList.size - 1)
                        }



                        isLoading = false

                        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                if (!isLoading) {
                                    binding.pb.visibility = View.VISIBLE
                                    loadMore(d, adapterServices, salonList)
                                    isLoading = true
                                }
                            }
                        })

                    } catch (e : Exception) {}
                }
                binding.rv.adapter = adapterServices
            }
    }

    private fun loadMore(startAfter : DocumentSnapshot, adapterSalon: AdapterSalon, list: ArrayList<ModelSalon>) {

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

                         val images = d[IMAGE_URL] as ArrayList<String>

                        if (d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED) {
                            list.add(
                                ModelSalon(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    d[AppConstaints.LATITUDE].toString().toDouble(),
                                    d[AppConstaints.LONGITUDE].toString().toDouble(),
                                    d[SALON_TYPE].toString(),
                                    d.id))
                            adapterSalon.notifyItemInserted(list.size - 1)
                        }

                        isLoading = it.size() < 10

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

    private fun slider(viewPager2: ViewPager2)  {

        val sliderItems = ArrayList<SliderItems>()
        val adapter = SliderAdapter(sliderItems, viewPager2, findNavController())

        for (i in 0..5) {
            sliderItems.add(SliderItems("", ""))
            adapter.notifyItemInserted(adapter.itemCount - 1)
        }

        db.collection(AppConstaints.COLLECTION_SALON)
            .orderBy(AppConstaints.FIELD_RATING, Query.Direction.DESCENDING)
            .limit(5)
            .get().addOnSuccessListener {
                sliderItems.clear()
                adapter.notifyDataSetChanged()
                for (d in it) {
                    try {
                        if ((d[AppConstaints.SALON_REVIEW_STATUS] != null && d[AppConstaints.SALON_REVIEW_STATUS] == AppConstaints.SALON_REVIEW_STATUS_APPROVED)) {
                            val image = d[IMAGE_URL] as ArrayList<String>
                            sliderItems.add(SliderItems(image[0], d.id))
                            adapter.notifyItemInserted(adapter.itemCount - 1)
                        }
                    } catch (e : Exception) {}
                }

                try {

                    viewPager2.adapter = adapter
                    viewPager2.clipToPadding = false
                    viewPager2.clipChildren = false
                    viewPager2.offscreenPageLimit = 3
                    viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER


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

                } catch (e : Exception) { }

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

            val typeface  = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
            holder.rowItemSalonByDefaultBinding.salonName.typeface = typeface
            holder.rowItemSalonByDefaultBinding.services.typeface = typeface
            holder.rowItemSalonByDefaultBinding.distance.typeface = typeface
            holder.rowItemSalonByDefaultBinding.book.typeface = typeface
            holder.rowItemSalonByDefaultBinding.salonName.typeface = typeface
            holder.rowItemSalonByDefaultBinding.salonName.text = list[position].salonName
            holder.rowItemSalonByDefaultBinding.rating.rating = list[position].rating
            holder.rowItemSalonByDefaultBinding.services.text = list[position].services
            holder.rowItemSalonByDefaultBinding.salonType.text = list[position].salonType
            holder.rowItemSalonByDefaultBinding.imageView.load(list[position].imageUrl)

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
            } catch (e : Exception) { }

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(SALON_ID, list[position].id)
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

    inner class ModelSalon(val salonName : String, val imageUrl : String, val rating : Float, val services : String, val latitude : Double, val longitude : Double, val salonType : String , val id : String)


}