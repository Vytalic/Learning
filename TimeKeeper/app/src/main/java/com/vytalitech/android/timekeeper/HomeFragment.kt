package com.vytalitech.android.timekeeper

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vytalitech.android.timekeeper.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), HomeFragmentActions {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private lateinit var timerViewModel: TimerViewModel
    private var isRemoveModeActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        database = DatabaseProvider.getDatabase(requireContext())

        // Initialize TimerViewModel
        timerViewModel = ViewModelProvider(requireActivity())[TimerViewModel::class.java]

        timerViewModel.categories.observe(viewLifecycleOwner) { categories ->
            adapter.updateCategories(categories)
        }

        // Set up RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = CategoryAdapter(
            categories = mutableListOf(),
            categoryTimes = timerViewModel.categoryTimes.value ?: emptyMap(),
            activeTimers = timerViewModel.activeTimers.value ?: emptyMap(),
            onStartClick = { category ->
                timerViewModel.startTimer(category.id)
                timerViewModel.refreshCategories()},
            onStopClick = { category ->
                timerViewModel.stopTimer(category.id)
                timerViewModel.refreshCategories() },
            onRemoveClick = { category ->
                lifecycleScope.launch {
                    database.categoryDao().deleteCategory(category)
                    timerViewModel.refreshCategories() // Notify ViewModel to refresh categories

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

        // Observe changes to the categories LiveData in TimerViewModel
        timerViewModel.categories.observe(viewLifecycleOwner) { categories ->
            Log.d("HomeFragment", "Categories observed: $categories")
            adapter.updateCategories(categories)
        }

        // Load categories
        lifecycleScope.launch {
            refreshCategories()
        }
        return binding.root
    }

    private suspend fun refreshCategories() {
        val categories = database.categoryDao().getAllCategories()
        adapter.updateCategories(categories)

        // Update timerViewModel with new categories
        val timesMap = categories.associate { it.id to it.totalTime }
        timerViewModel.categoryTimes.value = timesMap.toMutableMap()
    }

    override fun activateRemoveMode() {
        adapter.enableRemoveMode() // Enable remove mode in the adapter
        enterRemoveMode()
        //Toast.makeText(this, "Select a category to remove", Toast.LENGTH_SHORT).show()
    }



    private fun enterRemoveMode() {
        binding.btnCancelRemoveMode.visibility = View.VISIBLE
    }

    private fun exitRemoveMode() {
        isRemoveModeActive = false
        adapter.disableRemoveMode() // Exit remove mode in adapter
        binding.btnCancelRemoveMode.visibility = View.GONE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe timer states and refresh UI
        timerViewModel.categoryTimes.observe(viewLifecycleOwner) { times ->
            adapter.updateTimes(times)
        }

        timerViewModel.activeTimers.observe(viewLifecycleOwner) { activeTimers ->
            adapter.notifyDataSetChanged() // Rebind data to reflect active timers
        }

        // Re-emit timer data to ensure UI synchronization
        timerViewModel.reEmitCategoryTimes()
    }

    override fun onResume() {
        super.onResume()
        timerViewModel.reEmitCategoryTimes()
        timerViewModel.loadTimerData()
    }

}
