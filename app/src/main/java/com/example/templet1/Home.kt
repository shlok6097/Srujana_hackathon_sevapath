package com.example.templet1

import ImageSliderAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.example.templet1.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Home : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var currentPosition = 0
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val updates = mutableListOf<GovernmentUpdate>()
    private lateinit var adapter: UpdatesAdapter

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
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        adapter = UpdatesAdapter(updates)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { fetchNewsSimple() }

        fetchNewsSimple()

        // Offline images from drawable
        val images = listOf(
            R.drawable.aadhar,
            R.drawable.ambulance,
            R.drawable.fire
        )

        val adapter = ImageSliderAdapter(images)
        binding.imageSlider.adapter = adapter

        // Dots indicator


        // Auto-slide
        val runnable = object : Runnable {
            override fun run() {
                currentPosition = if (currentPosition >= images.size) 0 else currentPosition
                binding.imageSlider.currentItem = currentPosition++
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        handler.removeCallbacksAndMessages(null)
    }
    private fun fetchNewsSimple() {
        swipeRefresh.isRefreshing = true

        Thread {
            try {

                val urlString ="https://gnews.io/api/v4/search?q=government%20scheme&lang=en&country=in&apikey=43e6a3ea9962f7749952257b0b352f80"
                    Log.d("API_DEBUG", "Requesting URL: $urlString")

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                Log.d("API_DEBUG", "Response code: $responseCode")

                val inputStream = connection.inputStream
                val result = inputStream.bufferedReader().use { it.readText() }

                Log.d("API_DEBUG", "Raw response: $result")

                val jsonObject = JSONObject(result)
                val articles = jsonObject.getJSONArray("articles")

                val fetchedUpdates = mutableListOf<GovernmentUpdate>()

                for (i in 0 until articles.length()) {
                    val article = articles.getJSONObject(i)
                    val title = article.getString("title")
                    val description = article.optString("description", "")
                    val urlToImage = article.optString("urlToImage", "")
                    val publishedAt = article.optString("publishedAt", "")
                    val urlToNews = article.optString("url", "")
                    val source = article.getJSONObject("source").optString("name", "")

                    Log.d("API_DEBUG", "Parsed article: $title")

                    fetchedUpdates.add(
                        GovernmentUpdate(
                            title,
                            description,
                            publishedAt,
                            urlToNews,
                            urlToImage,
                            source
                        )
                    )
                }

                requireActivity().runOnUiThread {
                    updates.clear()
                    updates.addAll(fetchedUpdates)
                    adapter.notifyDataSetChanged()
                    swipeRefresh.isRefreshing = false
                }

            } catch (e: Exception) {
                Log.e("API_DEBUG", "Error fetching news", e)
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    swipeRefresh.isRefreshing = false
                }
            }
        }.start()
    }






}
