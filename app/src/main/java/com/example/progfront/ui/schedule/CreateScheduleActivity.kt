package com.example.progfront.ui.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.progfront.data.Result
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.databinding.ActivityCreateScheduleBinding
import com.example.progfront.utils.TokenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateScheduleActivity : AppCompatActivity(), AddHabitDialogFragment.OnHabitCreatedListener {

    private lateinit var binding: ActivityCreateScheduleBinding
    private var habits = mutableListOf<HabitResponse>()
    private var selectedDate: Calendar = Calendar.getInstance()
    private lateinit var tokenManager: TokenManager

    private val viewModel: ScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        setupTimeSpinners()
        setupListeners()
        setupObservers()
        // Ensure visibility reflects the initially checked radio option
        updateRepeatUi(binding.radioGroupRepeatPattern.checkedRadioButtonId)
        loadHabits()
        updateDateButton()
    }

    private fun setupTimeSpinners() {
        // hour spinners (0-23)
        val hours = (0..23).map { it.toString().padStart(2, '0') }
        val hourAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStartHour.adapter = hourAdapter
        binding.spinnerEndHour.adapter = hourAdapter

        // minute spinners (0, 15, 30, 45)
        val minutes = listOf("00", "15", "30", "45")
        val minuteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, minutes)
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStartMinute.adapter = minuteAdapter
        binding.spinnerEndMinute.adapter = minuteAdapter

        // Set default times
        binding.spinnerStartHour.setSelection(9) // 9 AM
        binding.spinnerStartMinute.setSelection(0) // 00 minutes
        binding.spinnerEndHour.setSelection(10) // 10 AM
        binding.spinnerEndMinute.setSelection(0) // 00 minutes
    }

    private fun setupListeners() {
        binding.buttonCreateNewHabit.setOnClickListener {
            showAddHabitDialog()
        }

        binding.buttonSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        binding.buttonCancel.setOnClickListener {
            finish()
        }

        binding.buttonCreate.setOnClickListener {
            createSchedule()
        }

        // Toggle custom day checkboxes and weeks field based on selection
        binding.radioGroupRepeatPattern.setOnCheckedChangeListener { _, checkedId ->
            updateRepeatUi(checkedId)
        }


    }

    private fun updateRepeatUi(checkedId: Int) {
        val showCustom = checkedId == binding.radioButtonCustomDays.id
        binding.layoutCustomDays.visibility = if (showCustom) View.VISIBLE else View.GONE
        binding.layoutNumberOfWeeks.visibility = if (showCustom) View.VISIBLE else View.GONE
    }

    private fun setupObservers() {
        viewModel.habits.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    // Could show loading indicator
                }
                is Result.Success -> {
                    habits.clear()
                    habits.addAll(result.data)
                    setupHabitSpinner()
                }
                is Result.Error -> {
                    Toast.makeText(this, "Failed to load habits: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.createResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.buttonCreate.setEnabled(false)
                }
                is Result.Success -> {
                    binding.buttonCreate.setEnabled(true)
                    Toast.makeText(this, "Schedule created successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    binding.buttonCreate.setEnabled(true)
                    Toast.makeText(this, "Failed to create schedule: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadHabits() {
        val token = tokenManager.getAccessToken()
        if (token == null) {
            Toast.makeText(this, "Authentication token not found.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.loadHabits()
    }

    private fun setupHabitSpinner() {
        val habitNames = habits.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, habitNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHabits.adapter = adapter
    }

    private fun showAddHabitDialog() {
        val dialog = AddHabitDialogFragment()
        dialog.show(supportFragmentManager, "AddHabitDialogFragment")
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(year, month, dayOfMonth)
                updateDateButton()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateButton() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        binding.buttonSelectDate.text = sdf.format(selectedDate.time)
    }

    override fun onHabitCreated(habit: HabitResponse) {
        habits.add(habit)
        setupHabitSpinner()
        binding.spinnerHabits.setSelection(habits.size - 1)
    }

    private fun createSchedule() {
        val token = tokenManager.getAccessToken()
        if (token == null) {
            Toast.makeText(this, "Authentication token not found.", Toast.LENGTH_SHORT).show()
            return
        }

        if (habits.isEmpty()) {
            Toast.makeText(this, "Please create a habit first.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedHabit = habits[binding.spinnerHabits.selectedItemPosition]
        val startHour = binding.spinnerStartHour.selectedItem.toString()
        val startMinute = binding.spinnerStartMinute.selectedItem.toString()
        val endHour = binding.spinnerEndHour.selectedItem.toString()
        val endMinute = binding.spinnerEndMinute.selectedItem.toString()
        val notes = binding.editTextNotes.text.toString()
        val durationText = binding.editTextDuration.text.toString()
        val duration = if (durationText.isNotEmpty()) durationText.toIntOrNull() else null

        // Format date and time
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val selectedDateStr = dateFormat.format(selectedDate.time)
        val startTime = "${selectedDateStr}T${startHour}:${startMinute}:00"
        val endTime = "${selectedDateStr}T${endHour}:${endMinute}:00"

        Log.d("CreateSchedule", "Creating schedule for habit: ${selectedHabit.name}")
        Log.d("CreateSchedule", "Start time: $startTime")
        Log.d("CreateSchedule", "End time: $endTime")
        Log.d("CreateSchedule", "Duration: $duration")

        when (binding.radioGroupRepeatPattern.checkedRadioButtonId) {
            binding.radioButtonNone.id -> {
                // One-time schedule
                createCustomSchedule(selectedHabit.id, selectedDateStr, startTime, endTime, duration, notes)
            }
            binding.radioButtonDaily.id -> {
                // Daily recurring schedule
                createRecurringSchedule(selectedHabit.id, startTime, "daily", endTime, duration, notes)
            }
            binding.radioButtonWeekdays.id -> {
                // Weekdays recurring schedule
                createRecurringSchedule(selectedHabit.id, startTime, "weekdays", endTime, duration, notes)
            }
            binding.radioButtonWeekends.id -> {
                // Weekends recurring schedule
                createRecurringSchedule(selectedHabit.id, startTime, "weekends", endTime, duration, notes)
            }
            binding.radioButtonCustomDays.id -> {
                // Custom days schedule
                createWeekdaySchedule(selectedHabit.id, startTime, endTime, duration, notes)
            }
            else -> {
                // Default to one-time schedule
                createCustomSchedule(selectedHabit.id, selectedDateStr, startTime, endTime, duration, notes)
            }
        }
    }

    private fun createCustomSchedule(habitId: Int, date: String, startTime: String, endTime: String?, duration: Int?, notes: String) {
        // Basic validation: if endTime provided and earlier than startTime, ignore it
        val safeEndTime = if (endTime != null && endTime < startTime) {
            Log.w("CreateSchedule", "End time $endTime earlier than start time $startTime. Clearing end_time.")
            null
        } else endTime

        val request = com.example.progfront.data.model.CustomScheduleRequest(
            habitId = habitId,
            date = date,
            start_time = startTime,
            is_custom = true,
            end_time = safeEndTime,
            duration_minutes = duration,
            participantIds = null,
            notes = notes.ifEmpty { null }
        )

        Log.d("CreateSchedule", "Custom schedule request: $request")
        viewModel.createCustomSchedule(request)
    }

    private fun createRecurringSchedule(habitId: Int, startTime: String, pattern: String, endTime: String?, duration: Int?, notes: String) {
        val request = com.example.progfront.data.model.RecurringScheduleRequest(
            habitId = habitId,
            start_time = startTime,
            repeatPattern = pattern,
            is_custom = true,
            end_time = endTime,
            duration_minutes = duration,
            repeatDays = 30,
            participantIds = null,
            notes = notes.ifEmpty { null }
        )

        Log.d("CreateSchedule", "Recurring schedule request: $request")
        viewModel.createRecurringSchedule(request)
    }

    private fun createWeekdaySchedule(habitId: Int, startTime: String, endTime: String?, duration: Int?, notes: String) {
        val selectedDays = mutableListOf<Int>()

        if (binding.checkboxMonday.isChecked) selectedDays.add(1)
        if (binding.checkboxTuesday.isChecked) selectedDays.add(2)
        if (binding.checkboxWednesday.isChecked) selectedDays.add(3)
        if (binding.checkboxThursday.isChecked) selectedDays.add(4)
        if (binding.checkboxFriday.isChecked) selectedDays.add(5)
        if (binding.checkboxSaturday.isChecked) selectedDays.add(6)
        if (binding.checkboxSunday.isChecked) selectedDays.add(7)

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day for custom schedule", Toast.LENGTH_SHORT).show()
            return
        }

        val numberOfWeeks = binding.editTextNumberOfWeeks.text.toString().toIntOrNull() ?: 4

        val request = com.example.progfront.data.model.WeekdayScheduleRequest(
            habitId = habitId,
            start_time = startTime,
            daysOfWeek = selectedDays,
            numberOfWeeks = numberOfWeeks,
            duration_minutes = duration,
            end_time = endTime,
            participantIds = null,
            notes = notes.ifEmpty { null }
        )

        Log.d("CreateSchedule", "Weekday schedule request: $request")
        viewModel.createWeekdaySchedule(request)
    }
}
