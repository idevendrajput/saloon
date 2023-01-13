package com.dr.saloon.fragments.services

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dr.saloon.R
import com.facebook.shimmer.ShimmerFrameLayout

class AdapterServicesShimmer() : RecyclerView.Adapter<AdapterServicesShimmer.Viewholder>() {

    class Viewholder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Viewholder {
        return Viewholder(LayoutInflater.from(parent.context).inflate(R.layout.shimmer_row_item_services_home, parent, false))
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmerEffect).startShimmer()
    }

    override fun getItemCount(): Int {
        return 8
    }
}

class AdapterSalonShimmer() : RecyclerView.Adapter<AdapterSalonShimmer.Viewholder>() {

    class Viewholder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Viewholder {
        return Viewholder(LayoutInflater.from(parent.context).inflate(R.layout.shimmer_row_item_top_salon_home, parent, false))
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmerEffect).startShimmer()
    }

    override fun getItemCount(): Int {
        return 5
    }
}

class AdapterSalonNearByShimmer() : RecyclerView.Adapter<AdapterSalonNearByShimmer.Viewholder>() {

    class Viewholder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Viewholder {
        return Viewholder(LayoutInflater.from(parent.context).inflate(R.layout.shimmer_row_item_nearby_home, parent, false))
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmerEffect).startShimmer()
    }

    override fun getItemCount(): Int {
        return 4
    }
}

class AdapterTopSalonShimmer() : RecyclerView.Adapter<AdapterTopSalonShimmer.Viewholder>() {

    class Viewholder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Viewholder {
        return Viewholder(LayoutInflater.from(parent.context).inflate(R.layout.shimmer_row_item_top_salon_, parent, false))
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.itemView.findViewById<ShimmerFrameLayout>(R.id.shimmerEffect).startShimmer()
    }

    override fun getItemCount(): Int {
        return 8
    }
}



