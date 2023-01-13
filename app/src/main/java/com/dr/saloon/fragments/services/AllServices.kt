package com.dr.saloon.fragments.services

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.database.WishlistEntity
import com.dr.saloon.databinding.FragmentAllServicesBinding
import com.dr.saloon.databinding.RowItemServicesBinding
import com.dr.saloon.fragments.wishlist.WishList
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLOR_GRAY_LIGHT
import com.dr.saloon.utils.AppConstaints.COLOR_RED
import com.dr.saloon.utils.AppConstaints.COLOR_WHITE
import com.dr.saloon.utils.AppConstaints.FEMALE
import com.dr.saloon.utils.AppConstaints.MALE
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.SERVICE_TYPE
import com.dr.saloon.utils.AppConstaints.UNISEX
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.collapseWidthWithAnim
import com.dr.saloon.utils.Utils.Companion.expendWidthWithAnim
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*

class AllServices : Fragment() {

    private lateinit var mContext : Context
    private lateinit var binding : FragmentAllServicesBinding
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
        binding = FragmentAllServicesBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        servicesFemale()

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
            findNavController().popBackStack(R.id.allServices, true)
        }

        binding.search.setOnClickListener {
            if (binding.etSearch.visibility == View.GONE) {
                binding.etSearch.visibility = View.VISIBLE
            }
        }
    }

    private fun hideSideBar() {
        if (!isCollapse) {
            collapseWidthWithAnim(binding.collapseLayout, binding.collapseLayout.width, 30)
            for (i in 0 until  binding.collapseLayout.childCount){
                binding.collapseLayout.getChildAt(i).visibility = View.GONE
            }
        }
        isCollapse = true
    }

    private fun showSideBar() {
        if (isCollapse) {
            expendWidthWithAnim(binding.collapseLayout, binding.collapseLayout.width, collapseLayoutInitialWidth)
            for (i in 0 until  binding.collapseLayout.childCount){
                binding.collapseLayout.getChildAt(i).visibility = View.VISIBLE
            }
        }
        isCollapse = false
    }

    private fun servicesFemale() {

        sideBarItemSelection(0)

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServicesHome(serviceList)

        serviceList.clear()
        binding.rv.removeAllViews()
        binding.noData.visibility = View.GONE

        db.collection(COLLECTION_SERVICES)
            .orderBy(AppConstaints.PREFERENCE_SCORE, Query.Direction.DESCENDING)
            .whereEqualTo(SERVICE_TYPE, FEMALE)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.GONE
                    return@addOnSuccessListener
                }
                for (d in it) {
                    try {
                        serviceList.add(ModelServices(d[SERVICE_NAME].toString(),
                            d[AppConstaints.IMAGE_URL].toString(),
                            d.id))
                        adapterServices.notifyItemInserted(serviceList.size - 1)
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

    private fun servicesMale() {

        sideBarItemSelection(1)

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServicesHome(serviceList)

        serviceList.clear()
        binding.rv.removeAllViews()
        binding.noData.visibility = View.GONE

        db.collection(COLLECTION_SERVICES)
            .orderBy(AppConstaints.PREFERENCE_SCORE, Query.Direction.DESCENDING)
            .whereEqualTo(SERVICE_TYPE, MALE)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.GONE
                    return@addOnSuccessListener
                }
                for (d in it) {
                    try {
                        serviceList.add(ModelServices(d[SERVICE_NAME].toString(),
                            d[AppConstaints.IMAGE_URL].toString(),
                            d.id))
                        adapterServices.notifyItemInserted(serviceList.size - 1)
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

    private fun servicesUnisex() {

        sideBarItemSelection(2)

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServicesHome(serviceList)

        serviceList.clear()
        binding.rv.removeAllViews()
        binding.noData.visibility = View.GONE

        db.collection(COLLECTION_SERVICES)
            .orderBy(AppConstaints.PREFERENCE_SCORE, Query.Direction.DESCENDING)
            .whereEqualTo(SERVICE_TYPE, UNISEX)
            .get()
            .addOnSuccessListener {
                if (it.size() == 0) {
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.GONE
                    return@addOnSuccessListener
                }
                for (d in it) {
                    try {
                        serviceList.add(ModelServices(d[SERVICE_NAME].toString(),
                            d[AppConstaints.IMAGE_URL].toString(),
                            d.id))
                        adapterServices.notifyItemInserted(serviceList.size - 1)
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
            view.setTextColor(ColorStateList.valueOf(Color.parseColor(COLOR_GRAY_LIGHT)))
            view.setOnClickListener {
                when(i) {
                    0-> {
                        servicesFemale()
                        binding.title.text = "Services for\nWomen"
                        binding.genderLogo.setImageResource(R.drawable.woman_128)
                    }
                    1-> {
                        servicesMale()
                        binding.title.text = "Services for\nMen"
                        binding.genderLogo.setImageResource(R.drawable.man_128)
                    }
                    2-> {
                        servicesUnisex()
                        binding.title.text = "Common\nServices"
                        binding.genderLogo.setImageResource(R.drawable.unisex_128)
                    }
                }
                sideBarItemSelection(i)
            }
        }
        (v.getChildAt(selectedItemPosition) as TextView).setTextColor(ColorStateList.valueOf(Color.parseColor(
            COLOR_WHITE)))
    }

    private fun search(serviceType : String, s : String) {

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServicesHome(serviceList)

        db.collection(COLLECTION_SERVICES)
            .orderBy(AppConstaints.DEFAULT_ORDER_FIELD, Query.Direction.DESCENDING)
            .whereEqualTo(SERVICE_TYPE, serviceType)
            .get()
            .addOnSuccessListener {
                for (d in it) {
                    try {
                        if (d[SERVICE_NAME].toString().trim().replace(" ","").lowercase().contains(s.trim().replace(" ","").lowercase())){
                            serviceList.add(ModelServices(d[SERVICE_NAME].toString(),
                                d[AppConstaints.IMAGE_URL].toString(),
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

    inner class AdapterServicesHome(private val list: ArrayList<ModelServices>) : RecyclerView.Adapter<AdapterServicesHome.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemServicesGridBinding: RowItemServicesBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemServicesGridBinding =
                RowItemServicesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemServicesGridBinding.root, rowItemServicesGridBinding)
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {

            holder.rowItemServicesGridBinding.serviceName.typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
            holder.rowItemServicesGridBinding.serviceName.text = list[position].serviceName
            holder.rowItemServicesGridBinding.imgService.load(list[position].imageUrl)

            holder.itemView.setOnLongClickListener {
                wishList(list[position].id, holder)
                false
            }

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(SERVICE_NAME, list[position].serviceName)
                args.putString(AppConstaints.SERVICE_ID, list[position].id)
                findNavController().navigate(R.id.salonsByServicesFilter, args)
            }

        }

        private fun wishList(serviceId: String, holder : RecyclerView.ViewHolder) {

            var isLiked: Boolean

            val room = MyDatabase.getDatabase(mContext)

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
                        GlobalScope.launch {
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

                        Utils.drawFavAnim(mContext, binding.root, COLOR_RED)

                        menu.dismiss()

                        val dataType = WishList.ITEM_TYPE_SERVICE
                        val timestamp = FieldValue.serverTimestamp()

                        GlobalScope.launch {
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
            } catch (e: java.lang.Exception) {
            }

            menu.show()

        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    inner class ModelServices(val serviceName : String, val imageUrl : String, val id : String)


}