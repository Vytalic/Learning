package com.vytalitech.android.timekeeper

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vytalitech.android.timekeeper.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), HomeFragmentActions {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private lateinit var timerViewModel: TimerViewModel
    private lateinit var recyclerView: RecyclerView
    private var isRemoveModeActive = false
    private var originalCategories: List<Category> = listOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HomeFragment", "onCreate called")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        database = DatabaseProvider.getDatabase(requireContext())

        // Initialize TimerViewModel
        timerViewModel = ViewModelProvider(requireActivity())[TimerViewModel::class.java]

        timerViewModel.categoryTimes.observe(viewLifecycleOwner) { times ->
            adapter.updateTimes(times)
        }

        timerViewModel.activeTimers.observe(viewLifecycleOwner) { activeTimers ->
            Log.d("HomeFragment", "ActiveTimers updated: $activeTimers")
            adapter.updateActiveTimers(activeTimers) // Update the adapter's activeTimers
        }

        binding.btnCancelRemoveMode.setOnClickListener {
            adapter.disableRemoveMode()
        }

        // Confirm reorder button click listener
        binding.btnConfirmReorder.apply {
            visibility = View.GONE // Ensure it's initially hidden
            setOnClickListener {
                saveReorderedCategories()
                exitReorderMode()
            }
        }

        // Confirm reorder button click listener
        binding.btnCancelReorder.apply {
            visibility = View.GONE // Ensure it's initially hidden
            setOnClickListener {
                cancelReorder()
                exitReorderMode()
            }
        }


        // Set up RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryAdapter(
            categories = mutableListOf(),
            categoryTimes = timerViewModel.categoryTimes.value?.toMutableMap() ?: mutableMapOf(),
            activeTimers = timerViewModel.activeTimers.value ?: emptyMap(),
            onStartClick = { category ->
                timerViewModel.startTimer(category.id, category.name, requireContext())},
            onStopClick = { category ->
                timerViewModel.stopTimer(category.id, requireContext()) },
            onRemoveClick = { category ->
                lifecycleScope.launch {
                    database.categoryDao().deleteCategory(category)

                    // Update button state
                    if (adapter.itemCount == 0) {
                        adapter.disableRemoveMode()
                    } else {
                        binding.btnCancelRemoveMode.text = getString(R.string.btn_finish)
                        binding.btnCancelRemoveMode.setBackgroundColor(
                            ContextCompat.getColor(requireContext(), R.color.btnGreen)
                        )
                    }
                }
            },
            onRemoveModeUpdate = { isActive ->
                binding.btnCancelRemoveMode.apply {
                    text = if (isActive) {
                        context.getString(R.string.btn_cancel)
                    } else {
                        context.getString(R.string.btn_finish)
                    }
                    visibility = View.VISIBLE
                }
            }
        )

        binding.recyclerView.adapter = adapter

        // Set up cancel button click listener
        binding.btnCancelRemoveMode.setOnClickListener {
            adapter.disableRemoveMode()
            exitRemoveMode()
        }

        // Observe timer states
        timerViewModel.categoryTimes.observe(viewLifecycleOwner) { times ->
            adapter.updateTimes(times)
        }


        return binding.root
    }

    override fun activateRemoveMode() {
        adapter.enableRemoveMode()
        isRemoveModeActive = true
        timerViewModel.isRemoveMode.value = true // Update ViewModel
        enterRemoveMode()
    }

    private fun exitRemoveMode() {
        isRemoveModeActive = false
        adapter.disableRemoveMode()
        timerViewModel.isRemoveMode.value = false // Update ViewModel
        binding.btnCancelRemoveMode.visibility = View.GONE
    }


    private fun enterRemoveMode() {
        binding.btnCancelRemoveMode.visibility = View.VISIBLE
        binding.buttonContainer.visibility = View.GONE
    }

    fun enterReorderMode() {
        Log.d("HomeFragment", "Entering reorder mode")
        binding.buttonContainer.visibility = View.VISIBLE // Show buttons
        binding.btnConfirmReorder.visibility = View.VISIBLE
        binding.btnCancelReorder.visibility = View.VISIBLE
        binding.btnCancelRemoveMode.visibility = View.GONE // Hide cancel remove mode button
        adapter.enableReorderMode(binding.recyclerView) // Enable reordering

        Log.d("HomeFragment", "buttonContainer visibility: ${binding.buttonContainer.visibility}")
    }

    fun exitReorderMode() {
        binding.buttonContainer.visibility = View.GONE // Hide buttons
        binding.btnConfirmReorder.visibility = View.GONE
        binding.btnCancelReorder.visibility = View.GONE
        binding.btnCancelRemoveMode.visibility = View.GONE
        adapter.disableReorderMode(binding.recyclerView) // Disable reordering
    }




    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Log.d("HomeFragment", "buttonContainer initialized: ${binding.buttonContainer != null}")

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe category changes from the database
        database.categoryDao().getAllCategories().observe(viewLifecycleOwner) { categories ->
            Log.d("HomeFragment", "Categories updated: $categories")
            adapter.updateCategories(categories) // Update adapter with new categories
            timerViewModel.loadInitialTimes(categories) // Update TimerViewModel with new categories
        }

        // Observe timer states and refresh UI
        timerViewModel.categoryTimes.observe(viewLifecycleOwner) { times ->
            adapter.updateTimes(times)
        }

        timerViewModel.activeTimers.observe(viewLifecycleOwner) { activeTimers ->
            Log.d("HomeFragment", "ActiveTimers updated: $activeTimers")
            activeTimers.forEach { (categoryId, isRunning) ->
                val position = adapter.categories.indexOfFirst { it.id == categoryId }
                if (position != -1) {
                    Log.d("HomeFragment", "Updating item at position $position for category $categoryId, isRunning=$isRunning")
                    adapter.notifyItemChanged(position) // Notify the adapter to refresh this item
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        timerViewModel.startUpdatingTimers()
    }


    fun saveReorderedCategories() {
        val reorderedCategories = adapter.getCurrentOrder()
        lifecycleScope.launch {
            // Update the order for each category
            reorderedCategories.forEachIndexed { index, category ->
                category.order = index // Update the order field
            }

            // Save the updated list to the database
            database.categoryDao().updateCategories(reorderedCategories)

            adapter.disableReorderMode(binding.recyclerView)
            Toast.makeText(requireContext(), "Changes Saved!", Toast.LENGTH_SHORT).show()
            Log.d("HomeFragment", "Reordered categories saved to database: $reorderedCategories")
        }
    }

    fun cancelReorder() {
        lifecycleScope.launch {
            // Reset the adapter's data to the original list
            adapter.updateCategories(originalCategories)

            // Notify the adapter to refresh the UI
            adapter.notifyDataSetChanged()

            // Disable reorder mode
            adapter.disableReorderMode(binding.recyclerView)

            // Reload categories from the database to sync LiveData
            val currentCategories = database.categoryDao().getAllCategoriesDirect() // New method to fetch categories synchronously
            adapter.updateCategories(currentCategories)

            Toast.makeText(requireContext(), "Canceled!", Toast.LENGTH_SHORT).show()
        }
    }



}
