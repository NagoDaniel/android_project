package com.example.progfront.ui.schedule

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progfront.R
import com.example.progfront.data.model.ProgressResponse
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import android.view.ViewGroup
import android.view.LayoutInflater
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.EditText
import android.widget.CheckBox
import com.example.progfront.data.model.ProgressCreateRequest
import android.util.Log
import android.app.TimePickerDialog
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.example.progfront.databinding.ActivityScheduleDetailBinding

class ScheduleDetailActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager

    // View Binding
    private lateinit var binding: ActivityScheduleDetailBinding

    private lateinit var textHabitName: TextView
    private lateinit var textHabitDescription: TextView
    private lateinit var textScheduleTime: TextView
    private lateinit var textStatusDetail: TextView
    private lateinit var textGoal: TextView
    private lateinit var progressCircle: CircularProgressIndicator
    private lateinit var textProgressPercent: TextView
    private lateinit var textNotes: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var loading: View
    private lateinit var textError: TextView
    private lateinit var fabAdd: FloatingActionButton

    // Inline notes editing views
    private lateinit var inputNotesLayout: TextInputLayout
    private lateinit var inputNotesEdit: TextInputEditText
    private lateinit var buttonEditNotes: MaterialButton
    private lateinit var buttonSaveNotes: MaterialButton
    private lateinit var buttonCancelNotes: MaterialButton

    private val timePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    private val utc = TimeZone.getTimeZone("UTC")
    private val outTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    private var scheduleId: Int = -1
    private var currentSchedule: ScheduleResponse? = null
    private val TAG = "ScheduleDetailActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate binding and set content view
        binding = ActivityScheduleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)
        bindViews()
        scheduleId = intent.getIntExtra("schedule_id", -1)
        if (scheduleId == -1) {
            Toast.makeText(this, "Invalid schedule", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        fabAdd.setOnClickListener { showAddProgressDialog() }
        fetchSchedule(scheduleId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.schedule_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                showEditScheduleDialog()
                true
            }
            R.id.action_delete -> {
                confirmDeleteSchedule()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // no view binding initially, just replaced this
    private fun bindViews() {
        // Map all referenced views from binding
        textHabitName = binding.textHabitName
        textHabitDescription = binding.textHabitDescription
        textScheduleTime = binding.textScheduleTime
        textStatusDetail = binding.textStatusDetail
        textGoal = binding.textGoal
        progressCircle = binding.progressCircle
        textProgressPercent = binding.textProgressPercent
        textNotes = binding.textNotes
        // Inline notes editing views
        inputNotesLayout = binding.inputNotesLayout
        inputNotesEdit = binding.inputNotesEdit
        buttonEditNotes = binding.buttonEditNotes
        buttonSaveNotes = binding.buttonSaveNotes
        buttonCancelNotes = binding.buttonCancelNotes

        recycler = binding.recyclerProgress
        textEmpty = binding.textEmptyProgress
        loading = binding.loading
        textError = binding.textError
        fabAdd = binding.fabAddProgress
        recycler.layoutManager = LinearLayoutManager(this)

        // Setup notes edit handlers
        buttonEditNotes.setOnClickListener { enableNotesEditing(true) }
        buttonCancelNotes.setOnClickListener { enableNotesEditing(false) }
        buttonSaveNotes.setOnClickListener { saveNotesInline() }
    }

    private fun enableNotesEditing(editing: Boolean) {
        if (editing) {
            inputNotesEdit.setText(textNotes.text)
        }
        inputNotesLayout.visibility = if (editing) View.VISIBLE else View.GONE
        buttonSaveNotes.visibility = if (editing) View.VISIBLE else View.GONE
        buttonCancelNotes.visibility = if (editing) View.VISIBLE else View.GONE
        buttonEditNotes.visibility = if (editing) View.GONE else View.VISIBLE
        textNotes.visibility = if (editing) View.GONE else View.VISIBLE
    }

    private fun fetchSchedule(id: Int) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        showLoading(true)
        RetrofitClient.instance.getScheduleById("Bearer $token", id)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val body = response.body()
                        Log.d(TAG, "GET /schedule/$id success code=${response.code()}")
                        if (body != null) {
                            Log.d(TAG, "Schedule raw JSON: ${Gson().toJson(body)}")
                            Log.d(TAG, "Progress entries count=${body.progress?.size ?: 0}")
                            body.progress?.forEachIndexed { idx, p ->
                                Log.d(TAG, "progress[$idx]: date=${p.date} completed=${p.is_completed} logged_time=${p.logged_time} notes=${p.notes}")
                            }
                            populate(body)
                        } else {
                            Log.e(TAG, "Schedule body null")
                            showError()
                        }
                    } else {
                        Log.e(TAG, "GET /schedule/$id failed code=${response.code()} body=${response.errorBody()?.string()}")
                        showError()
                    }
                }
                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "GET /schedule/$id network failure: ${t.message}", t)
                    showError(t.message)
                }
            })
    }

    private fun populate(schedule: ScheduleResponse) {
        currentSchedule = schedule
        textHabitName.text = schedule.habit?.name ?: "Habit #${schedule.habitId}"
        textHabitDescription.text = schedule.habit?.description.orEmpty()
        textHabitDescription.visibility = if (schedule.habit?.description.isNullOrBlank()) View.GONE else View.VISIBLE
        textGoal.text = schedule.habit?.goal?.let { getString(R.string.schedule_goal, it) } ?: ""

        val start = formatTime(schedule.start_time)
        val end = schedule.end_time?.let { formatTime(it) }
        textScheduleTime.text = if (end.isNullOrBlank()) start else "$start - $end"

        // Status chip
        applyStatusStyle(schedule.status)

        // Notes
        val notes = schedule.notes
        textNotes.text = if (notes.isNullOrBlank()) getString(R.string.schedule_notes_none) else notes

        val progressList = schedule.progress.orEmpty()
        textEmpty.visibility = if (progressList.isEmpty()) View.VISIBLE else View.GONE

        val total = progressList.size
        val completed = progressList.count { it.is_completed }
        val percent = if (total > 0) (completed * 100 / total) else 0
        Log.d(TAG, "Computed percent: completed=$completed total=$total percent=$percent")
        progressCircle.max = 100
        progressCircle.setProgress(percent, true)
        textProgressPercent.text = getString(R.string.schedule_progress_percent, percent)

        val sorted = progressList.sortedByDescending { it.date }
        recycler.adapter = ProgressAdapter(sorted)

        // Ensure edit mode is off when loading new schedule
        enableNotesEditing(false)
    }

    private fun applyStatusStyle(status: String) {
        textStatusDetail.text = status
        when (status.lowercase(Locale.getDefault())) {
            "completed" -> textStatusDetail.setBackgroundResource(R.drawable.status_background_completed)
            "skipped" -> textStatusDetail.setBackgroundResource(R.drawable.status_background_skipped)
            else -> textStatusDetail.setBackgroundResource(R.drawable.status_background_planned)
        }
    }

    private fun formatTime(raw: String): String {
        timePatterns.forEach { p ->
            try {
                val sdf = SimpleDateFormat(p, Locale.getDefault())
                if (p.contains("'Z'")) sdf.timeZone = utc
                val d = sdf.parse(raw)
                if (d != null) return outTime.format(d)
            } catch (_: ParseException) {}
        }
        return raw.takeLast(8).take(5)
    }

    private fun showLoading(show: Boolean) {
        loading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String? = null) {
        textError.visibility = View.VISIBLE
        textError.text = msg ?: getString(R.string.schedule_error_loading)
    }

    private fun showAddProgressDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_progress, null)
        val inputLogged = view.findViewById<EditText>(R.id.inputLoggedTime)
        val inputNotes = view.findViewById<EditText>(R.id.inputNotes)
        val checkCompleted = view.findViewById<CheckBox>(R.id.checkCompleted)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.schedule_add_progress))
            .setView(view)
            .setPositiveButton(getString(R.string.schedule_dialog_save)) { d, _ ->
                d.dismiss()
                submitProgress(
                    loggedTime = inputLogged.text.toString().toIntOrNull(),
                    notes = inputNotes.text.toString().takeIf { it.isNotBlank() },
                    isCompleted = checkCompleted.isChecked
                )
            }
            .setNegativeButton(getString(R.string.schedule_dialog_cancel)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun submitProgress(loggedTime: Int?, notes: String?, isCompleted: Boolean) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val schedule = currentSchedule ?: return
        // Determine date to send: prefer schedule.date else first 10 chars of start_time
        val dateStr = schedule.date.ifBlank { schedule.start_time.take(10) }
        val request = ProgressCreateRequest(
            scheduleId = schedule.id,
            date = dateStr,
            logged_time = loggedTime,
            notes = notes,
            is_completed = isCompleted
        )
        showLoading(true)
        RetrofitClient.instance.createProgress("Bearer $token", request)
            .enqueue(object : Callback<ProgressResponse> {
                override fun onResponse(call: Call<ProgressResponse>, response: Response<ProgressResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_progress_added), Toast.LENGTH_SHORT).show()
                        // Refresh schedule to update list & percentage
                        fetchSchedule(schedule.id)
                    } else {
                        Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_progress_add_error), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ProgressResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_progress_add_error), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showEditScheduleDialog() {
        val schedule = currentSchedule ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_schedule, null)
        val inputStart = view.findViewById<TextInputEditText>(R.id.inputStartTime)
        val inputEnd = view.findViewById<TextInputEditText>(R.id.inputEndTime)
        val inputDuration = view.findViewById<TextInputEditText>(R.id.inputDuration)
        val radioStatus = view.findViewById<RadioGroup>(R.id.radioStatus)
        val inputNotes = view.findViewById<TextInputEditText>(R.id.inputScheduleNotes)

        // Prefill
        inputStart.setText(formatTime(schedule.start_time))
        inputEnd.setText(schedule.end_time?.let { formatTime(it) } ?: "")
        inputDuration.setText(schedule.duration_minutes?.toString() ?: "")
        inputNotes.setText(schedule.notes ?: "")
        when (schedule.status) {
            "Completed" -> radioStatus.check(R.id.statusCompleted)
            "Skipped" -> radioStatus.check(R.id.statusSkipped)
            else -> radioStatus.check(R.id.statusPlanned)
        }

        fun pickTime(target: TextInputEditText) {
            val initial = (target.text?.toString()?.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "00:00").split(":")
            val h = initial[0].toIntOrNull() ?: 0
            val m = initial[1].toIntOrNull() ?: 0
            TimePickerDialog(this, { _, hour, minute ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
            }, h, m, true).show()
        }
        inputStart.setOnClickListener { pickTime(inputStart) }
        inputEnd.setOnClickListener { pickTime(inputEnd) }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.schedule_edit_title))
            .setView(view)
            .setPositiveButton(getString(R.string.schedule_dialog_save)) { d, _ ->
                d.dismiss()
                val datePart = schedule.start_time.take(10)
                val startTxt = inputStart.text?.toString()?.trim().orEmpty()
                if (!startTxt.matches(Regex("\\d{2}:\\d{2}"))) {
                    Toast.makeText(this, R.string.schedule_edit_invalid_start, Toast.LENGTH_SHORT).show(); return@setPositiveButton
                }
                val endTxt = inputEnd.text?.toString()?.trim().orEmpty()
                val duration = inputDuration.text?.toString()?.trim()?.toIntOrNull()
                val notesTxt = inputNotes.text?.toString()?.trim().orEmpty()

                val status = when (radioStatus.checkedRadioButtonId) {
                    R.id.statusCompleted -> "Completed"
                    R.id.statusSkipped -> "Skipped"
                    else -> "Planned"
                }

                val startIso = datePart + "T" + startTxt + ":00"
                var endIso = if (endTxt.matches(Regex("\\d{2}:\\d{2}"))) datePart + "T" + endTxt + ":00" else null

                // basic validation: ensure end >= start if both provided
                if (endIso != null && endTxt < startTxt) {
                    Toast.makeText(this, getString(R.string.schedule_edit_end_before_start), Toast.LENGTH_SHORT).show()
                    endIso = null
                }

                val body = mutableMapOf<String, Any?>()
                body["start_time"] = startIso
                body["end_time"] = endIso
                body["duration_minutes"] = duration
                body["status"] = status
                body["notes"] = notesTxt.ifBlank { null }

                Log.d(TAG, "PATCH body: ${Gson().toJson(body)}")
                submitScheduleUpdate(schedule.id, body)
            }
            .setNegativeButton(getString(R.string.schedule_dialog_cancel)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun submitScheduleUpdate(id: Int, body: Map<String, Any?>) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        showLoading(true)
        RetrofitClient.instance.updateSchedule("Bearer $token", id, body)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@ScheduleDetailActivity, R.string.schedule_edit_success, Toast.LENGTH_SHORT).show()
                        fetchSchedule(id)
                    } else {
                        Log.e(TAG, "PATCH /schedule/$id failed code=${response.code()} body=${response.errorBody()?.string()}")
                        Toast.makeText(this@ScheduleDetailActivity, R.string.schedule_edit_failed, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "PATCH /schedule/$id network failure: ${t.message}", t)
                    Toast.makeText(this@ScheduleDetailActivity, R.string.schedule_edit_failed, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmDeleteSchedule() {
        val schedule = currentSchedule ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.schedule_delete_title))
            .setMessage(getString(R.string.schedule_delete_confirm))
            .setPositiveButton(getString(R.string.delete_yes)) { d, _ ->
                d.dismiss(); deleteSchedule(schedule.id)
            }
            .setNegativeButton(getString(R.string.delete_no)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun deleteSchedule(id: Int) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show(); return
        }
        showLoading(true)
        RetrofitClient.instance.deleteSchedule("Bearer $token", id)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_deleted), Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_delete_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_delete_failed), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun saveNotesInline() {
        val schedule = currentSchedule ?: return
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val newNotes = inputNotesEdit.text?.toString()?.trim().orEmpty()
        val body = mapOf<String, Any?>(
            "notes" to newNotes.ifBlank { null }
        )
        // Disable while saving
        buttonSaveNotes.isEnabled = false
        buttonCancelNotes.isEnabled = false
        showLoading(true)
        RetrofitClient.instance.updateSchedule("Bearer $token", schedule.id, body)
            .enqueue(object : Callback<ScheduleResponse> {
                override fun onResponse(call: Call<ScheduleResponse>, response: Response<ScheduleResponse>) {
                    showLoading(false)
                    buttonSaveNotes.isEnabled = true
                    buttonCancelNotes.isEnabled = true
                    if (response.isSuccessful) {
                        enableNotesEditing(false)
                        fetchSchedule(schedule.id)
                    } else {
                        Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_edit_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ScheduleResponse>, t: Throwable) {
                    showLoading(false)
                    buttonSaveNotes.isEnabled = true
                    buttonCancelNotes.isEnabled = true
                    Toast.makeText(this@ScheduleDetailActivity, getString(R.string.schedule_edit_failed), Toast.LENGTH_SHORT).show()
                }
            })
    }
}

class ProgressAdapter(private val items: List<ProgressResponse>) : RecyclerView.Adapter<ProgressAdapter.VH>() {
    // Expose items for aggregation merge logic
    fun getItems(): List<ProgressResponse> = items

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val primary: TextView = view.findViewById(android.R.id.text1)
        val secondary: TextView = view.findViewById(android.R.id.text2)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        val ctx = holder.itemView.context
        val status = ctx.getString(if (p.is_completed) R.string.schedule_completed else R.string.schedule_pending)
        holder.primary.text = "${p.date.take(10)} • $status"
        val logged = ctx.getString(R.string.schedule_logged_time, p.logged_time ?: 0)
        val notesPart = if (!p.notes.isNullOrBlank()) " | ${p.notes}" else ""
        holder.secondary.text = "$logged$notesPart"
    }
}
