package com.example.progfront.ui.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
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
    private lateinit var tokenManager: TokenManager
    private val viewModel: CreateScheduleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Use applicationContext for long-lived helpers
        tokenManager = TokenManager(applicationContext)
        setupTimeSpinners()
        setupListeners()
        setupObservers()
        loadHabits()
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
        binding.spinnerStartHour.setSelection(hours.indexOf("09"))
        binding.spinnerStartMinute.setSelection(0)
        binding.spinnerEndHour.setSelection(hours.indexOf("10"))
        binding.spinnerEndMinute.setSelection(0)
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
            viewModel.updateTimes(
                binding.spinnerStartHour.selectedItem.toString(),
                binding.spinnerStartMinute.selectedItem.toString(),
                binding.spinnerEndHour.selectedItem.toString(),
                binding.spinnerEndMinute.selectedItem.toString()
            )
            viewModel.updateNotes(binding.editTextNotes.text.toString())
            viewModel.updateDuration(binding.editTextDuration.text.toString())
            viewModel.createSchedule()
        }

        binding.radioGroupRepeatPattern.setOnCheckedChangeListener { _, checkedId ->
            val pattern = when (checkedId) {
                binding.radioButtonNone.id -> CreateScheduleViewModel.RepeatPattern.NONE
                binding.radioButtonDaily.id -> CreateScheduleViewModel.RepeatPattern.DAILY
                binding.radioButtonWeekdays.id -> CreateScheduleViewModel.RepeatPattern.WEEKDAYS
                binding.radioButtonWeekends.id -> CreateScheduleViewModel.RepeatPattern.WEEKENDS
                binding.radioButtonCustomDays.id -> CreateScheduleViewModel.RepeatPattern.CUSTOM
                else -> CreateScheduleViewModel.RepeatPattern.NONE
            }
            viewModel.updateRepeatPattern(pattern)
        }

        // Update selected days when checkboxes change
        val checkboxes = listOf(
            binding.checkboxMonday to 1,
            binding.checkboxTuesday to 2,
            binding.checkboxWednesday to 3,
            binding.checkboxThursday to 4,
            binding.checkboxFriday to 5,
            binding.checkboxSaturday to 6,
            binding.checkboxSunday to 7
        )

        checkboxes.forEach { (checkbox, _) ->
            checkbox.setOnCheckedChangeListener { _, _ ->
                val selectedDays = checkboxes
                    .filter { (cb, _) -> cb.isChecked }
                    .map { (_, day) -> day }
                viewModel.updateSelectedDays(selectedDays)
            }
        }

        binding.editTextNumberOfWeeks.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.toIntOrNull()?.let { weeks ->
                    viewModel.updateNumberOfWeeks(weeks)
                }
            }
        })
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // Update loading state
            binding.buttonCreate.isEnabled = !state.isLoading

            // Update error messages
            state.error?.let { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }

            // Update habit spinner when habits change (previous code compared state.habits to itself)
            val currentAdapter = binding.spinnerHabits.adapter
            val currentCount = currentAdapter?.count ?: 0
            if (state.habits.size != currentCount) {
                setupHabitSpinner(state.habits)
            } else if (state.habits.isNotEmpty()) {
                // Ensure spinner selection matches UI state
                binding.spinnerHabits.setSelection(state.selectedHabitPosition.coerceIn(0, state.habits.size - 1))
            }

            // Update date button
            binding.buttonSelectDate.text = state.formattedDate

            // Update custom days layout visibility
            binding.layoutCustomDays.visibility = if (state.showCustomDaysLayout) View.VISIBLE else View.GONE
            binding.layoutNumberOfWeeks.visibility = if (state.showCustomDaysLayout) View.VISIBLE else View.GONE

            // Handle successful creation
            if (state.createSuccess) {
                Toast.makeText(this, "Schedule created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadHabits() {
        if (tokenManager.getAccessToken() == null) {
            Toast.makeText(this, "Authentication token not found.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.loadHabits()
    }

    private fun setupHabitSpinner(habits: List<HabitResponse>) {
        val habitNames = habits.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, habitNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHabits.adapter = adapter
        binding.spinnerHabits.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.updateSelectedHabitPosition(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set selection from viewModel state if available
        val positionToSelect = viewModel.uiState.value?.selectedHabitPosition ?: 0
        if (habits.isNotEmpty()) {
            binding.spinnerHabits.setSelection(positionToSelect.coerceIn(0, habits.size - 1))
        }
    }

    private fun showAddHabitDialog() {
        val dialog = AddHabitDialogFragment()
        dialog.show(supportFragmentManager, "AddHabitDialogFragment")
    }

    private fun showDatePickerDialog() {
        val state = viewModel.uiState.value ?: return
        val calendar = state.selectedDate

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                viewModel.updateDate(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onHabitCreated(habit: HabitResponse) {
        viewModel.loadHabits() // Reload habits to include the new one
    }
}
