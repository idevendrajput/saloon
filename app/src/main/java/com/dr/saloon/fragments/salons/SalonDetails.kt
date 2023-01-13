package com.dr.saloon.fragments.salons

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.dr.saloon.R
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.database.WishlistEntity
import com.dr.saloon.databinding.*
import com.dr.saloon.fragments.gallery.OpenPhoto
import com.dr.saloon.fragments.wishlist.WishList.Companion.ITEM_TYPE_SALON
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.APP_VERSION
import com.dr.saloon.utils.AppConstaints.COLLECTION_GALLERY
import com.dr.saloon.utils.AppConstaints.COLLECTION_LIKED_USERS
import com.dr.saloon.utils.AppConstaints.COLLECTION_REVIEWS
import com.dr.saloon.utils.AppConstaints.COLLECTION_REVIEW_REPLIES
import com.dr.saloon.utils.AppConstaints.COLLECTION_SALON
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLLECTION_WISH_LIST
import com.dr.saloon.utils.AppConstaints.COLOR_GREEN
import com.dr.saloon.utils.AppConstaints.COLOR_PRIMARY_1
import com.dr.saloon.utils.AppConstaints.COLOR_RED
import com.dr.saloon.utils.AppConstaints.COLOR_SECONDARY_1
import com.dr.saloon.utils.AppConstaints.DATA_TYPE
import com.dr.saloon.utils.AppConstaints.DESCRIPTION
import com.dr.saloon.utils.AppConstaints.EMAIL
import com.dr.saloon.utils.AppConstaints.FEMALE
import com.dr.saloon.utils.AppConstaints.FIELD_RATING
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.LATITUDE
import com.dr.saloon.utils.AppConstaints.LONGITUDE
import com.dr.saloon.utils.AppConstaints.MALE
import com.dr.saloon.utils.AppConstaints.MESSAGE
import com.dr.saloon.utils.AppConstaints.PHONE
import com.dr.saloon.utils.AppConstaints.PROFILE_URL
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SALON_TIMING
import com.dr.saloon.utils.AppConstaints.SALON_TYPE
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.dr.saloon.utils.AppConstaints.SERVICE_COST
import com.dr.saloon.utils.AppConstaints.SERVICE_ID
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.STARTING_PRICE
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.UNISEX
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.AppConstaints.USER_ID
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getCurrentMinuteOfDay
import com.dr.saloon.utils.Utils.Companion.getFormattedTimeWithHourAndMinute
import com.dr.saloon.utils.Utils.Companion.getScreenWidth
import com.dr.saloon.utils.Utils.Companion.mUid
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.HashMap

class SalonDetails : Fragment() {

    private lateinit var binding: FragmentSalonDetailsBinding
    private lateinit var reviewBinding: LayoutCustomerReviewsBinding
    private lateinit var mContext: Context
    private var themeColor = COLOR_SECONDARY_1
    private var sliderHandler: Handler = Handler(Looper.getMainLooper())
    private lateinit var viewPager2: ViewPager2
    private lateinit var sliderRunnable: Runnable
    private var db = FirebaseFirestore.getInstance()
    private lateinit var salonId: String

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSalonDetailsBinding.inflate(layoutInflater)
        reviewBinding = LayoutCustomerReviewsBinding.bind(binding.root)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            salonId = arguments?.getString(SALON_ID).toString()
            binding.mainLayout.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(themeColor))
            viewPager2 = binding.viewPager2
            actions()
            setViewPagerHeight()
        } catch (e: Exception) {
        }
    }

    private fun setViewPagerHeight() {
        val lp = viewPager2.layoutParams as ConstraintLayout.LayoutParams
        lp.height = (getScreenWidth() / 1.5).toInt()
    }

    override fun onResume() {
        super.onResume()
        try {
            gallery()
            review()
            updateUi()
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rvGallery.removeAllViewsInLayout()
            reviewBinding.rvReviews.removeAllViewsInLayout()
            binding.rvServices.removeAllViewsInLayout()
            binding.viewPager2.removeAllViewsInLayout()
            sliderHandler.removeCallbacks(sliderRunnable)
        } catch (e : Exception) {}
    }

    private fun updateUi() {

        db.collection(COLLECTION_SALON)
            .document(salonId)
            .get().addOnSuccessListener {

               try {

                val salonName = it[SALON_NAME].toString()
                val averageRating = it[FIELD_RATING].toString().toFloat()
                val latitude = it[LATITUDE].toString().toDouble()
                val longitude = it[LONGITUDE].toString().toDouble()
                val images = it[IMAGE_URL] as ArrayList<String>
                val services = it[SERVICES] as ArrayList<String>
                val description = it[DESCRIPTION].toString()
                val salonType = it[SALON_TYPE].toString()
                val email = it[EMAIL].toString()
                val phone = it[PHONE].toString()

                binding.direction.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.apply {
                        data = Uri.parse("https://maps.google.com?q=$latitude,$longitude")
                        `package` = "com.google.android.apps.maps"
                    }
                    startActivity(intent)
                }

                binding.txtServicesOfferedBy.text = "Services offered by\n$salonName"
                binding.address.text = getFullAddress(latitude, longitude)
                binding.email.text = email
                binding.phone.text = phone
                binding.ratingAverage.rating = averageRating
                binding.moreDetails.text = description
                binding.txtGallery.text = (salonName.split(' ')[0]) + "'s Gallery"
                binding.txtMoreDetail.text = "More About $salonName"

                slider(images)

                services(services)

                when (salonType) {
                    MALE -> {
                        binding.salonType.text = "Men"
                        binding.salonType.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.man_16,
                            0,
                            0,
                            0
                        )
                    }
                    FEMALE -> {
                        binding.salonType.text = "Women"
                        binding.salonType.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.woman_16,
                            0,
                            0,
                            0
                        )
                    }
                    UNISEX -> {
                        binding.salonType.text = "Unisex"
                        binding.salonType.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.unisex_16,
                            0,
                            0,
                            0
                        )
                    }
                }

                binding.viewAllServices.setOnClickListener {
                    val args = Bundle()
                    args.putStringArrayList(SERVICES, services)
                    args.putString(SALON_ID, salonId)
                    args.putString(SALON_TYPE, salonType)
                    args.putString(SALON_NAME, salonName)
                    findNavController().navigate(R.id.allServiceOfSalon, args)
                }

                binding.bookAppointment.setOnClickListener {
                    val args = Bundle()
                    args.putStringArrayList(SERVICES, services)
                    args.putString(SALON_ID, salonId)
                    args.putString(SALON_TYPE, salonType)
                    args.putString(SALON_NAME, salonName)
                    findNavController().navigate(R.id.allServiceOfSalon, args)
                }

                val timing = it[SALON_TIMING] as ArrayList<String>

                var openOn = timing[0]
                var closeOn = timing[1]

                openOn = if (openOn.toInt()  >= 720) {
                    getFormattedTimeWithHourAndMinute(openOn.toInt() - 720).plus("PM")
                } else {
                    getFormattedTimeWithHourAndMinute(openOn.toInt()).plus("AM")
                }

                closeOn = if (closeOn.toInt()  >= 720) {
                    getFormattedTimeWithHourAndMinute(closeOn.toInt() - 720).plus("PM")
                } else {
                    getFormattedTimeWithHourAndMinute(closeOn.toInt()).plus("AM")
                }

                val time = "$openOn - $closeOn"

                if (timing[1].toInt() < getCurrentMinuteOfDay()) {
                    binding.timing.setTextColor(Color.parseColor(COLOR_RED))
                } else {
                    binding.timing.setTextColor(Color.parseColor(COLOR_GREEN))
                }
                binding.timing.text = if (timing[1].toInt() > getCurrentMinuteOfDay()) "Open: $time" else "Closed: $time"



                } catch (e: Exception) {
                }
            }

        binding.bookAppointment.iconTint = ColorStateList.valueOf(Color.parseColor(themeColor))
        binding.bookAppointment.setTextColor(ColorStateList.valueOf(Color.parseColor(themeColor)))
        binding.bookAppointment.strokeColor = ColorStateList.valueOf(Color.parseColor(themeColor))

        val typeface = Typeface.createFromAsset(
            (activity as AppCompatActivity).assets,
            "fonts/futura-medium-bt.ttf"
        )
        binding.email.typeface = typeface
        binding.address.typeface = typeface
        binding.moreDetails.typeface = typeface
        binding.bookAppointment.typeface = typeface

        wishList()

    }

    private fun wishList() {

        var isLiked: Boolean

        val room = MyDatabase.getDatabase(mContext)

        val w = room.wishlistDao().getWishListById(salonId)

        isLiked = w.isNotEmpty()

        if (isLiked) {
            binding.addToWishList.setImageResource(R.drawable.favourite_red_32)
            binding.addToWishList.imageTintList = ColorStateList.valueOf(Color.parseColor(
                COLOR_PRIMARY_1))
        } else {
            binding.addToWishList.setImageResource(R.drawable.favourite_32)
            binding.addToWishList.imageTintList = ColorStateList.valueOf(Color.WHITE)
        }

        binding.addToWishList.setOnClickListener {
            if (isLiked) {
                binding.addToWishList.setImageResource(R.drawable.favourite_32)
                binding.addToWishList.imageTintList = ColorStateList.valueOf(Color.WHITE)
                CoroutineScope(IO)
                    .launch {
                    try {
                        room.wishlistDao().delete(salonId)
                    } catch (e: Exception) {}
                 }
                db.collection(USER_COLLECTION)
                    .document(mUid(mContext))
                    .collection(COLLECTION_WISH_LIST)
                    .document(salonId)
                    .delete()
            } else {

                Utils.drawFavAnim(mContext, binding.root, COLOR_PRIMARY_1)

                binding.addToWishList.setImageResource(R.drawable.favourite_red_32)
                binding.addToWishList.imageTintList = ColorStateList.valueOf(Color.parseColor(
                    COLOR_PRIMARY_1))

                val serviceId = ""
                val dataType =  ITEM_TYPE_SALON
                val timestamp = FieldValue.serverTimestamp()

                CoroutineScope(IO)
                    .launch {
                        try {
                            room.wishlistDao().insertWishlist(
                                WishlistEntity(
                                    Date().time,
                                    salonId,
                                    serviceId,
                                    dataType,
                                    salonId
                                )
                            )
                        } catch (e: Exception) {
                            room.wishlistDao().updateWishlist(
                                WishlistEntity(
                                    Date().time,
                                    salonId,
                                    serviceId,
                                    dataType,
                                    salonId
                                )
                            )
                        }
                    }

                val map = HashMap<String, Any>()
                map[SERVICE_ID] = serviceId
                map[SALON_ID] = salonId
                map[DATA_TYPE] = dataType
                map[TIMESTAMP_FIELD] = timestamp

                db.collection(USER_COLLECTION)
                    .document(mUid(mContext))
                    .collection(COLLECTION_WISH_LIST)
                    .document(salonId)[map] = SetOptions.merge()
            }
            isLiked = !isLiked
        }

    }

    private fun getFullAddress(lat: Double, lng: Double) : String {
        val geocoder = Geocoder(mContext, Locale.getDefault())
        var add = "Not Available"
        try {
            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val obj: Address = addresses[0]
            add = obj.getAddressLine(0)
        } catch (e: Exception) { }
        return add
    }

    private fun actions() {

        binding.nestedSv.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                binding.bookAppointment.visibility = View.GONE
            }
            if (scrollY < oldScrollY) {
                binding.bookAppointment.visibility = View.VISIBLE
            }
        }

        binding.viewMoreGallery.setOnClickListener {
            val args = Bundle()
            args.putString(SALON_ID, salonId)
            findNavController().navigate(R.id.salonsGallery, args)
        }

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.salonDetails, true)
        }

    }

    private fun gallery() {

        val galleryList = ArrayList<ModelGallery>()

        val galleryRef = db.collection(COLLECTION_GALLERY)
        val adapterGallery = AdapterGallery(galleryList)

        galleryRef.orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .whereEqualTo(SALON_ID, salonId)
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    binding.txtGallery.visibility = View.GONE
                    binding.rvGallery.visibility = View.GONE
                    binding.viewMoreGallery.visibility = View.GONE
                    return@addOnSuccessListener
                }
                binding.txtGallery.visibility = View.VISIBLE
                binding.rvGallery.visibility = View.VISIBLE
                binding.viewMoreGallery.visibility = View.VISIBLE

                for (d in it) {
                    val ref = galleryRef.document(d.id).collection(COLLECTION_LIKED_USERS)
                        .document(mUid(mContext))
                    galleryList.add(
                        ModelGallery(
                            d[IMAGE_URL].toString(),
                            d[SALON_ID].toString(), ref
                        )
                    )
                    adapterGallery.notifyItemInserted(galleryList.size - 1)
                }
            }

        binding.rvGallery.adapter = adapterGallery

    }

    private fun updateRating() {

        db.collection(COLLECTION_SALON)
            .document(salonId)
            .collection(COLLECTION_REVIEWS)
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    return@addOnSuccessListener
                }
                var ratingCount = 0f
                for (i in it) {
                    ratingCount += try {
                        i[FIELD_RATING].toString().toFloat()
                    } catch (e: Exception) { 0f }
                }
                val rating = ratingCount / it.size()

                val map = HashMap<String, Any>()
                map[FIELD_RATING] = rating
                db.collection(COLLECTION_SALON)
                    .document(salonId)[map] = SetOptions.merge()
            }
    }

    private fun services(services: ArrayList<String>) {

        val services1 = ArrayList<ModelSplitService>()

        for (i in 0 until services.size) {
            val s = services[i].split('?')
            services1.add(
                ModelSplitService(
                    try {
                        s[0]
                    } catch (e: Exception) {
                        "Not Found"
                    }, try {
                        s[1]
                    } catch (e: Exception) {
                        "Not Found"
                    },
                    try {
                        s[2]
                    } catch (e: Exception) {
                        "0"
                    }
                )
            )
        }

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServices(serviceList)

        for (i in 0 until services1.size) {
            if (i <= 9) {
                db.collection(COLLECTION_SERVICES)
                    .document(services1[i].serviceId)
                    .get().addOnSuccessListener { d ->
                        try {
                            if (d.exists()) {
                                serviceList.add(
                                    ModelServices(
                                        d[SERVICE_NAME].toString(),
                                        d[IMAGE_URL].toString(),
                                        services1[i].price.toInt(),
                                        d.id
                                    )
                                )
                                adapterServices.notifyItemInserted(serviceList.size - 1)
                            }
                        } catch (e: Exception) {
                        }
                    }
            }
        }

        binding.rvServices.adapter = adapterServices

        Collections.sort(services1, Comparator<ModelSplitService?> { o1, o2 ->
            (o1?.price?.toInt() as Int).compareTo((o2?.price?.toInt() as Int))
        } as Comparator<ModelSplitService?>?)

        if (services1.size > 0) {
            val startingPrice = services1[0].price
            val map = HashMap<String, Any>()
            map[STARTING_PRICE] = startingPrice
            db.collection(COLLECTION_SALON)
                .document(salonId)[map] = SetOptions.merge()
        }



    }

    private fun slider(imagesArray: ArrayList<String>) {

        try {
            viewPager2 = binding.viewPager2

            val adapter = SliderAdapter(imagesArray)

            viewPager2.adapter = adapter
            viewPager2.clipToPadding = false
            viewPager2.clipChildren = false
            viewPager2.offscreenPageLimit = 3
            viewPager2.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

            sliderRunnable = Runnable {
                if (viewPager2.currentItem == imagesArray.size - 1) {
                    viewPager2.setCurrentItem(0, true)
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
        } catch (e: Exception) { }
    }

    private fun review() {

        updateRating()

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

        val ref = db.collection(COLLECTION_SALON)
            .document(salonId)
            .collection(COLLECTION_REVIEWS)

        ref.orderBy(FIELD_RATING, Query.Direction.DESCENDING)
            .limit(7)
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    reviewBinding.noData.visibility = View.GONE
                    return@addOnSuccessListener
                }
                for (d in it) {
                    try {
                        list.add(
                            ModelReviews(
                                d[USER_ID].toString(),
                                d[MESSAGE].toString(),
                                d[FIELD_RATING].toString().toFloat(),
                                d.id
                            )
                        )
                        adapterReviews.notifyItemInserted(list.size - 1)
                    } catch (e: Exception) {
                    }
                }
            }

        reviewBinding.rvReviews.adapter = adapterReviews

        addReview(ref, list, adapterReviews)

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

        if (userId == mUid(mContext)) {
            dBinding.etMessage.visibility = View.VISIBLE
            dBinding.send.visibility = View.VISIBLE
        } else {
            dBinding.etMessage.visibility = View.INVISIBLE
            dBinding.send.visibility = View.INVISIBLE
        }

        db.collection(COLLECTION_SALON)
            .document(salonId)
            .collection(COLLECTION_REVIEWS)
            .document(id)
            .collection(COLLECTION_REVIEW_REPLIES)
            .orderBy(TIMESTAMP_FIELD, Query.Direction.ASCENDING)
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    dBinding.noData.visibility = View.VISIBLE
                    dBinding.pb.visibility = View.GONE
                    return@addOnSuccessListener
                }
                dBinding.pb.visibility = View.GONE
                for (d in it) {
                    list.add(ModelReviews(
                        d[USER_ID].toString(),
                        d[MESSAGE].toString(),
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
                    map[USER_ID] = mUid(mContext)
                    map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
                    map[MESSAGE] = dBinding.etMessage.text.toString().trim()

                    val dId = UUID.randomUUID().toString()

                    list.add(ModelReviews(
                        mUid(mContext),
                        dBinding.etMessage.text.toString().trim(),
                        0f,
                        dId
                    ))
                    dBinding.noData.visibility = View.GONE
                    dBinding.etMessage.setText("")

                    adapterReviews.notifyItemInserted(list.size - 1)

                    db.collection(COLLECTION_SALON)
                        .document(salonId)
                        .collection(COLLECTION_REVIEWS)
                        .document(id)
                        .collection(COLLECTION_REVIEW_REPLIES)
                        .document(dId)[map] = SetOptions.merge()

                }
            }
        }
    }

    private fun addReview(
        ref: CollectionReference,
        list: ArrayList<ModelReviews>,
        adapterReviews: AdapterReviews
    ) {

        ref.orderBy(FIELD_RATING).get().addOnSuccessListener {
            if (it.size() > 7) {
                reviewBinding.viewMore.visibility = View.VISIBLE
            }
        }

        reviewBinding.viewMore.setOnClickListener {
            val args = Bundle()
            args.putString(SALON_ID, salonId)
            findNavController().navigate(R.id.salonAllReview, args)
        }

        ref.whereEqualTo(USER_ID, mUid(mContext))
            .get().addOnSuccessListener {
                if (it.size() == 0) {
                    reviewBinding.txtWriteAReview.visibility = View.VISIBLE
                    reviewBinding.rating.visibility = View.VISIBLE
                } else {
                    reviewBinding.etReview.visibility = View.GONE
                    reviewBinding.submitReview.visibility = View.GONE
                    reviewBinding.txtWriteAReview.visibility = View.GONE
                    reviewBinding.rating.visibility = View.GONE
                }
            }


        var givenRating = 0f

        reviewBinding.rating.setOnRatingBarChangeListener { _, rating, _ ->
            givenRating = rating
            Log.d("rating","-->"+rating)
            reviewBinding.etReview.visibility = View.VISIBLE
            reviewBinding.submitReview.visibility = View.VISIBLE

        }

        reviewBinding.submitReview.setOnClickListener {
            if (reviewBinding.etReview.text.toString().trim().isEmpty()) {
                reviewBinding.etReview.error = "Please write a review"
                return@setOnClickListener
            }

            val dId = UUID.randomUUID().toString()

            val pInfo: PackageInfo =
                (activity as AppCompatActivity).packageManager.getPackageInfo(
                    (activity as AppCompatActivity).packageName,
                    0
                )
            val version = pInfo.versionName

            val map = HashMap<String, Any>()
            map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
            map[USER_ID] = mUid(mContext)
            map[APP_VERSION] = version
            map[FIELD_RATING] = givenRating
            map[MESSAGE] = reviewBinding.etReview.text.toString().trim()

            list.add(
                ModelReviews(
                    mUid(mContext),
                    reviewBinding.etReview.text.toString().trim(),
                    givenRating,
                    dId
                )
            )

            adapterReviews.notifyItemInserted(list.size - 1)

            val executor = Executors.newSingleThreadExecutor()

            executor.execute {
                ref.document(dId)[map] = SetOptions.merge()
            }

            Snackbar.make(
                binding.root,
                "Thanks for your review \uD83D\uDE0D",
                Snackbar.LENGTH_SHORT
            ).show()

            reviewBinding.etReview.visibility = View.GONE
            reviewBinding.submitReview.visibility = View.GONE
            reviewBinding.txtWriteAReview.visibility = View.GONE
            reviewBinding.rating.visibility = View.GONE

        }

    }

    private inner class SliderAdapter(private val sliderItems: ArrayList<String>) :
        RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
            return SliderViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.row_item_slider_salon_details, parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: SliderViewHolder, i: Int) {
            val img = holder.itemView.findViewById<ImageView>(R.id.imageView)
            img.load(sliderItems[i])
            holder.itemView.findViewById<View>(R.id.view).backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(themeColor))
            if (i == sliderItems.size - 2) {
                viewPager2.post(runnable)
            }
        }

        override fun getItemCount(): Int {
            return sliderItems.size
        }

        private val runnable = Runnable {
            sliderItems.addAll(sliderItems)
            notifyDataSetChanged()
        }

        inner class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private inner class AdapterGallery(val galleryList: ArrayList<ModelGallery>) :
        RecyclerView.Adapter<AdapterGallery.GalleryHolder>() {

        inner class GalleryHolder(
            itemView: View,
            val rowItemGallerySalonDetailBinding: RowItemGallerySalonDetailBinding
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GalleryHolder {
            val rowItemGallerySalonDetailBinding = RowItemGallerySalonDetailBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return GalleryHolder(
                rowItemGallerySalonDetailBinding.root,
                rowItemGallerySalonDetailBinding
            )
        }

        override fun onBindViewHolder(holder: GalleryHolder, i: Int) {

            holder.rowItemGallerySalonDetailBinding.imageView.load(galleryList[i].imageUrl)

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(IMAGE_URL, galleryList[i].imageUrl)
                args.putString(SALON_ID, galleryList[i].salonId)
                args.putBoolean(OpenPhoto.IS_SHOW_MORE_BUTTON, false)
                findNavController().navigate(R.id.openPhoto,args)
            }

            val fav = holder.rowItemGallerySalonDetailBinding.favourite
            var isLiked: Boolean

            val favRef = galleryList[i].ref

            favRef.get().addOnSuccessListener {
                isLiked = if (it.exists()) {
                    fav.setImageResource(R.drawable.star_gold_128)
                    true
                } else {
                    fav.setImageResource(R.drawable.star_stroke_128)
                    false
                }

                fav.setOnClickListener {
                    if (isLiked) {
                        fav.setImageResource(R.drawable.star_stroke_128)
                        removeFromFav(favRef)
                    } else {
                        fav.setImageResource(R.drawable.star_gold_128)
                        addToFav(favRef)
                    }
                    isLiked = !isLiked
                }
            }
        }

        private fun removeFromFav(ref: DocumentReference) {
            ref.delete()
        }

        private fun addToFav(ref: DocumentReference) {
            val map = HashMap<String, Any>()
            map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
            map[USER_ID] = mUid(mContext)
            ref[map] = SetOptions.merge()
        }

        override fun getItemCount(): Int {
            return galleryList.size
        }


    }

    private inner class ModelGallery(val imageUrl: String, val salonId : String, val ref: DocumentReference)

    private inner class AdapterServices(private val list: ArrayList<ModelServices>) :
        RecyclerView.Adapter<AdapterServices.ServiceHolder>() {

        inner class ServiceHolder(
            itemView: View,
            val rowItemSalonInsideHorizontalBinding: RowItemSalonInsideHorizontalBinding
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemSalonInsideHorizontalBinding =
                RowItemSalonInsideHorizontalBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ServiceHolder(
                rowItemSalonInsideHorizontalBinding.root,
                rowItemSalonInsideHorizontalBinding
            )
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {
            holder.rowItemSalonInsideHorizontalBinding.serviceName.typeface =
                Typeface.createFromAsset(
                    (activity as AppCompatActivity).assets,
                    "fonts/futura-medium-bt.ttf"
                )
            holder.rowItemSalonInsideHorizontalBinding.serviceName.text = list[position].serviceName
            holder.rowItemSalonInsideHorizontalBinding.price.text = "₹ ${list[position].price}"
            holder.rowItemSalonInsideHorizontalBinding.imageView.load(list[position].imageUrl)
            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(SALON_ID, salonId)
                args.putString(SERVICE_ID, list[position].id)
                args.putString(SERVICE_NAME, list[position].serviceName)
                args.putString(SERVICE_COST, list[position].price.toString())
                findNavController().navigate(R.id.bookAppointment, args)
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private inner class AdapterReviews(private val list: ArrayList<ModelReviews>, val salonId: String, val reviewView: ReviewView) :
        RecyclerView.Adapter<AdapterReviews.ServiceHolder>() {

        inner class ServiceHolder(
            itemView: View,
            val rowItemCustomerReviewBinding: RowItemReviewsBinding
        ) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemCustomerReviewBinding =
                RowItemReviewsBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            return ServiceHolder(rowItemCustomerReviewBinding.root, rowItemCustomerReviewBinding)
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {

            val typeface = Typeface.createFromAsset(
                (activity as AppCompatActivity).assets,
                "fonts/futura-medium-bt.ttf"
            )
            holder.rowItemCustomerReviewBinding.userName.typeface = typeface
            holder.rowItemCustomerReviewBinding.txtRating.typeface = typeface
            holder.rowItemCustomerReviewBinding.message.text = list[position].message
            holder.rowItemCustomerReviewBinding.txtRating.text = list[position].rating.toString()
            holder.rowItemCustomerReviewBinding.rating.rating = list[position].rating

            reviewView.reviewView(holder.rowItemCustomerReviewBinding.replies,
                list[position].id,
                holder.rowItemCustomerReviewBinding.rating,
                holder.rowItemCustomerReviewBinding.txtRating,
                holder.itemView, list[position].userId)

            db.collection(USER_COLLECTION)
                .document(list[position].userId)
                .get().addOnSuccessListener {
                    try {
                        if (it.exists()) {
                            val name = it[USER_NAME].toString()
                            val profileUrl = it[PROFILE_URL].toString()
                            holder.rowItemCustomerReviewBinding.userName.text = name
                            holder.rowItemCustomerReviewBinding.profile.load(profileUrl)
                        } else {
                            val name = "User"
                            holder.rowItemCustomerReviewBinding.userName.text = name
                        }
                    } catch (e: Exception) {
                    }
                }

             db.collection(COLLECTION_SALON)
                .document(salonId)
                .collection(COLLECTION_REVIEWS)
                .document(list[position].id)
                .collection(COLLECTION_REVIEW_REPLIES)
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

    interface ReviewView {
        fun reviewView(reply: TextView, id: String, ratingBar: RatingBar, txtRating: TextView, itemView: View, userId: String)
    }

    private inner class ModelServices(
        val serviceName: String,
        val imageUrl: String,
        val price: Int,
        val id: String
    )

    private inner class ModelSplitService(
        val serviceName: String,
        val serviceId: String,
        val price: String
    )

    private inner class ModelReviews(
        val userId : String,
    val message: String,
    val rating : Float,
    val id : String
    )

}