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
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentLikedGalleryPhotosBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.Utils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.*

class LikedGalleryPhotos : Fragment() {

    private lateinit var mContext: Context
    private lateinit var binding: FragmentLikedGalleryPhotosBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLikedGalleryPhotosBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadData()

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.likedGalleryPhotos, true)
        }

    }

    private fun loadData() {

        binding.rv.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        val list = ArrayList<ModelGallery>()
        val adapter = AdapterGallery(list)

        val galleryRef = db.collection(AppConstaints.COLLECTION_GALLERY)

        galleryRef.orderBy(AppConstaints.DEFAULT_ORDER_FIELD, Query.Direction.DESCENDING)
            .get().addOnSuccessListener {
                for ((i,d) in it.withIndex()) {
                    try {
                        val ref = galleryRef.document(d.id)
                            .collection(AppConstaints.COLLECTION_LIKED_USERS)
                            .document(Utils.mUid(mContext))
                        ref.get().addOnSuccessListener { u ->
                            if (u.exists()) {
                                list.add(
                                    ModelGallery(
                                        d[AppConstaints.IMAGE_URL].toString(),
                                        d[SALON_ID].toString(),
                                        ref,
                                        d.id
                                    )
                                )
                                adapter.notifyItemInserted(list.size - 1)
                                binding.pb.visibility = View.GONE
                                if (i == it.size() - 1) {
                                    if (list.size == 0) {
                                        binding.noData.visibility = View.VISIBLE
                                    } else {
                                        binding.noData.visibility = View.GONE
                                    }
                                }
                            }
                        }

                    } catch (e: Exception) {
                    }
                }
            }

        binding.rv.adapter = adapter
    }

    inner class AdapterGallery(private val list: ArrayList<ModelGallery>) :
        RecyclerView.Adapter<AdapterGallery.GalleryHolder>() {

        inner class GalleryHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): GalleryHolder {
            return GalleryHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_item_gallery, parent, false)
            )
        }

        override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
            val image = holder.itemView.findViewById<ImageView>(R.id.imageView)
            val fav = holder.itemView.findViewById<ImageView>(R.id.favourite)
            val shimmer = holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmerEffect)

            shimmer.startShimmer()

            Glide.with(holder.itemView.context)
                .load(list[position].imageUrl)
                .listener(object : RequestListener<Drawable> {
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
                args.putString(AppConstaints.SALON_ID, list[position].salonId)
                args.putBoolean(OpenPhoto.IS_SHOW_MORE_BUTTON, true)
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

    inner class ModelGallery(val imageUrl: String, val salonId : String, val ref: DocumentReference, id: String)

}