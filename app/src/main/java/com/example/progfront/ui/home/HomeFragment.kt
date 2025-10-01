package com.example.progfront.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.databinding.FragmentHomeBinding
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
        updateTitle()
        loadSchedules()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadSchedules()
    }

    private fun setupRecycler() {
        adapter = ScheduleAdapter { schedule -> toggleStatus(schedule) }
        binding.recyclerSchedules.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSchedules.adapter = adapter
        // Hide cache banner always (cache removed)
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
        Log.d("HomeFragment", "Fetching schedules via /schedule?day=$day")
        showLoading(true)
        RetrofitClient.instance.getSchedulesForDay("Bearer $token", day)
            .enqueue(object : Callback<List<ScheduleResponse>> {
                override fun onResponse(
                    call: Call<List<ScheduleResponse>>,
                    response: Response<List<ScheduleResponse>>
                ) {
                    finishLoading()
                    if (response.isSuccessful) {
                        val rawList = response.body().orEmpty()
                        Log.d("HomeFragment", "Server returned ${rawList.size} schedules (pre-filter) for day=$day")
                        val filtered = filterBySelectedDay(rawList, day)
                        Log.d("HomeFragment", "Filtered to ${filtered.size} schedules for exact day=$day")
                        applySchedules(filtered.sortedBy { it.start_time })
                    } else {
                        Log.e("HomeFragment", "Fetch failed code=${response.code()} body=${response.errorBody()?.string()}")
                        showEmpty()
                    }
                }

                override fun onFailure(call: Call<List<ScheduleResponse>>, t: Throwable) {
                    finishLoading()
                    Log.e("HomeFragment", "Network error: ${t.message}", t)
                    Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    showEmpty()
                }
            })
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
        val token = tokenManager.getAccessToken() ?: return
        val next = when (schedule.status.lowercase(Locale.getDefault())) {
            "planned" -> "Completed"
            "completed" -> "Planned"
            else -> "Completed"
        }
        adapter.markUpdating(schedule.id, true)
        RetrofitClient.instance.updateScheduleStatus(
            "Bearer $token",
            schedule.id,
            mapOf("status" to next)
        ).enqueue(object : Callback<ScheduleResponse> {
            override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                adapter.markUpdating(schedule.id, false)
                if (response.isSuccessful) {
                    val updated = response.body()
                    if (updated != null) {
                        // Re-fetch current day to stay consistent
                        loadSchedules()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                adapter.markUpdating(schedule.id, false)
                Toast.makeText(requireContext(), "Status update error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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