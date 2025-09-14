package com.example.templet1

import ImageSliderAdapter
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.templet1.databinding.FragmentHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class Home : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var currentPosition = 0
    private lateinit var recyclerView: RecyclerView

    private val updates = mutableListOf<NewsItem>()
    private lateinit var adapter: NewsAdapter // Using NewsAdapter

    // Map variables
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST = 101
    private val GOOGLE_API_KEY = "AIzaSyBaNvEX1VWri7w-DBqEAFb3vnhmSRpyh5k"  // Replace with your API key

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvUpdates)


        // Initialize NewsAdapter
        adapter = NewsAdapter(updates)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter



        // Load news initially
        loadNewsFromJson()

        // --- Image slider logic ---
        val images = listOf(
            R.drawable.img4,
            R.drawable.img5,
            R.drawable.img3,
            R.drawable.img7,
            R.drawable.img6,
            R.drawable.img1
        )
        val sliderAdapter = ImageSliderAdapter(images)
        binding.imageSlider.adapter = sliderAdapter

        val runnable = object : Runnable {
            override fun run() {
                currentPosition = if (currentPosition >= images.size) 0 else currentPosition
                binding.imageSlider.currentItem = currentPosition++
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
        // -----------------------------------

        // --- Initialize Map ---
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun loadNewsFromJson() {


        // Load JSON from assets
        val json = requireContext().assets.open("news.json").bufferedReader().use { it.readText() }
        val newsItems = Gson().fromJson(json, Array<NewsItem>::class.java).toList()

        updates.clear()
        updates.addAll(newsItems)

        adapter.notifyDataSetChanged()

    }

    // --- Map functions ---
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableUserLocation()
    }

    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLatLng = LatLng(it.latitude, it.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                fetchNearbyPlaces(userLatLng)
            }
        }
    }

    private fun fetchNearbyPlaces(location: LatLng) {
        val types = listOf("hospital", "atm", "bus_station", "police")
        val radius = 2000
        val client = OkHttpClient()

        for (type in types) {
            val url =
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${location.latitude},${location.longitude}&radius=$radius&type=$type&key=$GOOGLE_API_KEY"

            val request = Request.Builder().url(url).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    val data = response.body?.string() ?: return
                    val json = JSONObject(data)
                    val results = json.getJSONArray("results")

                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val loc = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = loc.getDouble("lat")
                        val lng = loc.getDouble("lng")
                        val name = place.getString("name")

                        activity?.runOnUiThread {
                            map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(lat, lng))
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor(type)))
                            )
                        }
                    }
                }
            })
        }
    }

    // --- Helper function for color coding ---
    private fun getMarkerColor(type: String): Float {
        return when (type) {
            "hospital" -> BitmapDescriptorFactory.HUE_RED
            "atm" -> BitmapDescriptorFactory.HUE_BLUE
            "bus_station" -> BitmapDescriptorFactory.HUE_YELLOW
            "police" -> BitmapDescriptorFactory.HUE_GREEN
            else -> BitmapDescriptorFactory.HUE_ORANGE
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        }
    }
}
