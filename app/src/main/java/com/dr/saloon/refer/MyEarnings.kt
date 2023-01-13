package com.dr.saloon.refer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentsMyEarningsBinding
import com.dr.saloon.utils.AppConstaints.DEFAULT_DATE_FORMAT
import com.dr.saloon.utils.AppConstaints.REFER_VALUE
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.dr.saloon.utils.AppConstaints.UPLINK_UID
import com.dr.saloon.utils.AppConstaints.USER_COLLECTION
import com.dr.saloon.utils.AppConstaints.USER_NAME
import com.dr.saloon.utils.Utils.Companion.mUid
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MyEarnings : Fragment() {

    lateinit var binding : FragmentsMyEarningsBinding
    lateinit var adapter : AdapterMyEarning
    var date : String? = null
    var db = FirebaseFirestore.getInstance()
    var oldEarningList = ArrayList<MyEarningModel>()
    var newEarningList = ArrayList<MyEarningModel>()
    private lateinit var mContext: Context

    override fun onAttach(context: Context) {
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentsMyEarningsBinding.inflate(layoutInflater)


        binding.back.setOnClickListener{
            findNavController().popBackStack(R.id.myEarnings, true)
        }


        adapter = AdapterMyEarning(newEarningList)

        db.collection(USER_COLLECTION)
            .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .whereEqualTo(UPLINK_UID, mUid(mContext))
            .get().addOnSuccessListener { qs->

                if (qs.size() == 0){
                    binding.pb.visibility = View.GONE
                    binding.noData.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                oldEarningList.clear()

                for (d in qs){
                    try {

                        val date = d.getDate(TIMESTAMP_FIELD)
                        val dateformat = SimpleDateFormat(
                            DEFAULT_DATE_FORMAT,
                            Locale.ENGLISH).format(date as Date)

                        oldEarningList.add(
                            MyEarningModel(
                                d[USER_NAME].toString(),
                                d[REFER_VALUE]?.toString() ?: throw  Exception(),
                                dateformat,
                                d.id
                            )
                        )
                        binding.pb.visibility = View.GONE
                        binding.noData.visibility = View.GONE
                    }catch (e:Exception){}
                }
                loadData()
            }

        binding.rv.adapter = adapter
        // Inflate the layout for this fragment
        return binding.root
    }


    private fun loadData() {


        for (i in newEarningList.size..newEarningList.size+9){

            if (i < oldEarningList.size) {

                newEarningList.add(oldEarningList[i])
                adapter.notifyItemInserted(newEarningList.size - 1)
            }
        }

        binding.nestedSv.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener
            { v, _, scrollY, _, _ ->
                if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                    loadData()
                }
            })
    }

    class AdapterMyEarning(var list:ArrayList<MyEarningModel>): RecyclerView.Adapter<AdapterMyEarning.viewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
            val view  = LayoutInflater.from(parent.context).inflate(R.layout.row_item_my_earnings,parent,false)
            return viewHolder(view)
        }

        override fun onBindViewHolder(holder: viewHolder, position: Int) {
            holder.setData(list[position].userName,list[position].amount,list[position].date,list[position].id)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        inner class viewHolder(itemView:View):RecyclerView.ViewHolder(itemView){
            var userName : TextView = itemView.findViewById(R.id.userName)
            var amount : TextView = itemView.findViewById(R.id.amount)
            var date : TextView = itemView.findViewById(R.id.date)
            fun setData(userName: String, amount: String, date: String, id: String) {
                this.userName.text = userName
                this.amount.text = "Amount: ₹$amount"
                this.date.text = date
            }
        }
    }

    class MyEarningModel(var userName:String,var amount:String,var date:String,var id:String){}
}