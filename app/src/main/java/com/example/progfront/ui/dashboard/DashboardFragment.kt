package com.example.progfront.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.databinding.FragmentDashboardBinding
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        tokenManager = TokenManager(requireContext())

        dashboardViewModel.text.observe(viewLifecycleOwner) {
            binding.textDashboard.text = it
        }

        binding.buttonDeleteAllSchedules.setOnClickListener {
            deleteAllSchedulesDebug()
        }

        return root
    }

    private fun deleteAllSchedulesDebug() {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No token", Toast.LENGTH_SHORT).show()
            return
        }
        binding.buttonDeleteAllSchedules.isEnabled = false
        binding.textDeleteProgress.visibility = View.VISIBLE
        binding.textDeleteProgress.text = "Collecting schedules..."

        // Define a window (e.g., past 15 days and next 30 days)
        val toDeleteIds = mutableSetOf<Int>()
        val today = Calendar.getInstance()
        val startCal = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -15) }
        val endCal = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 30) }

        val days = mutableListOf<String>()
        val probe = startCal.clone() as Calendar
        while (!probe.after(endCal)) {
            days.add(dateFormat.format(probe.time))
            probe.add(Calendar.DAY_OF_MONTH, 1)
        }

        var fetched = 0
        fun fetchNextDay() {
            if (fetched >= days.size) {
                if (toDeleteIds.isEmpty()) {
                    binding.textDeleteProgress.text = "No schedules found in window"
                    binding.buttonDeleteAllSchedules.isEnabled = true
                } else {
                    binding.textDeleteProgress.text = "Deleting ${toDeleteIds.size} schedules..."
                    performDeletes(token, toDeleteIds.toList())
                }
                return
            }
            val day = days[fetched]
            fetched++
            RetrofitClient.instance.getSchedulesForDay("Bearer $token", day)
                .enqueue(object : Callback<List<ScheduleResponse>> {
                    override fun onResponse(
                        call: Call<List<ScheduleResponse>>,
                        response: Response<List<ScheduleResponse>>
                    ) {
                        if (response.isSuccessful) {
                            response.body()?.forEach { toDeleteIds.add(it.id) }
                        }
                        binding.textDeleteProgress.text = "Collected ${toDeleteIds.size} ids (day $fetched/${days.size})"
                        fetchNextDay()
                    }

                    override fun onFailure(call: Call<List<ScheduleResponse>>, t: Throwable) {
                        binding.textDeleteProgress.text = "Error fetching day $day: ${t.message}"
                        fetchNextDay() // continue
                    }
                })
        }
        fetchNextDay()
    }

    private fun performDeletes(token: String, ids: List<Int>) {
        var deleted = 0
        fun deleteNext(index: Int) {
            if (index >= ids.size) {
                binding.textDeleteProgress.text = "Deleted $deleted / ${ids.size} schedules"
                binding.buttonDeleteAllSchedules.isEnabled = true
                Toast.makeText(requireContext(), "Delete complete", Toast.LENGTH_SHORT).show()
                return
            }
            val id = ids[index]
            binding.textDeleteProgress.text = "Deleting ($deleted/${ids.size}) id=$id"
            RetrofitClient.instance.deleteSchedule("Bearer $token", id)
                .enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) deleted++
                        deleteNext(index + 1)
                    }

                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Skip failure and continue
                        deleteNext(index + 1)
                    }
                })
        }
        deleteNext(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}