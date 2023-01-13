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
import com.dr.saloon.databinding.FragmentSearchCitiesBinding
import com.dr.saloon.utils.AppConstaints
import com.dr.saloon.utils.AppConstaints.CITY_NAME
import com.dr.saloon.utils.AppConstaints.COLLECTION_SERVICES
import com.dr.saloon.utils.AppConstaints.COLLECTION_TOP_CITIES
import com.dr.saloon.utils.AppConstaints.LATITUDE
import com.dr.saloon.utils.AppConstaints.LONGITUDE
import com.dr.saloon.utils.AppConstaints.MAP_API_KEY
import com.dr.saloon.utils.AppConstaints.PLACE_NAME
import com.dr.saloon.utils.AppConstaints.SOMETHING_WENT_WRONG
import com.dr.saloon.utils.AppConstaints.TIMESTAMP_FIELD
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.*
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
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

class SearchCities : Fragment() {

    private lateinit var binding : FragmentSearchCitiesBinding
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
        binding = FragmentSearchCitiesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTopCities()
        updateUi()
        actions()

    }

    private fun actions() {
        binding.etSearch.addTextChangedListener {
            if (binding.etSearch.text.toString().trim().isNotEmpty()){
                searchCities(binding.etSearch.text.toString())
            }
        }
        binding.currentLocation.setOnClickListener {
            getLocation()
        }
    }

    private fun updateUi() {


    }

    private fun searchCities(query : String) {

        Places.initialize(mContext, MAP_API_KEY)

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
                 val list = ArrayList<CityNameModel>()
                 for (prediction in response.autocompletePredictions) {
                    list.add(CityNameModel(
                        prediction.getFullText(null).toString(),
                        prediction.placeId,.0,.0) )
                 }
                val adapterNotifications = AdapterCities(list)

                binding.rvTopCities.adapter = adapterNotifications

            }.addOnFailureListener { exception: Exception? ->
            if (exception is ApiException) {
                val apiException = exception as ApiException
              //  Log.e("--->", "Place not found: " + apiException.statusCode)
            }
        }
    }

    private fun getLatLang(placeId : String, placeName : String) {

        val baseUrl = "https://maps.googleapis.com/maps/api/place/details/json?placeid=$placeId&key=$MAP_API_KEY"

        val jsonObjectRequest = object : JsonObjectRequest(Method.GET,baseUrl, null,
            {
                 val resultObj = it.getJSONObject("result")
                 val geometryObj = resultObj.getJSONObject("geometry")
                 val locationObj = geometryObj.getJSONObject("location")
                 val latitude = locationObj.get("lat").toString().toDouble()
                 val longitude = locationObj.get("lng").toString().toDouble()
                 binding.pb.visibility = View.GONE
                 val args = Bundle()
                 args.putDouble(LATITUDE, latitude)
                 args.putDouble(LONGITUDE, longitude)
                 args.putString(PLACE_NAME, placeName)
                findNavController().navigate(R.id.salonByLocation, args)
                updateCitySearch(latitude, longitude, placeName)
            }
            ,{
                binding.pb.visibility = View.GONE
                binding.rvTopCities.visibility = View.VISIBLE
            Toast.makeText(mContext, "Something went wrong!", Toast.LENGTH_SHORT).show()
        }){}

        Volley.newRequestQueue(mContext).add(jsonObjectRequest)

    }

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

                        val args = Bundle()
                        args.putDouble(LATITUDE, userLat)
                        args.putDouble(LONGITUDE, userLong)
                        args.putString(PLACE_NAME, getAddress(userLat, userLong))
                        findNavController().navigate(R.id.salonByLocation, args)
                    } catch (e : Exception) {
                        Toast.makeText(mContext, SOMETHING_WENT_WRONG, Toast.LENGTH_SHORT).show()
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
            "Nearby me "
        }
        return add
    }

    private fun loadTopCities() {

        val list = ArrayList<CityNameModel>()

        val adapter = AdapterCities(list)

        db.collection(COLLECTION_TOP_CITIES)
            .orderBy(TIMESTAMP_FIELD, Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener {
                if (it.size() == 0){
                    binding.pb.visibility = View.GONE
                    return@addOnSuccessListener
                }
                for (d in it){
                    try {
                        list.add(CityNameModel(
                            d[CITY_NAME].toString(),
                            "",
                            d[LATITUDE].toString().toDouble(),
                            d[LONGITUDE].toString().toDouble()))
                        adapter.notifyItemInserted(list.size-1)
                    } catch (e : Exception) {}
                }
            }

        binding.pb.visibility = View.GONE

        binding.rvTopCities.adapter = adapter

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
                if (list[position].placeId.trim().isEmpty()) {
                    val args = Bundle()
                    args.putDouble(LATITUDE, list[position].lat)
                    args.putDouble(LONGITUDE, list[position].lng)
                    args.putString(PLACE_NAME, list[position].cityName)
                    findNavController().navigate(R.id.salonByLocation, args)
                    return@setOnClickListener
                }
                binding.pb.visibility = View.VISIBLE
                binding.rvTopCities.visibility = View.GONE
                getLatLang(list[position].placeId, list[position].cityName)
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

    }

    inner class CityNameModel(val cityName : String, val placeId : String, val lat : Double, val lng : Double)

    private fun updateCitySearch(lat: Double, lng: Double, cityName: String) {

        val map = HashMap<String,Any>()
        map[LATITUDE] = lat
        map[LONGITUDE] = lng
        map[CITY_NAME] = cityName

        db.collection(AppConstaints.COLLECTION_TOP_SEARCH_CITIES)
            .document(cityName)
            .get().addOnSuccessListener {
                if (it.exists()) {
                    map[AppConstaints.SEARCH_TIME] = if (it[AppConstaints.SEARCH_TIME] != null) it[AppConstaints.SEARCH_TIME].toString().toInt() + 1 else 1
                    db.collection(AppConstaints.COLLECTION_TOP_SEARCH_CITIES)
                        .document(cityName)[map] = SetOptions.merge()
                    return@addOnSuccessListener
                }
                map[AppConstaints.SEARCH_TIME] = 1
                db.collection(AppConstaints.COLLECTION_TOP_SEARCH_CITIES)
                    .document(cityName)[map] = SetOptions.merge()
            }

    }

}