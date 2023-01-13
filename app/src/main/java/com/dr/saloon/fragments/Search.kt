package com.dr.saloon.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.dr.saloon.R
import com.dr.saloon.databinding.FragmentSearchBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.CITY_NAME
import com.dr.saloon.utils.AppConstaints.COLLECTION_TOP_SEARCH_CITIES
import com.dr.saloon.utils.AppConstaints.LATITUDE
import com.dr.saloon.utils.AppConstaints.LONGITUDE
import com.dr.saloon.utils.AppConstaints.SEARCH_TIME
import com.dr.saloon.utils.SharedPref.Companion.setDouble
import com.dr.saloon.utils.Utils
import com.dr.saloon.utils.Utils.Companion.setPlaceName
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Search : Fragment() {

    private lateinit var binding : FragmentSearchBinding
    private lateinit var mContext: Context
    private val db = FirebaseFirestore.getInstance()
    private lateinit var placesClient: PlacesClient

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateUi()
        actions()
        loadMetros()
        loadTopCities()
        loadTopSearch()

    }

    private fun loadMetros() {

        val r = ArrayList<CityNameModel>()

        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))

        val adapter1 = AdapterCities(r)

        binding.rvTopMetro.adapter = adapter1

        val list = ArrayList<CityNameModel>()

        val adapter = AdapterCities(list)

        db.collection(AppConstaints.COLLECTION_TOP_METRO)
            .orderBy(AppConstaints.TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener {
                if (it.size() == 0){
                    return@addOnSuccessListener
                }
                list.clear()
                for (d in it){
                    try {
                        list.add(CityNameModel(
                            d[AppConstaints.CITY_NAME].toString(),
                            "",
                            d[AppConstaints.LATITUDE].toString().toDouble(),
                            d[AppConstaints.LONGITUDE].toString().toDouble()))
                        adapter.notifyItemInserted(list.size-1)
                    } catch (e : Exception) {}
                }
            }

        binding.rvTopMetro.adapter = adapter

    }

    private fun loadTopCities() {

        val r = ArrayList<CityNameModel>()
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))
        r.add(CityNameModel("...",  "",.0,.0))

        val adapter1 = AdapterCities(r)

        binding.rvTopCities.adapter = adapter1

        val list = ArrayList<CityNameModel>()
        val adapter  = AdapterCities(list)

       db.collection(AppConstaints.COLLECTION_TOP_CITIES)
              .orderBy(AppConstaints.TIMESTAMP_FIELD, Query.Direction.DESCENDING)
              .limit(10)
              .get().addOnSuccessListener {
                  if (it.size() == 0){
                      return@addOnSuccessListener
                  }
                list.clear()
                  for (d in it){
                      try {
                          list.add(CityNameModel(
                              d[AppConstaints.CITY_NAME].toString(),
                              "",
                              d[AppConstaints.LATITUDE].toString().toDouble(),
                              d[AppConstaints.LONGITUDE].toString().toDouble()))
                          adapter.notifyItemInserted(list.size-1)
                      } catch (e : Exception) {}
                  }
              }

          binding.rvTopCities.adapter = adapter

    }

    private fun searchCities(query : String) {

        if (query.isEmpty()) {
            try {
                binding.layoutContents.visibility = View.VISIBLE
                binding.rvSearch.removeAllViewsInLayout()
                binding.rvSearch.visibility = View.GONE
            } catch (e : Exception) {}
            return
        }

        binding.rvSearch.visibility = View.VISIBLE
        binding.pb.visibility = View.VISIBLE

        Places.initialize(mContext, AppConstaints.MAP_API_KEY)

        placesClient = Places.createClient(mContext)

        val token = AutocompleteSessionToken.newInstance()

        val bounds = RectangularBounds.newInstance(
            LatLng(-33.880490, 151.184363),  //dummy lat/lng
            LatLng(-33.880490, 151.184363)
        )

        val request = FindAutocompletePredictionsRequest.builder() // Call either setLocationBias() OR setLocationRestriction().
            .setLocationBias(bounds) //.setLocationRestriction(bounds)
            .setCountry("in")
            .setTypeFilter(TypeFilter.CITIES)
            .setSessionToken(token)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                binding.pb.visibility = View.GONE
                binding.layoutContents.visibility = View.GONE
                val list = ArrayList<CityNameModel>()
                for (prediction in response.autocompletePredictions) {
                    list.add(CityNameModel(
                        prediction.getFullText(null).toString(),
                        prediction.placeId,.0,.0) )
                }
                val adapter  = AdapterCities(list)

                binding.rvSearch.adapter = adapter
            }

    }

    private fun getLatLang(placeId : String, cityName: String) {

        val baseUrl = "https://maps.googleapis.com/maps/api/place/details/json?placeid=$placeId&key=${AppConstaints.MAP_API_KEY}"

        val jsonObjectRequest = object : JsonObjectRequest(
            Method.GET,baseUrl, null,
            {
                val resultObj = it.getJSONObject("result")
                val geometryObj = resultObj.getJSONObject("geometry")
                val locationObj = geometryObj.getJSONObject("location")
                val latitude = locationObj.get("lat").toString().toDouble()
                val longitude = locationObj.get("lng").toString().toDouble()
                binding.pb.visibility = View.GONE
                setDouble(mContext, LATITUDE, latitude)
                setDouble(mContext, LONGITUDE, longitude)
                setPlaceName(mContext, cityName)
                val map = HashMap<String,Any>()
                map[LATITUDE] = latitude
                map[LONGITUDE] = longitude
                db.collection(AppConstaints.USER_COLLECTION)
                    .document(Utils.mUid(mContext))[map] = SetOptions.merge()
                findNavController().navigate(R.id.home)
                updateCitySearch(latitude, longitude, cityName)
            }
            ,{
                binding.pb.visibility = View.GONE
                Toast.makeText(mContext, "Something went wrong!", Toast.LENGTH_SHORT).show()
            }){}

        Volley.newRequestQueue(mContext).add(jsonObjectRequest)

    }

    private fun updateCitySearch(lat: Double, lng: Double, cityName: String) {

        val map = HashMap<String,Any>()
        map[LATITUDE] = lat
        map[LONGITUDE] = lng
        map[CITY_NAME] = cityName

        db.collection(COLLECTION_TOP_SEARCH_CITIES)
            .document(cityName)
            .get().addOnSuccessListener {
                if (it.exists()) {
                    map[SEARCH_TIME] = if (it[SEARCH_TIME] != null) it[SEARCH_TIME].toString().toInt() + 1 else 1
                    db.collection(COLLECTION_TOP_SEARCH_CITIES)
                        .document(cityName)[map] = SetOptions.merge()
                  return@addOnSuccessListener
                }
                map[SEARCH_TIME] = 1
                db.collection(COLLECTION_TOP_SEARCH_CITIES)
                    .document(cityName)[map] = SetOptions.merge()
            }
    }

    private fun loadTopSearch() {

        binding.chipGroup.removeAllViews()

        val x = ArrayList<TopSearchItems>()

        db.collection(COLLECTION_TOP_SEARCH_CITIES)
            .orderBy(SEARCH_TIME, Query.Direction.DESCENDING)
            .limit(5)
            .get().addOnSuccessListener {
                if (it.size() == 0){
                    return@addOnSuccessListener
                }
                for (d in it){
                    try {
                        val cityName = d[CITY_NAME].toString()
                        val chip = Chip(mContext)
                        chip.text = cityName
                        chip.height = 35
                        chip.width = LinearLayout.LayoutParams.WRAP_CONTENT
                        chip.textSize = 10f
                        binding.chipGroup.addView(chip)

                        x.add(TopSearchItems(cityName,d[LATITUDE].toString().toDouble(), d[LONGITUDE].toString().toDouble()))

                        for (i in 0 until x.size){
                            binding.chipGroup.getChildAt(i).setOnClickListener {
                                setDouble(mContext, LATITUDE, x[i].lat)
                                setDouble(mContext, LONGITUDE, x[i].lng)
                                setPlaceName(mContext, x[i].cityName)
                                val map = HashMap<String,Any>()
                                map[LATITUDE] = x[i].lat
                                map[LONGITUDE] = x[i].lng
                                db.collection(AppConstaints.USER_COLLECTION)
                                    .document(Utils.mUid(mContext))[map] = SetOptions.merge()
                                findNavController().navigate(R.id.home)
                            }
                        }

                    } catch (e : Exception) {}
                }
            }
    }

    private inner class TopSearchItems(val cityName : String,val lat : Double, val lng : Double)

    private fun getLocation() {
        Dexter.withContext(mContext)
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(mContext, "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    try {
                        val locationManager = (activity as AppCompatActivity).getSystemService(Context.LOCATION_SERVICE) as LocationManager
                        val locationProvider = LocationManager.NETWORK_PROVIDER
                        @SuppressLint("MissingPermission")
                        val lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)
                        val userLat = lastKnownLocation!!.latitude
                        val userLong = lastKnownLocation.longitude

                        Executors.newSingleThreadExecutor().execute {
                            setDouble(mContext, LATITUDE, userLat)
                            setDouble(mContext, LONGITUDE, userLong)
                            setPlaceName(mContext, getAddress(userLat, userLong))
                            val map = HashMap<String,Any>()
                            map[LATITUDE] = userLat
                            map[LONGITUDE] = userLong
                            db.collection(AppConstaints.USER_COLLECTION)
                                .document(Utils.mUid(mContext))[map] = SetOptions.merge()
                        }

                        findNavController().navigate(R.id.home)

                    } catch (e : Exception) {
                        Toast.makeText(mContext, AppConstaints.SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {}
            }).check()
    }

    fun getAddress(lat: Double, lng: Double) : String {
        val geocoder = Geocoder( mContext, Locale.getDefault())
        var add = ""
        add = try {

            val addresses: List<Address> = geocoder.getFromLocation(lat, lng, 1)
            val obj: Address = addresses[0]

            obj.locality

        } catch (e: IOException) {
            "Nearby me"
        }
        return add
    }

    private fun actions() {
        binding.etSearch.addTextChangedListener {
            searchCities(binding.etSearch.text.toString())
        }
        binding.currentLocation.setOnClickListener {
            getLocation()
        }
    }

    private fun updateUi() {

        val typesFace = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
        binding.etSearch.typeface = typesFace
        binding.txtTopCities.typeface = typesFace
        binding.txtTopMetro.typeface = typesFace
        binding.txtTopSearch.typeface = typesFace

    }

    inner class AdapterCities(val list: ArrayList<CityNameModel>) : RecyclerView.Adapter<AdapterCities.CityHolder>() {

        inner class CityHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): AdapterCities.CityHolder {
            return CityHolder(LayoutInflater.from(parent.context).inflate(R.layout.row_item_simple_list_items, parent, false))
        }

        override fun onBindViewHolder(holder: AdapterCities.CityHolder, position: Int) {
            val text = holder.itemView.findViewById<TextView>(R.id.text1)
            text.text = list[position].cityName
            val typesFace = Typeface.createFromAsset((activity as AppCompatActivity).assets, "fonts/futura-medium-bt.ttf")
            text.typeface = typesFace

            holder.itemView.setOnClickListener {

                if (list[position].lat != .0 && list[position].placeId.isEmpty()) {
                    setDouble(holder.itemView.context, LATITUDE, list[position].lat)
                    setDouble(holder.itemView.context, LONGITUDE, list[position].lng)
                    setPlaceName(mContext, list[position].cityName)
                    val map = HashMap<String,Any>()
                    map[LATITUDE] = list[position].lat
                    map[LONGITUDE] = list[position].lng
                    db.collection(AppConstaints.USER_COLLECTION)
                        .document(Utils.mUid(mContext))[map] = SetOptions.merge()
                    findNavController().navigate(R.id.home)
                    return@setOnClickListener
                }

                binding.pb.visibility = View.VISIBLE
                getLatLang(list[position].placeId, list[position].cityName)

            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

    inner class CityNameModel(val cityName : String, val placeId : String, val lat : Double, val lng : Double )

}