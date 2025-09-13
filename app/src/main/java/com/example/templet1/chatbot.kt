package com.example.templet1


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.templet1.databinding.FragmentChatbotBinding
import kotlinx.coroutines.launch

class chatbot : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private lateinit var chatBot: AlphaGeeksChatBot

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)

        // Initialize RecyclerView and Adapter
        adapter = ChatAdapter(mutableListOf())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = adapter

        // Initialize AlphaGeeks ChatBot
        chatBot = AlphaGeeksChatBot()

        // Send button click
        binding.sendButton.setOnClickListener {
            val userMessage = binding.userMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {

                // 1️⃣ Add user message to chat
                adapter.addMessage(ChatMessage(text = userMessage, isUser = true))
                binding.userMessage.text.clear()
                binding.chatRecyclerView.scrollToPosition(adapter.itemCount - 1)

                // 2️⃣ Get bot response asynchronously
                lifecycleScope.launch {
                    val botMessage = chatBot.sendMessage(userMessage)

                    // 3️⃣ Handle links/images
                    when (botMessage.type) {
                        MessageType.LINK -> {
                            // Clickable link
                            adapter.addMessage(botMessage)
                        }
                        MessageType.IMAGE, MessageType.INFOGRAPHIC -> {
                            // Image/infographic, load with Glide
                            adapter.addMessage(botMessage)
                        }
                        else -> {
                            // Default text
                            adapter.addMessage(botMessage)
                        }
                    }
                    binding.chatRecyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
