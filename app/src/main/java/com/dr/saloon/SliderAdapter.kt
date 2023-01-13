package com.dr.saloon

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.dr.saloon.utils.AppConstaints.SALON_ID

class SliderAdapter(private val sliderItems: ArrayList<SliderItems>, private val viewPager2: ViewPager2, val navController: NavController ) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        return SliderViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.row_item_slider_home, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        holder.setImage(sliderItems[position])
        if (position == sliderItems.size - 2) {
            viewPager2.post(runnable)
        }
    }

    override fun getItemCount(): Int {
        return sliderItems.size
    }

    inner class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        fun setImage(sliderItems: SliderItems) {
            if (sliderItems.imageUrl.isEmpty()){
              return
            }
            Glide.with(itemView.context)
                .load(sliderItems.imageUrl).into(imageView)
            if (sliderItems.salonId.isNotEmpty()){
                itemView.setOnClickListener {
                    val args = Bundle()
                    args.putString(SALON_ID, sliderItems.salonId)
                    navController.navigate(R.id.salonDetails, args)
                }
            }
        }

    }

    private val runnable = Runnable {
        sliderItems.addAll(sliderItems)
        notifyDataSetChanged()
    }
}

class SliderItems(val imageUrl : String, val salonId : String)