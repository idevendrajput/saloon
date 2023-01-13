package com.dr.saloon.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.dr.saloon.MainActivity
import com.dr.saloon.R
import com.dr.saloon.SliderAdapter
import com.dr.saloon.SliderItems
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.database.NearBySalonEntity
import com.dr.saloon.database.WishlistEntity
import com.dr.saloon.databinding.*
import com.dr.saloon.fragments.services.AdapterSalonNearByShimmer
import com.dr.saloon.fragments.services.AdapterSalonShimmer
import com.dr.saloon.fragments.services.AdapterServicesShimmer
import com.dr.saloon.fragments.wishlist.WishList
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS_DONE
import com.dr.saloon.utils.AppConstaints.AUTH_STATUS_INFO
import com.dr.saloon.utils.AppConstaints.COLLECTION_SALON
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLLECTION_SLIDER
import com.dr.saloon.utils.AppConstaints.COLOR_RED
import com.dr.saloon.utils.AppConstaints.DEFAULT_ORDER_FIELD
import com.dr.saloon.utils.AppConstaints.FIELD_RATING
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.LATITUDE
import com.dr.saloon.utils.AppConstaints.LONGITUDE
import com.dr.saloon.utils.AppConstaints.PREFERENCE_SCORE
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SALON_REVIEW_STATUS
import com.dr.saloon.utils.AppConstaints.SALON_REVIEW_STATUS_APPROVED
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.dr.saloon.utils.AppConstaints.SERVICE_ID
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.THEME_COLOR
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.SharedPref
import com.dr.saloon.utils.SharedPref.Companion.getDouble
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.drawFavAnim
import com.dr.saloon.utils.Utils.Companion.getAuthStatus
import com.dr.saloon.utils.Utils.Companion.getFormattedFloatInString
import com.dr.saloon.utils.Utils.Companion.getPlaceName
import com.dr.saloon.utils.Utils.Companion.getProfileUrl
import com.dr.saloon.utils.Utils.Companion.mUid
import com.dr.saloon.utils.Utils.Companion.updateConstraintData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.IOException
import java.lang.Runnable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.Executors

class Home : Fragment() {

    private lateinit var binding : FragmentHomeBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable : Runnable
    private lateinit var exitDialog: Dialog
    private lateinit var room : MyDatabase

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
        binding = FragmentHomeBinding.inflate(layoutInflater)
        room = MyDatabase.getDatabase(mContext)
        when(getAuthStatus(mContext)){
            AUTH_STATUS_INFO-> {
                findNavController().navigate(R.id.informations)
            }
            ""-> {
                findNavController().navigate(R.id.login)
            }
        }
        return binding.root
    }

    private fun setViewPagerHeight() {
        val lp = binding.viewPager2.layoutParams as ConstraintLayout.LayoutParams
        lp.height = ((Utils.getScreenWidth() - 80) / 3)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setViewPagerHeight()
        exitDialog()

        if (getAuthStatus(mContext) == AUTH_STATUS_DONE) {

            updateUi()
            actions()
            getLocation()
            slider(binding.viewPager2)

            updateConstraintData(mContext)
            Utils.countBalance(mContext)
            Utils.updateWishList(mContext)
            Utils.countReferRewards(mContext)

            val intentType = (activity as AppCompatActivity).intent.getStringExtra(AppConstaints.INTENT_TYPE)
            if (intentType == AppConstaints.INTENT_TYPE_NOTIFICATION) {
                findNavController().navigate(R.id.notifications)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        services()
        salon()
        nearBySalon()
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rvNearBySalons.removeAllViewsInLayout()
            binding.rvSalons.removeAllViewsInLayout()
            binding.viewPager2.removeAllViewsInLayout()
        } catch (e : Exception) {}
    }

    private fun actions() {
        try {

            binding.search.setOnClickListener {
                findNavController().navigate(R.id.searchCities)
            }
            binding.notification.setOnClickListener {
                findNavController().navigate(R.id.notifications)
            }
            binding.location.setOnClickListener {
                findNavController().navigate(R.id.search)
            }
            binding.viewAllServices.setOnClickListener {
                findNavController().navigate(R.id.allServices)
            }
            binding.viewAllSalons.setOnClickListener {
                findNavController().navigate(R.id.allSalon)
            }

            binding.viewAllNearBySalons.setOnClickListener {
                val args = Bundle()
                args.putDouble(LATITUDE, getDouble(mContext, LATITUDE))
                args.putDouble(LONGITUDE, getDouble(mContext, LONGITUDE))
                args.putString(AppConstaints.PLACE_NAME, "Nearby Me")
                findNavController().navigate(R.id.salonByLocation, args)
            }

            binding.settings.setOnClickListener {
                findNavController().navigate(R.id.settings)
            }

            val onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    exitDialog.show()
                }
            }

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        } catch (e : Exception) { }

    }

    private fun exitDialog() {

        exitDialog = Dialog(mContext, com.google.android.material.R.style.ThemeOverlay_Material3)
        exitDialog.setContentView(R.layout.back_dialog)
        exitDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val exit = exitDialog.findViewById<TextView>(R.id.exit)

        exit.setOnClickListener {
            exitDialog.dismiss()
            (activity as AppCompatActivity).finishAffinity()
        }
        exitDialog.findViewById<TextView>(R.id.stayHere).setOnClickListener {
            exitDialog.dismiss() }
        exitDialog.findViewById<ImageView>(R.id.close).setOnClickListener {
            exitDialog.dismiss() }
        exit.paintFlags = exit.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // rv

        val list = room.nearBySalonDao().getAllNearBy() as ArrayList<NearBySalonEntity>

        val l = ArrayList<NearBySalonEntity>()

        for (i in 0..2){
            if (i < list.size){
                l.add(
                    NearBySalonEntity(
                      list[i].salonName,
                      list[i].imageUrl,
                      list[i].rating,
                      list[i].services,
                      list[i].distance,
                      list[i].id
                    )
                )
            }
        }

        val adapter = AdapterExitDialogViews(l, object: ExitDialogView {
            override fun exitDialogView(itemView: View, id: String) {
                itemView.setOnClickListener {
                    exitDialog.dismiss()
                    val args = Bundle()
                    args.putString(SALON_ID,  id)
                    findNavController().navigate(R.id.salonDetails, args)
                }
            }
        })

        exitDialog.findViewById<RecyclerView>(R.id.rv).adapter = adapter

    }

    private fun updateUi() {

        try {
            binding.location.text = getPlaceName(mContext)
            binding.settings.load(getProfileUrl(mContext))
        } catch (e : Exception) { }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun slider(viewPager2: ViewPager2 )  {

        val sliderItems = ArrayList<SliderItems>()
        val adapter = SliderAdapter(sliderItems, viewPager2, findNavController())

        for (i in 0..5) {
            sliderItems.add(SliderItems("",""))
            adapter.notifyItemInserted(adapter.itemCount - 1)
        }

        db.collection(COLLECTION_SLIDER)
            .orderBy(DEFAULT_ORDER_FIELD, Query.Direction.DESCENDING)
            .get().addOnSuccessListener {
                sliderItems.clear()
                adapter.notifyDataSetChanged()
                for (d in it) {
                    try {
                        var salonId = ""
                        if (d[SALON_ID] != null && d[SALON_ID].toString().trim().isNotEmpty()){
                            salonId = d[SALON_ID].toString()
                        }
                        sliderItems.add(SliderItems(d[IMAGE_URL].toString(), salonId))
                        adapter.notifyItemInserted(adapter.itemCount - 1)
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
                } catch (e : Exception) {
                }
            }
    }

    private fun services() {

        //Shimmer Effected Items

        binding.rvServices.adapter = AdapterServicesShimmer()

        // Real Data

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServicesHome(serviceList, findNavController())

        db.collection(COLLECTION_SERVICES)
            .orderBy(PREFERENCE_SCORE, Query.Direction.DESCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener {
                for (d in it) {
                    try {
                        serviceList.add(ModelServices(d[SERVICE_NAME].toString(),
                        d[IMAGE_URL].toString(),
                        d.id))
                        adapterServices.notifyItemInserted(serviceList.size - 1)
                    } catch (e : Exception) {}
                }
                binding.rvServices.adapter = adapterServices
            }
    }

    private fun salon() {

        // Shimmer Effect

        val adapterShimmer = AdapterSalonShimmer()
        binding.rvSalons.adapter = adapterShimmer

        // Real Data Load

        val salonList = ArrayList<ModelSalonHome>()

        val adapterSalon = AdapterSalonHome(salonList)

        db.collection(COLLECTION_SALON)
            .orderBy(FIELD_RATING, Query.Direction.DESCENDING)
            .limit(8)
            .get()
            .addOnSuccessListener {
                for (d in it) {
                    try {

                        val services = d[SERVICES] as ArrayList<String>

                        var s = ""
                        for (i in 0 until services.size){
                            s +=  services[i].split('?')[0] + ","
                        }

                        val images = d[IMAGE_URL] as ArrayList<String>

                        if (d[SALON_REVIEW_STATUS] != null && d[SALON_REVIEW_STATUS] == SALON_REVIEW_STATUS_APPROVED) {
                            salonList.add(ModelSalonHome(d[SALON_NAME].toString(),
                                images[0],
                                d[FIELD_RATING].toString().toFloat(),
                                s.substring(0,10) + "...",
                                d[THEME_COLOR].toString(),
                                d.id))
                            adapterSalon.notifyItemInserted(salonList.size - 1)
                        }

                    } catch (e : Exception) {}
                }
                binding.rvSalons.adapter = adapterSalon
            }
    }

    private fun getLocation() {
        Dexter.withContext(mContext)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    if (getPlaceName(mContext).isEmpty()) {
                        findNavController().navigate(R.id.search)
                    }
                }

                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    try {
                        if (getPlaceName(mContext).isEmpty()) {
                            val locationManager = (activity as AppCompatActivity).getSystemService(Context.LOCATION_SERVICE) as LocationManager
                            val locationProvider = LocationManager.NETWORK_PROVIDER
                            @SuppressLint("MissingPermission")
                            val lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)
                            val userLat = lastKnownLocation!!.latitude
                            val userLong = lastKnownLocation.longitude

                            Executors.newSingleThreadExecutor().execute {
                                SharedPref.setDouble(mContext, LATITUDE, userLat)
                                SharedPref.setDouble(mContext, LONGITUDE, userLong)
                                Utils.setPlaceName(mContext, getAddress(userLat, userLong))

                                val map = HashMap<String,Any>()
                                map[LATITUDE] = userLat
                                map[LONGITUDE] = userLong
                                db.collection(USER_COLLECTION)
                                    .document(mUid(mContext))[map] = SetOptions.merge()
                            }
                        }
                    } catch (e : Exception) {
                        Toast.makeText(mContext, AppConstaints.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {}
            }).check()
    }

    fun getAddress(lat: Double, lng: Double) : String {
        val geocoder = Geocoder( mContext, Locale.getDefault())
        var add = ""
        add = try {

            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val obj: Address = addresses[0]

            obj.locality

        } catch (e: IOException) {
            "Nearby me"
        }
        return add
    }

    private fun nearBySalon() {


        // Shimmer Effect

        val adapterShimmer = AdapterSalonNearByShimmer()
        binding.rvNearBySalons.adapter = adapterShimmer

        // Real Data

        val allSalons = ArrayList<ModelSalonHomeNearBy>()
        val salonList = ArrayList<ModelSalonHomeNearBy>()

        allSalons.clear()
        salonList.clear()

        db.collection(COLLECTION_SALON)
            .orderBy(LATITUDE, Query.Direction.DESCENDING).get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    return@addOnSuccessListener
                }

                var v = 0

                for (d in it) {
                    try {
                        v++
                        val mylocation = Location("")
                        mylocation.latitude = getDouble(mContext, LATITUDE)
                        mylocation.longitude = getDouble(mContext, LONGITUDE)
                        val dest_location = Location("")
                        dest_location.latitude = d[LATITUDE].toString().toDouble()
                        dest_location.longitude = d[LONGITUDE].toString().toDouble()

                        val distance = (mylocation.distanceTo(dest_location) / 1000).toDouble()

                        val services = d[SERVICES] as ArrayList<String>

                        var srvs = ""
                        for (i in 0 until services.size){
                            srvs +=  services[i].split('?')[0] + ","
                        }

                        val images = d[IMAGE_URL] as ArrayList<String>

                        if (d[SALON_REVIEW_STATUS] != null && d[SALON_REVIEW_STATUS] == SALON_REVIEW_STATUS_APPROVED) {
                            allSalons.add(
                                ModelSalonHomeNearBy(
                                    d[AppConstaints.SALON_NAME].toString(),
                                    images[0],
                                    d[AppConstaints.FIELD_RATING].toString().toFloat(),
                                    srvs,
                                    distance,
                                    d.id))
                        }
                        Collections.sort(allSalons, Comparator<ModelSalonHomeNearBy?> { o1, o2 ->
                            (o1?.distance as Double).compareTo((o2?.distance as Double))
                        } as Comparator<ModelSalonHomeNearBy?>?)

                        if (v >= it.size()){

                            if (allSalons.size == 0){
                                return@addOnSuccessListener
                            }

                            val adapter = AdapterSalonHomeNearBy(salonList)

                            room.nearBySalonDao().delete()

                            for (i in 0..9) {

                                if (i < allSalons.size) {
                                    salonList.add(
                                        ModelSalonHomeNearBy(
                                            allSalons[i].salonName,
                                            allSalons[i].imageUrl,
                                            allSalons[i].rating,
                                            allSalons[i].services,
                                            allSalons[i].distance,
                                            allSalons[i].id)
                                    )
                                    adapter.notifyItemInserted(salonList.size - 1)
                                }

                                if (i < allSalons.size && i < 4) {
                                    CoroutineScope(IO)
                                        .launch {
                                            try {
                                                room.nearBySalonDao()
                                                    .insertWishlist(
                                                        NearBySalonEntity(
                                                            allSalons[i].salonName,
                                                            allSalons[i].imageUrl,
                                                            allSalons[i].rating,
                                                            allSalons[i].services,
                                                            allSalons[i].distance,
                                                            allSalons[i].id)
                                                    )
                                            } catch (e: Exception) {
                                                room.nearBySalonDao()
                                                    .updateWishlist(
                                                        NearBySalonEntity(
                                                            allSalons[i].salonName,
                                                            allSalons[i].imageUrl,
                                                            allSalons[i].rating,
                                                            allSalons[i].services,
                                                            allSalons[i].distance,
                                                            allSalons[i].id))
                                            }
                                        }
                                }
                            }

                            binding.rvNearBySalons.adapter = adapter
                        }

                    } catch (e : Exception) {}
                }
            }
    }

    inner class AdapterServicesHome(private val list: ArrayList<ModelServices>, private val navController: NavController) : RecyclerView.Adapter<AdapterServicesHome.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemServicesHomeBinding: RowItemServicesHomeBinding) : RecyclerView.ViewHolder(itemView){
            val marginLayoutParam = itemView.layoutParams as ViewGroup.MarginLayoutParams
            val stageWidth  = Resources.getSystem().displayMetrics.widthPixels
            fun setMargin(){
                marginLayoutParam.setMargins(12,12,12,12)

                val lp = itemView.layoutParams
                lp.height = (stageWidth - 96)/ 4
                lp.width = (stageWidth - 96)/ 4
            }


        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterServicesHome.ServiceHolder {
            val rowItemServicesHomeBinding =
                RowItemServicesHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemServicesHomeBinding.root, rowItemServicesHomeBinding)
        }

        override fun onBindViewHolder(holder: AdapterServicesHome.ServiceHolder, position: Int) {

             holder.rowItemServicesHomeBinding.serviceName.typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
             holder.rowItemServicesHomeBinding.serviceName.text = list[position].serviceName
             holder.rowItemServicesHomeBinding.imgService.load(list[position].imageUrl)

             holder.itemView.setOnClickListener {
                 val args = Bundle()
                 args.putString(SERVICE_NAME, list[position].serviceName)
                 args.putString(SERVICE_ID, list[position].id)
                 navController.navigate(R.id.salonsByServicesFilter, args)
             }

            holder.itemView.setOnLongClickListener {
                wishList(list[position].id, holder)
                false
            }
            holder.setMargin()

        }

        private fun wishList(serviceId: String, holder : RecyclerView.ViewHolder) {

            var isLiked: Boolean

            val w = room.wishlistDao().getWishListById(serviceId)

            isLiked = w.isNotEmpty()

            val menu = PopupMenu(holder.itemView.context, holder.itemView)
            menu.inflate(R.menu.wish_list_pop_up)

            if (isLiked) {
                menu.menu.getItem(0).icon = ContextCompat.getDrawable(holder.itemView.context,R.drawable.ic_baseline_favorite_24)
                menu.menu.getItem(0).title = "Remove from Wishlist"
            } else {
                menu.menu.getItem(0).icon = ContextCompat.getDrawable(holder.itemView.context,R.drawable.ic_baseline_favorite_border_24)
                menu.menu.getItem(0).title = "Add to Wishlist"
            }

            menu.setOnMenuItemClickListener {
                if (it.itemId == R.id.addToWishList) {
                    if (isLiked) {
                        menu.dismiss()
                        CoroutineScope(IO)
                            .launch {
                                try {
                                    room.wishlistDao().delete(serviceId)
                                } catch (e: Exception) {}
                            }
                        db.collection(AppConstaints.USER_COLLECTION)
                            .document(Utils.mUid(mContext))
                            .collection(AppConstaints.COLLECTION_WISH_LIST)
                            .document(serviceId)
                            .delete()
                    } else {

                        drawFavAnim(mContext, binding.root, COLOR_RED)

                        menu.dismiss()

                        val dataType = WishList.ITEM_TYPE_SERVICE
                        val timestamp = FieldValue.serverTimestamp()

                        CoroutineScope(IO)
                            .launch {
                                try {
                                    room.wishlistDao().insertWishlist(
                                        WishlistEntity(
                                            Date().time,
                                            serviceId,
                                            serviceId,
                                            dataType,
                                            serviceId
                                        )
                                    )
                                } catch (e: Exception) {
                                    room.wishlistDao().updateWishlist(
                                        WishlistEntity(
                                            Date().time,
                                            serviceId,
                                            serviceId,
                                            dataType,
                                            serviceId
                                        )
                                    )
                                }
                            }

                        val map = HashMap<String, Any>()
                        map[AppConstaints.SERVICE_ID] = serviceId
                        map[AppConstaints.SALON_ID] = ""
                        map[AppConstaints.DATA_TYPE] = dataType
                        map[AppConstaints.TIMESTAMP_FIELD] = timestamp

                        db.collection(AppConstaints.USER_COLLECTION)
                            .document(Utils.mUid(mContext))
                            .collection(AppConstaints.COLLECTION_WISH_LIST)
                            .document(serviceId)[map] = SetOptions.merge()
                    }
                    isLiked = !isLiked
                }
                false
            }

            try {
                val fields: Array<Field> = menu.javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper: Any = field.get(menu)
                        val classPopupHelper = Class.forName(
                            menuPopupHelper
                                .javaClass.name
                        )
                        val setForceIcons: Method = classPopupHelper.getMethod(
                            "setForceShowIcon", Boolean::class.javaPrimitiveType
                        )
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: java.lang.Exception) { }

            menu.show()

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    inner class ModelServices(val serviceName : String, val imageUrl : String, val id : String)

    inner class AdapterSalonHome(private val list: ArrayList<ModelSalonHome>) : RecyclerView.Adapter<AdapterSalonHome.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemSalonHomeBinding: RowItemSalonsHomeBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterSalonHome.ServiceHolder {
            val rowItemSalonHomeBinding =
                RowItemSalonsHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemSalonHomeBinding.root, rowItemSalonHomeBinding)
        }

        override fun onBindViewHolder(holder: AdapterSalonHome.ServiceHolder, position: Int) {

             val typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
             holder.rowItemSalonHomeBinding.salonName.typeface = typeface
             holder.rowItemSalonHomeBinding.salonName.text = list[position].salonName
             holder.rowItemSalonHomeBinding.services.typeface = typeface
             holder.rowItemSalonHomeBinding.services.text = list[position].services.substring(0, 10)+"..."
             holder.rowItemSalonHomeBinding.rating.rating = list[position].rating
             holder.rowItemSalonHomeBinding.imageView.load(list[position].imageUrl)

             holder.itemView.setOnClickListener {
                 val args = Bundle()
                 args.putString(SALON_ID, list[position].id)
                 findNavController().navigate(R.id.salonDetails, args)
             }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    inner class ModelSalonHome(val salonName : String, val imageUrl : String, val rating : Float, val services : String, val themeColor : String , val id : String)

    inner class AdapterSalonHomeNearBy(private val list: ArrayList<ModelSalonHomeNearBy>) : RecyclerView.Adapter<AdapterSalonHomeNearBy.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemSalonNearbyBinding: RowItemNearbySalonsHomeBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterSalonHomeNearBy.ServiceHolder {
            val rowItemSalonNearbyBinding =
                RowItemNearbySalonsHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemSalonNearbyBinding.root, rowItemSalonNearbyBinding)
        }

        override fun onBindViewHolder(holder: AdapterSalonHomeNearBy.ServiceHolder, position: Int) {

            val typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
             holder.rowItemSalonNearbyBinding.salonName.typeface = typeface
             holder.rowItemSalonNearbyBinding.salonName.text = list[position].salonName
             holder.rowItemSalonNearbyBinding.services.typeface = typeface
             holder.rowItemSalonNearbyBinding.services.text = if (list[position].services.length >= 10) list[position].services.substring(0, 10)+"..." else list[position].services
             holder.rowItemSalonNearbyBinding.rating.rating = list[position].rating
             holder.rowItemSalonNearbyBinding.imageView.load(list[position].imageUrl)

            holder.rowItemSalonNearbyBinding.distance.text = getFormattedFloatInString(list[position].distance.toFloat(), 2).plus("Km...")

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(SALON_ID, list[position].id)
                findNavController().navigate(R.id.salonDetails, args)
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    inner class ModelSalonHomeNearBy(val salonName : String, val imageUrl : String, val rating : Float, val services : String ,  val distance : Double , val id : String)

    private class AdapterExitDialogViews(val list: ArrayList<NearBySalonEntity>, val exitDialogView: ExitDialogView): RecyclerView.Adapter<AdapterExitDialogViews.Viewholder>() {

        class Viewholder(itemView: View, val dBinding: ExitDialogSalonItemsBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): Viewholder {
            val dBinding = ExitDialogSalonItemsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return Viewholder(dBinding.root, dBinding)
        }

        override fun onBindViewHolder(holder: Viewholder, position: Int) {
            try {
                holder.dBinding.title.text = list[position].salonName
                holder.dBinding.distance.text = getFormattedFloatInString(list[position].distance.toFloat(), 2).plus("Km...")
                holder.dBinding.imageView.load(list[position].imageUrl)
                exitDialogView.exitDialogView(holder.itemView, list[position].id)

            } catch (e: Exception) {}
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }
    interface ExitDialogView {
        fun exitDialogView(itemView: View, id: String)
    }
}