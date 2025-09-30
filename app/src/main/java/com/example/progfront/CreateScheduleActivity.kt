package com.example.progfront

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.progfront.network.*
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class CreateScheduleActivity : AppCompatActivity(), AddHabitDialogFragment.OnHabitCreatedListener {

    private lateinit var spinnerHabits: Spinner
    private lateinit var buttonCreateNewHabit: Button
    private lateinit var buttonSelectDate: Button
    private lateinit var spinnerStartHour: Spinner
    private lateinit var spinnerStartMinute: Spinner
    private lateinit var spinnerEndHour: Spinner
    private lateinit var spinnerEndMinute: Spinner
    private lateinit var editTextDuration: EditText
    private lateinit var radioGroupRepeatPattern: RadioGroup
    private lateinit var layoutCustomDays: LinearLayout
    private lateinit var layoutNumberOfWeeks: LinearLayout
    private lateinit var editTextNumberOfWeeks: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var buttonCancel: Button
    private lateinit var buttonCreate: Button

    private var habits = mutableListOf<HabitResponse>()
    private var selectedDate: Calendar = Calendar.getInstance()
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_schedule)

        tokenManager = TokenManager(this)

        initializeViews()
        setupTimeSpinners()
        setupListeners()
        loadHabits()
    }

    private fun initializeViews() {
        spinnerHabits = findViewById(R.id.spinnerHabits)
        buttonCreateNewHabit = findViewById(R.id.buttonCreateNewHabit)
        buttonSelectDate = findViewById(R.id.buttonSelectDate)
        spinnerStartHour = findViewById(R.id.spinnerStartHour)
        spinnerStartMinute = findViewById(R.id.spinnerStartMinute)
        spinnerEndHour = findViewById(R.id.spinnerEndHour)
        spinnerEndMinute = findViewById(R.id.spinnerEndMinute)
        editTextDuration = findViewById(R.id.editTextDuration)
        radioGroupRepeatPattern = findViewById(R.id.radioGroupRepeatPattern)
        layoutCustomDays = findViewById(R.id.layoutCustomDays)
        layoutNumberOfWeeks = findViewById(R.id.layoutNumberOfWeeks)
        editTextNumberOfWeeks = findViewById(R.id.editTextNumberOfWeeks)
        editTextNotes = findViewById(R.id.editTextNotes)
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonCreate = findViewById(R.id.buttonCreate)

        updateDateButton()
    }

    private fun setupTimeSpinners() {
        // Setup hour spinners (0-23)
        val hours = (0..23).map { String.format("%02d", it) }
        val hourAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hours)
        hourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStartHour.adapter = hourAdapter
        spinnerEndHour.adapter = hourAdapter

        // Setup minute spinners (0, 15, 30, 45)
        val minutes = listOf("00", "15", "30", "45")
        val minuteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, minutes)
        minuteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStartMinute.adapter = minuteAdapter
        spinnerEndMinute.adapter = minuteAdapter

        // Set default times
        spinnerStartHour.setSelection(9) // 9 AM
        spinnerStartMinute.setSelection(0) // 00 minutes
        spinnerEndHour.setSelection(10) // 10 AM
        spinnerEndMinute.setSelection(0) // 00 minutes
    }

    private fun setupListeners() {
        buttonCreateNewHabit.setOnClickListener {
            showAddHabitDialog()
        }

        buttonSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        radioGroupRepeatPattern.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonCustomDays -> {
                    layoutCustomDays.visibility = View.VISIBLE
                    layoutNumberOfWeeks.visibility = View.VISIBLE
                }
                else -> {
                    layoutCustomDays.visibility = View.GONE
                    layoutNumberOfWeeks.visibility = View.GONE
                }
            }
        }

        buttonCancel.setOnClickListener {
            finish()
        }

        buttonCreate.setOnClickListener {
            createSchedule()
        }
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
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        buttonSelectDate.text = dateFormat.format(selectedDate.time)
    }

    private fun loadHabits() {
        val token = tokenManager.getBearerToken()

        if (token == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RetrofitClient.instance.getHabits(token)
            .enqueue(object : Callback<List<HabitResponse>> {
                override fun onResponse(call: Call<List<HabitResponse>>, response: Response<List<HabitResponse>>) {
                    Log.d("CreateSchedule", "Habits response: ${response.body()}")
                    if (response.isSuccessful) {
                        response.body()?.let { habitList ->
                            habits.clear()
                            habits.addAll(habitList)
                            updateHabitsSpinner()

                            if (habits.isEmpty()) {
                                Toast.makeText(this@CreateScheduleActivity, "No habits found. Please create a habit first.", Toast.LENGTH_LONG).show()
                                showAddHabitDialog()
                            }
                        }
                    } else {
                        Log.e("CreateSchedule", "Failed to load habits: ${response.errorBody()?.string()}")
                        Toast.makeText(this@CreateScheduleActivity, "Failed to load habits", Toast.LENGTH_SHORT).show()
                        showAddHabitDialog()
                    }
                }

                override fun onFailure(call: Call<List<HabitResponse>>, t: Throwable) {
                    Log.e("CreateSchedule", "Network error loading habits: ${t.message}", t)
                    Toast.makeText(this@CreateScheduleActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    showAddHabitDialog()
                }
            })
    }

    private fun updateHabitsSpinner() {
        val habitNames = habits.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, habitNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerHabits.adapter = adapter
    }

    private fun showAddHabitDialog() {
        val dialog = AddHabitDialogFragment()
        dialog.setOnHabitCreatedListener(this)
        dialog.show(supportFragmentManager, "AddHabitDialog")
    }

    override fun onHabitCreated(habit: HabitResponse) {
        habits.add(habit)
        updateHabitsSpinner()
        spinnerHabits.setSelection(habits.size - 1)
        Toast.makeText(this, "Habit '${habit.name}' created successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun createSchedule() {
        if (habits.isEmpty()) {
            Toast.makeText(this, "Please create a habit first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedHabitIndex = spinnerHabits.selectedItemPosition
        if (selectedHabitIndex < 0) {
            Toast.makeText(this, "Please select a habit", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedHabit = habits[selectedHabitIndex]

        // Get time values
        val startHour = spinnerStartHour.selectedItem.toString().toInt()
        val startMinute = spinnerStartMinute.selectedItem.toString().toInt()
        val endHour = spinnerEndHour.selectedItem.toString().toInt()
        val endMinute = spinnerEndMinute.selectedItem.toString().toInt()

        // Set start time
        val startTime = Calendar.getInstance().apply {
            timeInMillis = selectedDate.timeInMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Set end time if different from start
        val endTime = if (endHour != startHour || endMinute != startMinute) {
            Calendar.getInstance().apply {
                timeInMillis = selectedDate.timeInMillis
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else null

        val durationText = editTextDuration.text.toString()
        val duration = if (durationText.isNotEmpty()) durationText.toIntOrNull() else null

        val notes = editTextNotes.text.toString().trim().ifEmpty { null }

        // Get selected repeat pattern
        val selectedRadioId = radioGroupRepeatPattern.checkedRadioButtonId
        when (selectedRadioId) {
            R.id.radioButtonNone -> {
                createCustomSchedule(selectedHabit.id, startTime, endTime, duration, notes)
            }
            R.id.radioButtonDaily -> {
                createRecurringSchedule(selectedHabit.id, startTime, endTime, "daily", duration, notes)
            }
            R.id.radioButtonWeekdays -> {
                createRecurringSchedule(selectedHabit.id, startTime, endTime, "weekdays", duration, notes)
            }
            R.id.radioButtonWeekends -> {
                createRecurringSchedule(selectedHabit.id, startTime, endTime, "weekends", duration, notes)
            }
            R.id.radioButtonCustomDays -> {
                createWeekdaySchedule(selectedHabit.id, startTime, endTime, duration, notes)
            }
            else -> {
                createCustomSchedule(selectedHabit.id, startTime, endTime, duration, notes)
            }
        }
    }

    private fun createCustomSchedule(
        habitId: Int,
        startTime: Calendar,
        endTime: Calendar?,
        duration: Int?,
        notes: String?
    ) {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        val scheduleRequest = CustomScheduleRequest(
            habitId = habitId,
            date = isoFormat.format(selectedDate.time),
            start_time = isoFormat.format(startTime.time),
            is_custom = true,
            end_time = endTime?.let { isoFormat.format(it.time) },
            duration_minutes = duration,
            participantIds = null,
            notes = notes
        )

        Log.d("CreateSchedule", "Creating custom schedule: $scheduleRequest")

        val token = tokenManager.getBearerToken()
        if (token == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.createCustomSchedule(token, scheduleRequest)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    Log.d("CreateSchedule", "Custom schedule response: ${response.body()}")
                    if (response.isSuccessful) {
                        Toast.makeText(this@CreateScheduleActivity, "Schedule created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("CreateSchedule", "Failed to create schedule: ${response.errorBody()?.string()}")
                        Toast.makeText(this@CreateScheduleActivity, "Failed to create schedule", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    Log.e("CreateSchedule", "Network error creating schedule: ${t.message}", t)
                    Toast.makeText(this@CreateScheduleActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createRecurringSchedule(
        habitId: Int,
        startTime: Calendar,
        endTime: Calendar?,
        repeatPattern: String,
        duration: Int?,
        notes: String?
    ) {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        val scheduleRequest = RecurringScheduleRequest(
            habitId = habitId,
            start_time = isoFormat.format(startTime.time),
            repeatPattern = repeatPattern,
            is_custom = true,
            end_time = endTime?.let { isoFormat.format(it.time) },
            duration_minutes = duration,
            repeatDays = 30,
            participantIds = null,
            notes = notes
        )

        Log.d("CreateSchedule", "Creating recurring schedule: $scheduleRequest")

        val token = tokenManager.getBearerToken()
        if (token == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.createRecurringSchedule(token, scheduleRequest)
            .enqueue(object : Callback<List<ScheduleResponse>> {
                override fun onResponse(call: Call<List<ScheduleResponse>>, response: Response<List<ScheduleResponse>>) {
                    Log.d("CreateSchedule", "Recurring schedule response: ${response.body()}")
                    if (response.isSuccessful) {
                        val scheduleCount = response.body()?.size ?: 0
                        Toast.makeText(this@CreateScheduleActivity, "$scheduleCount schedules created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("CreateSchedule", "Failed to create recurring schedule: ${response.errorBody()?.string()}")
                        Toast.makeText(this@CreateScheduleActivity, "Failed to create schedule", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<ScheduleResponse>>, t: Throwable) {
                    Log.e("CreateSchedule", "Network error creating recurring schedule: ${t.message}", t)
                    Toast.makeText(this@CreateScheduleActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createWeekdaySchedule(
        habitId: Int,
        startTime: Calendar,
        endTime: Calendar?,
        duration: Int?,
        notes: String?
    ) {
        val selectedDays = mutableListOf<Int>()

        if (findViewById<CheckBox>(R.id.checkboxMonday).isChecked) selectedDays.add(1)
        if (findViewById<CheckBox>(R.id.checkboxTuesday).isChecked) selectedDays.add(2)
        if (findViewById<CheckBox>(R.id.checkboxWednesday).isChecked) selectedDays.add(3)
        if (findViewById<CheckBox>(R.id.checkboxThursday).isChecked) selectedDays.add(4)
        if (findViewById<CheckBox>(R.id.checkboxFriday).isChecked) selectedDays.add(5)
        if (findViewById<CheckBox>(R.id.checkboxSaturday).isChecked) selectedDays.add(6)
        if (findViewById<CheckBox>(R.id.checkboxSunday).isChecked) selectedDays.add(7)

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day", Toast.LENGTH_SHORT).show()
            return
        }

        val numberOfWeeks = editTextNumberOfWeeks.text.toString().toIntOrNull() ?: 4
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

        val scheduleRequest = WeekdayScheduleRequest(
            habitId = habitId,
            start_time = isoFormat.format(startTime.time),
            daysOfWeek = selectedDays,
            numberOfWeeks = numberOfWeeks,
            duration_minutes = duration,
            end_time = endTime?.let { isoFormat.format(it.time) },
            participantIds = null,
            notes = notes
        )

        Log.d("CreateSchedule", "Creating weekday schedule: $scheduleRequest")

        val token = tokenManager.getBearerToken()
        if (token == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.createWeekdaySchedule(token, scheduleRequest)
            .enqueue(object : Callback<List<ScheduleResponse>> {
                override fun onResponse(call: Call<List<ScheduleResponse>>, response: Response<List<ScheduleResponse>>) {
                    Log.d("CreateSchedule", "Weekday schedule response: ${response.body()}")
                    if (response.isSuccessful) {
                        val scheduleCount = response.body()?.size ?: 0
                        Toast.makeText(this@CreateScheduleActivity, "$scheduleCount schedules created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Log.e("CreateSchedule", "Failed to create weekday schedule: ${response.errorBody()?.string()}")
                        Toast.makeText(this@CreateScheduleActivity, "Failed to create schedule", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<ScheduleResponse>>, t: Throwable) {
                    Log.e("CreateSchedule", "Network error creating weekday schedule: ${t.message}", t)
                    Toast.makeText(this@CreateScheduleActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
