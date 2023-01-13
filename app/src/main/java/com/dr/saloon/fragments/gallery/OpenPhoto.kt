package com.dr.saloon.fragments.gallery

import android.content.Context
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentOpenPhotoBinding
import com.dr.saloon.utils.AppConstaints.COLLECTION_SALON
import com.dr.saloon.utils.AppConstaints.IMAGE_URL
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.google.firebase.firestore.FirebaseFirestore

class OpenPhoto : Fragment() {

    private lateinit var binding : FragmentOpenPhotoBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private lateinit var imageUrl : String
    private lateinit var salonId : String
    private lateinit var popupMenu : PopupMenu

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOpenPhotoBinding.inflate(layoutInflater)
        val inflater1 = TransitionInflater.from(requireContext())
        enterTransition = inflater1.inflateTransition(R.transition.fade)
        imageUrl = arguments?.getString(IMAGE_URL).toString()
        salonId = arguments?.getString(SALON_ID).toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imageView.load(imageUrl)

        actions()

        if (arguments?.getBoolean(IS_SHOW_MORE_BUTTON) as Boolean) {
            binding.more.visibility = View.VISIBLE
        } else {
            binding.more.visibility = View.GONE
        }

        db.collection(COLLECTION_SALON)
            .document(salonId)
            .get().addOnSuccessListener {
                try {
                    binding.title.text = "Photo by " + it[SALON_NAME].toString()
                    menu(it[SALON_NAME].toString())
                } catch (e : Exception) { }
            }
    }

    private fun actions() {
        binding.more.setOnClickListener {
            popupMenu.show()
        }
        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.openPhoto, true)
        }
    }

    private fun menu(salonName : String) {

        popupMenu = PopupMenu(mContext, binding.more)

        popupMenu.menuInflater.inflate(R.menu.open_photo_menu, popupMenu.menu)

        val item = popupMenu.menu.getItem(0) as MenuItem
        item.title = "More from $salonName"

        popupMenu.setOnMenuItemClickListener {item->
            if (item.itemId == R.id.viewMore) {
                val args = Bundle()
                args.putString(SALON_ID, salonId)
                findNavController().navigate(R.id.salonsGallery, args)
            }
            true
        }
    }

    companion object {
        const val IS_SHOW_MORE_BUTTON = "isShowMoreButton"
    }

}