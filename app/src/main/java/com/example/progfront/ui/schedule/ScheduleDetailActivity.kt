package com.example.progfront.ui.schedule

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progfront.R
import com.example.progfront.data.Result
import com.example.progfront.data.model.ProgressResponse
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.utils.TokenManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.util.Log
import android.app.TimePickerDialog
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.example.progfront.data.model.ProgressCreateRequest
import com.example.progfront.databinding.ActivityScheduleDetailBinding
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.EditText
import android.widget.CheckBox
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.example.progfront.databinding.DialogAddProgressBinding
import androidx.appcompat.app.AlertDialog
import android.widget.Button

class ScheduleDetailActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var binding: ActivityScheduleDetailBinding
    private val viewModel: ScheduleDetailViewModel by viewModels()

    // Reference to the add-progress dialog so we can dismiss / update it from observers
    private var addProgressDialog: AlertDialog? = null

    private var scheduleId: Int = -1
    private var currentSchedule: ScheduleResponse? = null
    private val progressAdapter = ProgressAdapter(mutableListOf())

    private val TAG = "ScheduleDetailActivity"

    // Time parsing/formatting (single authoritative definition)
    private val parsePatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss"
    )
    private val utc = TimeZone.getTimeZone("UTC")
    private val outTime = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = utc }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)

        setupRecycler()
        setupNotesInlineEditing()
        setupFab()
        setupObservers()

        scheduleId = intent.getIntExtra("schedule_id", -1)
        if (scheduleId == -1) {
            Toast.makeText(this, R.string.schedule_invalid_id, Toast.LENGTH_SHORT).show()
            finish(); return
        }
        fetchSchedule(scheduleId)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.schedule_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit -> { showEditScheduleDialog(); true }
        R.id.action_delete -> { confirmDeleteSchedule(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupRecycler() {
        binding.recyclerProgress.apply {
            layoutManager = LinearLayoutManager(this@ScheduleDetailActivity)
            adapter = progressAdapter
        }
    }

    private fun setupFab() {
        binding.fabAddProgress.setOnClickListener { showAddProgressDialog() }
    }

    private fun setupNotesInlineEditing() {
        binding.buttonEditNotes.setOnClickListener { enableNotesEditing(true) }
        binding.buttonCancelNotes.setOnClickListener { enableNotesEditing(false) }
        binding.buttonSaveNotes.setOnClickListener { saveNotesInline() }
    }

    private fun enableNotesEditing(editing: Boolean) = with(binding) {
        if (editing) inputNotesEdit.setText(textNotes.text)
        inputNotesLayout.visibility = if (editing) View.VISIBLE else View.GONE
        buttonSaveNotes.visibility = if (editing) View.VISIBLE else View.GONE
        buttonCancelNotes.visibility = if (editing) View.VISIBLE else View.GONE
        buttonEditNotes.visibility = if (editing) View.GONE else View.VISIBLE
        textNotes.visibility = if (editing) View.GONE else View.VISIBLE
    }

    private fun fetchSchedule(id: Int) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.auth_not_authenticated, Toast.LENGTH_SHORT).show()
            finish(); return
        }
        viewModel.loadScheduleById(id)
    }

    private fun setupObservers() {
        viewModel.scheduleDetail.observe(this) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    Log.d(TAG, "Schedule JSON: ${Gson().toJson(result.data)}")
                    currentSchedule = result.data
                    populate(result.data)
                }
                is Result.Error -> {
                    showLoading(false)
                    showError(result.message)
                }
            }
        }

        viewModel.updateResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    Toast.makeText(this, R.string.schedule_edit_success, Toast.LENGTH_SHORT).show()
                    fetchSchedule(scheduleId)
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(this, R.string.schedule_edit_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    Toast.makeText(this, R.string.schedule_deleted, Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(this, R.string.schedule_delete_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.progressResult.observe(this) { result ->
            when (result) {
                is Result.Loading -> showLoading(true)
                is Result.Success -> {
                    showLoading(false)
                    // if the add-progress dialog is open, dismiss it
                    addProgressDialog?.let { dlg ->
                        if (dlg.isShowing) dlg.dismiss()
                    }
                    addProgressDialog = null
                    Toast.makeText(this, R.string.schedule_progress_added, Toast.LENGTH_SHORT).show()
                    fetchSchedule(scheduleId)
                }
                is Result.Error -> {
                    showLoading(false)
                    // If dialog exists, re-enable its positive button so user can retry
                    addProgressDialog?.let { dlg ->
                        if (dlg.isShowing) {
                            try {
                                val btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE)
                                btn?.isEnabled = true
                            } catch (_: Exception) { /* ignore */ }
                        }
                    }
                    Toast.makeText(this, R.string.schedule_progress_add_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populate(schedule: ScheduleResponse) = with(binding) {
        textHabitName.text = schedule.habit?.name ?: getString(R.string.schedule_habit_fallback, schedule.habitId)
        textHabitDescription.text = schedule.habit?.description.orEmpty()
        textHabitDescription.visibility = if (schedule.habit?.description.isNullOrBlank()) View.GONE else View.VISIBLE
        textGoal.text = schedule.habit?.goal?.let { getString(R.string.schedule_goal, it) } ?: ""

        val start = formatTime(schedule.start_time)
        val end = schedule.end_time?.let { formatTime(it) }
        textScheduleTime.text = if (end.isNullOrBlank()) start else "$start - $end"

        applyStatusStyle(schedule.status)

        val notes = schedule.notes
        textNotes.text = if (notes.isNullOrBlank()) getString(R.string.schedule_notes_none) else notes

        val progressList = schedule.progress.orEmpty()
        textEmptyProgress.visibility = if (progressList.isEmpty()) View.VISIBLE else View.GONE

        val total = progressList.size
        val completed = progressList.count { it.is_completed }
        val percent = if (total > 0) (completed * 100 / total) else 0
        progressCircle.max = 100
        progressCircle.setProgress(percent, true)
        textProgressPercent.text = getString(R.string.schedule_progress_percent, percent)

        progressAdapter.updateList(progressList.sortedByDescending { it.date })
        enableNotesEditing(false)
    }

    private fun applyStatusStyle(status: String) = with(binding) {
        textStatusDetail.text = status
        when (status.lowercase(Locale.getDefault())) {
            "completed" -> textStatusDetail.setBackgroundResource(R.drawable.status_background_completed)
            "skipped" -> textStatusDetail.setBackgroundResource(R.drawable.status_background_skipped)
            else -> textStatusDetail.setBackgroundResource(R.drawable.status_background_planned)
        }
    }

    private fun formatTime(raw: String): String {
        // Fast path: extract HH:mm after 'T'
        val tIndex = raw.indexOf('T')
        if (tIndex >= 0 && raw.length >= tIndex + 6) {
            val candidate = raw.substring(tIndex + 1, tIndex + 6)
            if (candidate.matches(Regex("\\d{2}:\\d{2}"))) return candidate
        }
        // Fallback parse
        parsePatterns.forEach { p ->
            try {
                val sdf = SimpleDateFormat(p, Locale.getDefault())
                if (p.contains("'Z'")) sdf.timeZone = utc
                val d = sdf.parse(raw)
                if (d != null) return outTime.format(d)
            } catch (_: ParseException) {}
        }
        return raw.takeLast(8).take(5)
    }

    private fun showLoading(show: Boolean) { binding.loading.visibility = if (show) View.VISIBLE else View.GONE }

    private fun showError(msg: String?) = with(binding) {
        textError.visibility = View.VISIBLE
        textError.text = msg ?: getString(R.string.schedule_error_loading)
    }

    private fun showAddProgressDialog() {
        val dialogBinding = DialogAddProgressBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.schedule_add_progress))
            .setView(dialogBinding.root)
            .setNegativeButton(getString(R.string.schedule_dialog_cancel)) { d, _ -> d.dismiss() }
            .setPositiveButton(getString(R.string.schedule_dialog_save), null)
            .create()

        // Keep a reference so observers can dismiss or re-enable the button
        addProgressDialog = dialog

        // Clear our reference whenever the dialog is dismissed or cancelled
        dialog.setOnDismissListener { addProgressDialog = null }

        dialog.setOnShowListener {
            val btn: Button? = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btn?.setOnClickListener {
                // collect values from binding
                val loggedText = dialogBinding.inputLoggedTime.text?.toString().orEmpty()
                val loggedTime = loggedText.toIntOrNull()
                val notes = dialogBinding.inputNotes.text?.toString().takeIf { !it.isNullOrBlank() }
                val isCompleted = dialogBinding.checkCompleted.isChecked



                // disable button to prevent duplicate submissions; keep dialog shown until ViewModel responds
                btn.isEnabled = false

                // submit via ViewModel; the progressResult observer will dismiss or re-enable the button
                submitProgress(loggedTime, notes, isCompleted)
            }
        }

        dialog.show()
    }

    private fun submitProgress(loggedTime: Int?, notes: String?, isCompleted: Boolean) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.auth_not_authenticated, Toast.LENGTH_SHORT).show(); return
        }
        val schedule = currentSchedule ?: return
        // Delegate to ViewModel which builds the request and calls repository
        viewModel.addProgressForSchedule(schedule, loggedTime, notes, isCompleted)
    }

    private fun showEditScheduleDialog() {
        val schedule = currentSchedule ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_schedule, null)
        val inputStart = view.findViewById<TextInputEditText>(R.id.inputStartTime)
        val inputEnd = view.findViewById<TextInputEditText>(R.id.inputEndTime)
        val inputDuration = view.findViewById<TextInputEditText>(R.id.inputDuration)
        val radioStatus = view.findViewById<android.widget.RadioGroup>(R.id.radioStatus)
        val inputNotes = view.findViewById<TextInputEditText>(R.id.inputScheduleNotes)

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
                if (endIso != null && endTxt < startTxt) {
                    Toast.makeText(this, R.string.schedule_edit_end_before_start, Toast.LENGTH_SHORT).show()
                    endIso = null
                }

                val body = mutableMapOf<String, Any?>().apply {
                    this["start_time"] = startIso
                    this["end_time"] = endIso
                    this["duration_minutes"] = duration
                    this["status"] = status
                    this["notes"] = notesTxt.ifBlank { null }
                }
                Log.d(TAG, "PATCH body: ${Gson().toJson(body)}")
                submitScheduleUpdate(schedule.id, body)
            }
            .setNegativeButton(getString(R.string.schedule_dialog_cancel)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun submitScheduleUpdate(id: Int, body: Map<String, Any?>) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.auth_not_authenticated, Toast.LENGTH_SHORT).show(); return
        }
        viewModel.updateSchedule(id, body)
    }

    private fun confirmDeleteSchedule() {
        val schedule = currentSchedule ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.schedule_delete_title))
            .setMessage(getString(R.string.schedule_delete_confirm))
            .setPositiveButton(getString(R.string.delete_yes)) { d, _ -> d.dismiss(); deleteSchedule(schedule.id) }
            .setNegativeButton(getString(R.string.delete_no)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun deleteSchedule(id: Int) {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.auth_not_authenticated, Toast.LENGTH_SHORT).show(); return
        }
        viewModel.deleteSchedule(id)
    }

    private fun saveNotesInline() {
        val schedule = currentSchedule ?: return
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.auth_not_authenticated, Toast.LENGTH_SHORT).show(); return
        }
        val newNotes = binding.inputNotesEdit.text?.toString()?.trim().orEmpty()
        val body = mapOf<String, Any?>("notes" to newNotes.ifBlank { null })
        binding.buttonSaveNotes.isEnabled = false
        binding.buttonCancelNotes.isEnabled = false
        viewModel.updateSchedule(schedule.id, body)
        binding.buttonSaveNotes.isEnabled = true
        binding.buttonCancelNotes.isEnabled = true
        enableNotesEditing(false)
    }
}

// Adapter updated to support list updates without recreating the adapter instance
class ProgressAdapter(private val items: MutableList<ProgressResponse>) : RecyclerView.Adapter<ProgressAdapter.VH>() {

    fun updateList(newItems: List<ProgressResponse>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

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
        holder.primary.text = ctx.getString(R.string.schedule_progress_list_primary, p.date.take(10), status)
        val logged = ctx.getString(R.string.schedule_logged_time, p.logged_time ?: 0)
        holder.secondary.text = if (!p.notes.isNullOrBlank()) ctx.getString(R.string.schedule_progress_list_secondary_with_notes, logged, p.notes) else logged
    }
}
