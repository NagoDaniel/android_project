package com.example.progfront.ui.home

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progfront.data.Result
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.databinding.FragmentHomeBinding
import com.example.progfront.ui.schedule.ScheduleDetailActivity
import com.example.progfront.utils.TokenManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private lateinit var adapter: ScheduleAdapter
    private val calendar: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val viewModel: HomeViewModel by viewModels()

    private var pendingStatusUpdateId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        setupRecycler()
        setupDatePicker()
        setupSwipeRefresh()
        setupObservers()
        updateTitle()
        loadSchedules()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadSchedules()
    }

    private fun setupRecycler() {
        adapter = ScheduleAdapter(
            onStatusToggle = { schedule -> toggleStatus(schedule) },
            onItemClick = { schedule ->
                val ctx = requireContext()
                val intent = Intent(ctx, ScheduleDetailActivity::class.java)
                intent.putExtra("schedule_id", schedule.id)
                startActivity(intent)
            }
        )
        binding.recyclerSchedules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSchedules.adapter = adapter
        // Hide cache banner always (cache removed) idk
        binding.bannerCache.visibility = View.GONE
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadSchedules()
        }
    }

    private fun setupDatePicker() {
        binding.buttonPickDate.setOnClickListener {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            val d = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                updateTitle()
                loadSchedules()
            }, y, m, d).show()
        }
    }

    private fun setupObservers() {
        viewModel.schedules.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    finishLoading()
                    val day = dateFormat.format(calendar.time)
                    Log.d("HomeFragment", "Server returned ${result.data.size} schedules (pre-filter) for day=$day")
                    val filtered = filterBySelectedDay(result.data, day)
                    Log.d("HomeFragment", "Filtered to ${filtered.size} schedules for exact day=$day")
                    applySchedules(filtered.sortedBy { it.start_time })
                }
                is Result.Error -> {
                    finishLoading()
                    Log.e("HomeFragment", "Fetch failed: ${result.message}")
                    Toast.makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                    showEmpty()
                }
            }
        }

        viewModel.statusUpdateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    pendingStatusUpdateId?.let { adapter.markUpdating(it, false) }
                    pendingStatusUpdateId = null
                    loadSchedules()
                }
                is Result.Error -> {
                    pendingStatusUpdateId?.let { adapter.markUpdating(it, false) }
                    pendingStatusUpdateId = null
                    Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                }
                is Result.Loading -> { /* per-item loading already shown */ }
            }
        }
    }

    private fun updateTitle() {
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val selectedStr = dateFormat.format(calendar.time)
        binding.textTitle.text = if (todayStr == selectedStr) "Today's Plan" else "Plan for $selectedStr"
    }

    private fun loadSchedules() {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Not authenticated", Toast.LENGTH_SHORT).show()
            finishLoading()
            showEmpty()
            return
        }
        val day = dateFormat.format(calendar.time)
        Log.d("HomeFragment", "Fetching schedules for day=$day")
        viewModel.loadSchedulesForDay(day)
    }

    private fun filterBySelectedDay(list: List<ScheduleResponse>, targetDay: String): List<ScheduleResponse> {
        if (list.isEmpty()) return list
        return list.filter { schedule ->
            val dateCandidates = listOfNotNull(schedule.start_time, schedule.date)
            dateCandidates.any { candidate ->
                // candidate may be ISO like 2025-10-01T09:00:00.000Z or 2025-10-01T00:00:00Z
                val dayPart = candidate.take(10)
                dayPart == targetDay
            }
        }
    }

    private fun toggleStatus(schedule: ScheduleResponse) {
        val sequence = listOf("Planned", "Completed", "Skipped")
        val idx = sequence.indexOfFirst { it.equals(schedule.status, ignoreCase = true) }.let { if (it == -1) 0 else it }
        val next = sequence[(idx + 1) % sequence.size]
        adapter.markUpdating(schedule.id, true)
        pendingStatusUpdateId = schedule.id
        viewModel.toggleScheduleStatus(schedule.id, next)
    }

    private fun applySchedules(list: List<ScheduleResponse>) {
        if (list.isEmpty()) {
            showEmpty()
        } else {
            binding.textEmpty.visibility = View.GONE
            binding.recyclerSchedules.visibility = View.VISIBLE
            adapter.submitList(list)
        }
    }

    private fun showLoading(loading: Boolean) {
        if (loading && !binding.swipeRefresh.isRefreshing) {
            binding.progressLoading.visibility = View.VISIBLE
            binding.recyclerSchedules.visibility = View.GONE
            binding.textEmpty.visibility = View.GONE
        }
    }

    private fun finishLoading() {
        binding.swipeRefresh.isRefreshing = false
        binding.progressLoading.visibility = View.GONE
    }

    private fun showEmpty() {
        binding.recyclerSchedules.visibility = View.GONE
        binding.textEmpty.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}