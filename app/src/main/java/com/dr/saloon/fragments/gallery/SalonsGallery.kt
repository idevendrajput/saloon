package com.dr.saloon.fragments.gallery

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentSalonGalleryBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.firestore.*
import java.util.HashMap

class SalonsGallery : Fragment() {

    private lateinit var binding : FragmentSalonGalleryBinding
    private lateinit var salonId : String
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
        binding = FragmentSalonGalleryBinding.inflate(layoutInflater)
        salonId = arguments?.getString(SALON_ID).toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        gallery()
        actions()
    }

    private fun actions() {
        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.salonsGallery, true)
        }
    }

    private fun gallery() {

        val galleryList = ArrayList<ModelGallery>()

        val galleryRef = db.collection(AppConstaints.COLLECTION_GALLERY)
        val adapterGallery = AdapterGallery(galleryList)

        galleryRef.orderBy(AppConstaints.TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .whereEqualTo(SALON_ID, salonId)
            .get().addOnSuccessListener {
                for (d in it) {
                    val ref = galleryRef.document(d.id).collection(AppConstaints.COLLECTION_LIKED_USERS)
                        .document(Utils.mUid(mContext))
                    galleryList.add(
                        ModelGallery(
                            d[AppConstaints.IMAGE_URL].toString(),
                            d[SALON_ID].toString(),
                            ref
                        )
                    )
                    adapterGallery.notifyItemInserted(galleryList.size - 1)
                    binding.pb.visibility = View.GONE
                }
            }
        binding.rv.adapter = adapterGallery

    }

    private inner class AdapterGallery(private val list : ArrayList<ModelGallery>) : RecyclerView.Adapter<AdapterGallery.GalleryHolder>() {

        inner class GalleryHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GalleryHolder {
            return GalleryHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_item_gallery, parent, false))
        }

        override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
            val image = holder.itemView.findViewById<ImageView>(R.id.imageView)
            val fav = holder.itemView.findViewById<ImageView>(R.id.favourite)
            val shimmer = holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmerEffect)

            shimmer.startShimmer()

            Glide.with(holder.itemView.context)
                .load(list[position].imageUrl)
                .listener(object : RequestListener<Drawable>{
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        shimmer.stopShimmer()
                        shimmer.visibility = View.GONE
                        return true
                    }
                })
                .into(image)

            image.load(list[position].imageUrl)

            var isLiked: Boolean
            val favRef = list[position].ref
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

            val marginLayoutParams = view!!.layoutParams as ViewGroup.MarginLayoutParams
            marginLayoutParams.setMargins(1, 1, 1, 1)

            image.maxHeight = (Utils.getScreenWidth() / 2) - 4
            image.maxWidth = (Utils.getScreenWidth() / 2) - 4

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(AppConstaints.IMAGE_URL, list[position].imageUrl)
                args.putString(SALON_ID, list[position].salonId)
                args.putBoolean(OpenPhoto.IS_SHOW_MORE_BUTTON, false)
                findNavController().navigate(R.id.openPhoto,args)
            }
        }

        private fun removeFromFav(ref: DocumentReference) {
            ref.delete()
        }

        private fun addToFav(ref: DocumentReference) {
            val map = HashMap<String, Any>()
            map[AppConstaints.TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
            map[AppConstaints.USER_ID] = Utils.mUid(mContext)
            ref[map] = SetOptions.merge()
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

    private inner class ModelGallery(val imageUrl : String,val salonId : String, val ref : DocumentReference)

}