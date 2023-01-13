package com.dr.saloon.fragments.services

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentAllServicesOfSalonBinding
import com.dr.saloon.databinding.RowItemServicesInsideAllSalonBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.SALON_ID
import com.dr.saloon.utils.AppConstaints.SALON_NAME
import com.dr.saloon.utils.AppConstaints.SALON_TYPE
import com.dr.saloon.utils.AppConstaints.SERVICES
import com.google.firebase.firestore.FirebaseFirestore
import java.util.ArrayList

class AllServiceOfSalon : Fragment() {

    private lateinit var binding : FragmentAllServicesOfSalonBinding
    private lateinit var mContext: Context
    private lateinit var salonId : String
    private val db = FirebaseFirestore.getInstance()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllServicesOfSalonBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {

            val services = arguments?.getStringArrayList(SERVICES) as ArrayList<String>
            salonId = arguments?.getString(SALON_ID).toString()

            actions()

            val salonName = arguments?.getString(SALON_NAME).toString()

            when(arguments?.getString(SALON_TYPE).toString()) {
                AppConstaints.MALE -> {
                    binding.genderLogo.setImageResource(R.drawable.man_128)
                }
                AppConstaints.FEMALE -> {
                    binding.genderLogo.setImageResource(R.drawable.woman_128)
                }
                AppConstaints.UNISEX -> {
                    binding.genderLogo.setImageResource(R.drawable.unisex_128)
                }
            }
            binding.title.text = "Services offered by\n$salonName"

            services(services)


        } catch (e : Exception) { }

    }

    private fun actions() {
        binding.back.setOnClickListener {
            findNavController().popBackStack(R.id.allServiceOfSalon, true)
        }
    }

    private fun services(services : ArrayList<String>) {

        val services1 = ArrayList<ModelSplitService>()

        for (i in 0 until services.size) {
            val s = services[i].split('?')
            services1.add(ModelSplitService(
                try {
                    s[0]
                } catch (e : Exception) { "Not Found" }
                ,  try {
                    s[1]
                } catch (e : Exception) { "Not Found" },
                try {
                    s[2]
                } catch (e : Exception) { "0" }
            ))
        }

        val serviceList = ArrayList<ModelServices>()

        val adapterServices = AdapterServices(serviceList)

        for (i in 0 until services1.size) {
            db.collection(AppConstaints.COLLECTION_SERVICES)
                .document(services1[i].serviceId)
                .get().addOnSuccessListener { d->
                    try {
                        if (d.exists()) {
                            serviceList.add(ModelServices(d[AppConstaints.SERVICE_NAME].toString(),
                                d[AppConstaints.IMAGE_URL].toString(),
                                services1[i].price.toInt(),
                                d.id))
                            adapterServices.notifyItemInserted(serviceList.size - 1)
                            binding.pb.visibility = View.GONE
                        }
                    } catch (e : Exception) {}
                }
        }

        binding.rv.adapter = adapterServices

    }
    
    private inner class AdapterServices(private val list: ArrayList<ModelServices>) : RecyclerView.Adapter<AdapterServices.ServiceHolder>() {

        inner class ServiceHolder(itemView: View, val rowItemServicesInsideAllSalonBinding: RowItemServicesInsideAllSalonBinding) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ServiceHolder {
            val rowItemServicesInsideAllSalonBinding =
                RowItemServicesInsideAllSalonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ServiceHolder(rowItemServicesInsideAllSalonBinding.root, rowItemServicesInsideAllSalonBinding)
        }

        override fun onBindViewHolder(holder: ServiceHolder, position: Int) {
            holder.rowItemServicesInsideAllSalonBinding.serviceName.typeface = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
            holder.rowItemServicesInsideAllSalonBinding.serviceName.text = list[position].serviceName
            holder.rowItemServicesInsideAllSalonBinding.price.text = "₹ ${list[position].price}"
            holder.rowItemServicesInsideAllSalonBinding.imageView.load(list[position].imageUrl)
            holder.itemView.setOnClickListener {
                val args = Bundle()
                args.putString(SALON_ID, salonId)
                args.putString(AppConstaints.SERVICE_ID, list[position].id)
                args.putString(AppConstaints.SERVICE_NAME, list[position].serviceName)
                args.putString(AppConstaints.SERVICE_COST, list[position].price.toString())
                findNavController().navigate(R.id.bookAppointment, args)
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    private inner class ModelServices(val serviceName : String, val imageUrl : String, val price : Int, val id : String)

    private inner class ModelSplitService(
        val serviceName: String,
        val serviceId: String,
        val price: String
    )

}