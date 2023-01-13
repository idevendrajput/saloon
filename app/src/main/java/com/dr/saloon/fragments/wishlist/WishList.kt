package com.dr.saloon.fragments.wishlist

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.database.MyDatabase
import com.dr.saloon.database.WishlistEntity
import com.dr.saloon.databinding.*
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.COLLECTION_SALON
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLLECTION_WISH_LIST
import com.dr.saloon.utils.AppConstaints.DATA_TYPE
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SERVICE_ID
import com.dr.saloon.utils.AppConstaints.SERVICE_NAME
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.Utils.Companion.mUid
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WishList : Fragment() {

    private lateinit var binding : FragementWishlistBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragementWishlistBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUi()
        actions()

        loadData()

    }

    private fun actions() {
        binding.back.setOnClickListener { findNavController().popBackStack(R.id.wishList,true) }
    }

    private fun updateUi() {

        binding.title.typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")

    }

    private fun loadData() {

        try {

            val room = MyDatabase.getDatabase(mContext)

            val list = room.wishlistDao().getAllWishList() as ArrayList<WishlistEntity>

            if (list.size == 0){
                binding.noData.visibility = View.VISIBLE
            }

            val adapter =  AdapterWishList(list, findNavController())

            binding.pb.visibility = View.GONE

            binding.rv.adapter = adapter

        } catch (e: Exception) { }

    }

    companion object {
        const val ITEM_TYPE_SERVICE = "Service"
        const val ITEM_TYPE_SALON = "Salon"
    }

    private class AdapterWishList(val list : ArrayList<WishlistEntity>, val navController: NavController) : RecyclerView.Adapter<AdapterWishList.WishlistHolder>() {

        val db = FirebaseFirestore.getInstance()

        class WishlistHolder(itemView: View, val rowItemWishlistBinding: RowItemWishlistBinding) : RecyclerView.ViewHolder(itemView) {

        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): WishlistHolder {
            val rowItemWishListBinding = RowItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return WishlistHolder(rowItemWishListBinding.root, rowItemWishListBinding)
        }

        override fun onBindViewHolder(holder: WishlistHolder, position: Int) {

            when (list[position].dataType) {
                ITEM_TYPE_SERVICE-> {
                   db.collection(COLLECTION_SERVICES)
                       .document(list[position].serviceId)
                       .get().addOnSuccessListener {
                           try {

                               val serviceName = it[SERVICE_NAME].toString()

                               holder.rowItemWishlistBinding.title.text = serviceName
                               holder.rowItemWishlistBinding.imageView.load(it[IMAGE_URL].toString())
                               holder.rowItemWishlistBinding.dataType.text = "In Services"

                               holder.itemView.setOnClickListener {
                                   val args = Bundle()
                                   args.putString(SERVICE_NAME, serviceName)
                                   args.putString(AppConstaints.SERVICE_ID, list[position].serviceId)
                                   navController.navigate(R.id.salonsByServicesFilter, args)
                               }

                           } catch (e: Exception) {}
                       }
                }
                ITEM_TYPE_SALON-> {
                    db.collection(COLLECTION_SALON)
                        .document(list[position].salonId)
                        .get().addOnSuccessListener {
                            try {

                                holder.rowItemWishlistBinding.title.text = it[SALON_NAME].toString()
                                holder.rowItemWishlistBinding.imageView.load((it[IMAGE_URL] as ArrayList<String>)[0])
                                holder.rowItemWishlistBinding.dataType.text = "In Salons"

                                holder.itemView.setOnClickListener {
                                    val args = Bundle()
                                    args.putString(AppConstaints.SALON_ID, list[position].salonId)
                                    navController.navigate(R.id.salonDetails, args)
                                }

                            } catch (e: Exception) {}
                        }
                }
            }

            holder.itemView.setOnLongClickListener {

                val deleteDialog = Dialog(holder.itemView.context)
                val dBinding = CustomAlertDialogBinding.inflate(deleteDialog.layoutInflater)
                deleteDialog.setContentView(dBinding.root)
                deleteDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                deleteDialog.show()

                dBinding.title.text = "Remove from Wishlist"
                dBinding.message.text = "Are you sure you want to remove this item from wishlist?"
                dBinding.img.setImageResource(R.drawable.delete_128)

                dBinding.positiveButton.setOnClickListener {

                    dBinding.message.text = "Please wait..."
                    dBinding.positiveButton.isEnabled = false

                    val room = MyDatabase.getDatabase(holder.itemView.context)

                    CoroutineScope(IO)
                        .launch {
                            room.wishlistDao().delete(list[position].id)
                        }

                    db.collection(USER_COLLECTION)
                        .document(mUid(holder.itemView.context))
                        .collection(COLLECTION_WISH_LIST)
                        .document(list[position].id)
                        .delete().addOnSuccessListener {
                            deleteDialog.dismiss()
                            list.removeAt(position)
                            notifyItemRemoved(position)
                        }
                }

                dBinding.negativeButton.setOnClickListener {
                    deleteDialog.dismiss()
                }
                false
            }

        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

}