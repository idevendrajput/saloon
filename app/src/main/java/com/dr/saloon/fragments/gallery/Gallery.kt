package com.dr.saloon.fragments.gallery

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
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
import com.dr.saloon.databinding.FragmentGalleryBinding
import com.dr.saloon.fragments.gallery.OpenPhoto.Companion.IS_SHOW_MORE_BUTTON
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.COLLECTION_GALLERY
import com.dr.saloon.utils.AppConstaints.DEFAULT_ORDER_FIELD
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.getScreenWidth
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.firestore.*

class Gallery : Fragment() {

    private lateinit var binding : FragmentGalleryBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private var isLoading = true

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(layoutInflater)
        val inflater1 = TransitionInflater.from(requireContext())
        exitTransition = inflater1.inflateTransition(R.transition.fade)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUi()
        actions()

    }

    override fun onResume() {
        super.onResume()
        try {
            loadData()
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            binding.rv.removeAllViewsInLayout()
        } catch (e: Exception) {}
    }

    private fun loadData() {

        binding.rv.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        val list = ArrayList<ModelGallery>()
        val adapter = AdapterGallery(list)

        val galleryRef = db.collection(COLLECTION_GALLERY)

        galleryRef.orderBy(DEFAULT_ORDER_FIELD, Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener {
                for (d in it) {
                    try {
                        val ref = galleryRef.document(d.id).collection(AppConstaints.COLLECTION_LIKED_USERS)
                            .document(Utils.mUid(mContext))
                        list.add(ModelGallery(
                            d[IMAGE_URL].toString(),
                            d[SALON_ID].toString(),
                            ref,
                            d.id))
                        adapter.notifyItemInserted(list.size - 1)
                        binding.pb.visibility = View.GONE

                        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
                            if(scrollY > oldScrollY) {
                                binding.view.visibility = View.VISIBLE
                            }
                            if (scrollY == 0) {
                                 binding.view.visibility = View.GONE
                            }
                            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                if (!isLoading) {
                                    binding.pb.visibility = View.VISIBLE
                                    loadMore(galleryRef, list, adapter)
                                    isLoading = true
                                }
                            }
                        })
                    } catch (e : Exception) { }
                }
            }

        binding.rv.adapter = adapter
    }

    private fun loadMore(galleryRef : CollectionReference, list: ArrayList<ModelGallery>, adapter : AdapterGallery) {

        galleryRef.orderBy(DEFAULT_ORDER_FIELD, Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener {
                binding.pb.visibility = View.GONE
                for (d in it) {
                    try {
                        val ref = galleryRef.document(d.id).collection(AppConstaints.COLLECTION_LIKED_USERS)
                            .document(Utils.mUid(mContext))
                        list.add(ModelGallery(
                            d[IMAGE_URL].toString(),
                            d[SALON_ID].toString(),
                            ref,
                            d.id))
                        adapter.notifyItemInserted(list.size - 1)

                        isLoading = it.size() < 10

                        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY  ->
                            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                                if (!isLoading) {
                                    binding.pb.visibility = View.VISIBLE
                                    loadMore(galleryRef, list, adapter)
                                    isLoading = true
                                }
                            }
                        })
                    } catch (e : Exception) { }
                }
            }
    }

    private fun actions() {

        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.gallery,true)
        }

        binding.viewFavourites.setOnClickListener {
            findNavController().navigate(R.id.likedGalleryPhotos)
        }

    }

    private fun updateUi() {

    }

    inner class AdapterGallery(private val list : ArrayList<ModelGallery>) : RecyclerView.Adapter<AdapterGallery.GalleryHolder>() {

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

            shimmer.stopShimmer()
            shimmer.visibility = View.GONE
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

            val marginLayoutParams = view!!.layoutParams as MarginLayoutParams
            marginLayoutParams.setMargins(1, 1, 1, 1)

            image.maxHeight = (getScreenWidth() / 2) - 4
            image.maxWidth = (getScreenWidth() / 2) - 4

            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(IMAGE_URL, list[position].imageUrl)
                args.putString(SALON_ID, list[position].salonId)
                args.putBoolean(IS_SHOW_MORE_BUTTON, true)
                findNavController().navigate(R.id.openPhoto,args)
            }
        }

        private fun removeFromFav(ref: DocumentReference) {
            ref.delete()
        }

        private fun addToFav(ref: DocumentReference) {
            val map = HashMap<String, Any>()
            map[TIMESTAMP_FIELD] = FieldValue.serverTimestamp()
            map[AppConstaints.USER_ID] = Utils.mUid(mContext)
            ref[map] = SetOptions.merge()
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

    inner class ModelGallery(val imageUrl : String, val salonId : String, val ref : DocumentReference, id:String)

}