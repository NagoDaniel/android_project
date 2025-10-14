package com.example.progfront.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.progfront.data.Result
import com.example.progfront.databinding.FragmentDashboardBinding
import com.example.progfront.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        tokenManager = TokenManager(requireContext())

        viewModel.text.observe(viewLifecycleOwner) {
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

        viewLifecycleOwner.lifecycleScope.launch {
            var fetched = 0
            for (day in days) {
                fetched++
                val result = viewModel.getSchedulesForDay(day)
                when (result) {
                    is Result.Success -> {
                        result.data.forEach { toDeleteIds.add(it.id) }
                    }
                    is Result.Error -> {
                        // Continue on error
                    }
                    is Result.Loading -> {}
                }
                binding.textDeleteProgress.text = "Collected ${toDeleteIds.size} ids (day $fetched/${days.size})"
            }

            if (toDeleteIds.isEmpty()) {
                binding.textDeleteProgress.text = "No schedules found in window"
                binding.buttonDeleteAllSchedules.isEnabled = true
            } else {
                binding.textDeleteProgress.text = "Deleting ${toDeleteIds.size} schedules..."
                performDeletes(toDeleteIds.toList())
            }
        }
    }

    private fun performDeletes(ids: List<Int>) {
        viewLifecycleOwner.lifecycleScope.launch {
            var deleted = 0
            for ((index, id) in ids.withIndex()) {
                binding.textDeleteProgress.text = "Deleting ($deleted/${ids.size}) id=$id"
                val result = viewModel.deleteSchedule(id)
                if (result is Result.Success) {
                    deleted++
                }
            }
            binding.textDeleteProgress.text = "Deleted $deleted / ${ids.size} schedules"
            binding.buttonDeleteAllSchedules.isEnabled = true
            Toast.makeText(requireContext(), "Delete complete", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}