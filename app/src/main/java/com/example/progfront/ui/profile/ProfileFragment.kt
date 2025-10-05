package com.example.progfront.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progfront.R
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.model.ProfileResponse
import com.example.progfront.data.model.UpdateProfileRequest
import com.example.progfront.data.model.ScheduleResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.ui.auth.login.LoginActivity
import com.example.progfront.ui.schedule.AddHabitDialogFragment
import com.example.progfront.utils.TokenManager
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.progfront.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import android.os.Handler
import android.os.Looper

class ProfileFragment : Fragment(), AddHabitDialogFragment.OnHabitCreatedListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private var currentProfile: ProfileResponse? = null
    private val habitsAdapter = ProfileHabitsAdapter()

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    private val TAG = "ProfileFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        tokenManager = TokenManager(requireContext())
        setupRecycler()
        setupButtons()
        fetchProfile()
        return binding.root
    }

    private fun setupRecycler() {
        binding.recyclerHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHabits.adapter = habitsAdapter
    }

    private fun setupButtons() {
        binding.buttonEditProfile.setOnClickListener { openEditProfileDialog() }
        binding.buttonAddHabit.setOnClickListener { openAddHabitDialog() }
    }

    private fun fetchProfile() {
        val bearer = tokenManager.getBearerToken()
        if (bearer == null) {
            navigateToLogin(); return
        }
        showLoading(true)
        RetrofitClient.instance.getMyProfile(bearer)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    if (response.isSuccessful) {
                        val profile = response.body()
                        Log.d(TAG, "Profile raw: ${Gson().toJson(profile)}")
                        if (profile != null) {
                            currentProfile = profile
                            bindProfile(profile)
                            fetchHabits(profile.id)
                        } else showError(getString(R.string.profile_fetch_failed))
                    } else {
                        Log.e(TAG, "Profile fetch failed code=${response.code()} body=${response.errorBody()?.string()}")
                        showError(getString(R.string.profile_fetch_failed))
                    }
                }
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    Log.e(TAG, "Profile fetch network failure: ${t.message}", t)
                    showError(t.message ?: getString(R.string.profile_fetch_failed))
                }
            })
    }

    private fun fetchHabits(userId: Int) {
        val bearer = tokenManager.getBearerToken() ?: return
        RetrofitClient.instance.getHabitsByUser(bearer, userId)
            .enqueue(object : Callback<List<HabitResponse>> {
                override fun onResponse(call: Call<List<HabitResponse>>, response: Response<List<HabitResponse>>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val list = response.body().orEmpty()
                        habitsAdapter.submit(list)
                        binding.textHabitsEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        // Kick off monthly progress aggregation
                        if (list.isNotEmpty()) computeMonthlyHabitProgress(list)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.profile_habits_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<List<HabitResponse>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), getString(R.string.profile_habits_failed), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun bindProfile(profile: ProfileResponse) {
        binding.textUsername.text = profile.username ?: "(no username)"
        binding.textEmail.text = profile.email
        binding.textDescription.text = profile.description.orEmpty()
        binding.textDescription.visibility = if (profile.description.isNullOrBlank()) View.GONE else View.VISIBLE
        // Decide image source preference: URL first, fallback to base64
        val chosen = when {
            !profile.profileImageUrl.isNullOrBlank() -> profile.profileImageUrl
            !profile.profileImageBase64.isNullOrBlank() -> {
                val b64 = profile.profileImageBase64.trim()
                if (b64.startsWith("data:image")) b64 else "data:image/*;base64,$b64"
            }
            else -> null
        }
        Log.d(TAG, "bindProfile: imageUrl=${profile.profileImageUrl} base64Present=${!profile.profileImageBase64.isNullOrBlank()} chosen=${chosen?.take(30)}...")
        loadProfileImage(chosen)
    }

    private fun loadProfileImage(raw: String?) {
        if (raw.isNullOrBlank()) return
        // base64 variants
        if (raw.startsWith("data:image") || raw.contains(";base64,")) {
            val base64Part = raw.substringAfter(",", "")
            try {
                val bytes = android.util.Base64.decode(base64Part, android.util.Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    binding.imageProfile.setImageBitmap(bmp)
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode base64 profile image: ${e.message}")
            }
        }
        val url = normalizeImageUrl(raw)
        Log.d(TAG, "Loading profile image url=$url (raw=$raw)")
        Picasso.get()
            .load(url)
            .placeholder(R.mipmap.ic_launcher_round)
            .error(R.mipmap.ic_launcher_round)
            .into(binding.imageProfile)
    }

    private fun normalizeImageUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        // ensure no duplicate slashes
        val base = getBaseUrl().trimEnd('/')
        val path = trimmed.trimStart('/')
        return "$base/$path"
    }

    private fun getBaseUrl(): String = try {
        val field = RetrofitClient::class.java.getDeclaredField("BASE_URL")
        field.isAccessible = true
        (field.get(null) as? String) ?: "http://10.0.2.2:8080/"
    } catch (_: Exception) { "http://10.0.2.2:8080/" }

    private fun openAddHabitDialog() {
        val dialog = AddHabitDialogFragment()
        dialog.setOnHabitCreatedListener(this)
        dialog.show(childFragmentManager, "AddHabitDialog")
    }

    private fun openEditProfileDialog() {
        val profile = currentProfile ?: return
        val view = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val inputUsername = view.findViewById<EditText>(R.id.inputUsername)
        val inputDescription = view.findViewById<EditText>(R.id.inputDescription)
        val buttonChangePhoto = view.findViewById<View>(R.id.buttonChangePhotoDialog)
        inputUsername.setText(profile.username ?: "")
        inputDescription.setText(profile.description ?: "")
        buttonChangePhoto.setOnClickListener {
            imagePicker.launch("image/*")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.profile_edit))
            .setView(view)
            .setPositiveButton(getString(R.string.profile_save)) { d, _ ->
                d.dismiss()
                submitProfileUpdate(
                    inputUsername.text.toString().takeIf { it.isNotBlank() },
                    inputDescription.text.toString().takeIf { it.isNotBlank() }
                )
            }
            .setNegativeButton(getString(R.string.profile_cancel)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun submitProfileUpdate(username: String?, description: String?) {
        val bearer = tokenManager.getBearerToken() ?: return
        val body = UpdateProfileRequest(username = username, description = description)
        showLoading(true)
        RetrofitClient.instance.updateMyProfile(bearer, body)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val updated = response.body()
                        if (updated != null) {
                            currentProfile = updated
                            bindProfile(updated)
                            Toast.makeText(requireContext(), getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(requireContext(), getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.profile_logout_confirm_title))
            .setMessage(getString(R.string.profile_logout_confirm_message))
            .setPositiveButton(getString(R.string.profile_logout_yes)) { d, _ ->
                d.dismiss(); performLogout()
            }
            .setNegativeButton(getString(R.string.profile_logout_no)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun performLogout() {
        val bearer = tokenManager.getBearerToken()
        tokenManager.clearTokens() // clear locally regardless
        if (bearer == null) { navigateToLogin(); return }
        RetrofitClient.instance.logout(bearer)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    navigateToLogin()
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    navigateToLogin()
                }
            })
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.progressProfile.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(msg: String) {
        showLoading(false)
        binding.textErrorProfile.visibility = View.VISIBLE
        binding.textErrorProfile.text = msg
    }

    // === Monthly per-habit progress aggregation ===
    private fun computeMonthlyHabitProgress(habits: List<HabitResponse>) {
        val bearer = tokenManager.getBearerToken() ?: return
        if (!isAdded) return
        showLoading(true)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        // Include today and go back 15 days => 16 days total
        val days = mutableListOf<String>()
        for (i in 0..15) { // 0..15 inclusive
            days.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }

        val scheduleTotals = mutableMapOf<Int, Int>() // habitId -> total schedules
        val scheduleCompleted = mutableMapOf<Int, Int>() // habitId -> completed schedules
        val habitIds = habits.map { it.id }.toSet()

        val handler = Handler(Looper.getMainLooper())
        val delayMs = 50L

        fun finish() {
            // Build percent map
            val percents = mutableMapOf<Int, Int>()
            for (hid in habitIds) {
                val t = scheduleTotals[hid] ?: 0
                val c = scheduleCompleted[hid] ?: 0
                val percent = if (t > 0) (c * 100 / t) else 0
                percents[hid] = percent
            }
            if (isAdded) {
                habitsAdapter.updateProgress(percents)
                showLoading(false)
            }
        }

        fun processDay(index: Int) {
            if (!isAdded) { showLoading(false); return }
            if (index >= days.size) { finish(); return }
            val day = days[index]
            RetrofitClient.instance.getSchedulesForDay(bearer, day)
                .enqueue(object : Callback<List<ScheduleResponse>> {
                    override fun onResponse(call: Call<List<ScheduleResponse>>, response: Response<List<ScheduleResponse>>) {
                        if (response.isSuccessful) {
                            val schedules = response.body().orEmpty()
                            Log.d(TAG, "Day=$day schedules=${schedules.size}")
                            for (sch in schedules) {
                                val hid = sch.habitId
                                if (!habitIds.contains(hid)) continue
                                scheduleTotals[hid] = (scheduleTotals[hid] ?: 0) + 1
                                val completedForSchedule = when {
                                    !sch.progress.isNullOrEmpty() -> sch.progress.any { it.is_completed }
                                    else -> sch.status.equals("Completed", ignoreCase = true)
                                }
                                if (completedForSchedule) {
                                    scheduleCompleted[hid] = (scheduleCompleted[hid] ?: 0) + 1
                                }
                            }
                        } else {
                            Log.e(TAG, "getSchedulesForDay($day) failed code=${response.code()}")
                        }
                        handler.postDelayed({ processDay(index + 1) }, delayMs)
                    }
                    override fun onFailure(call: Call<List<ScheduleResponse>>, t: Throwable) {
                        Log.e(TAG, "getSchedulesForDay($day) failure: ${t.message}")
                        handler.postDelayed({ processDay(index + 1) }, delayMs)
                    }
                })
        }

        processDay(0)
    }

    private fun handleImageSelected(uri: Uri) {
        val bearer = tokenManager.getBearerToken() ?: return
        val file = copyUriToTempFile(uri) ?: run {
            Toast.makeText(requireContext(), getString(R.string.profile_image_upload_failed), Toast.LENGTH_SHORT).show(); return
        }
        val reqBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("profileImage", file.name, reqBody)
        showLoading(true)
        RetrofitClient.instance.uploadProfileImage(bearer, part)
            .enqueue(object : Callback<ProfileResponse> {
                override fun onResponse(call: Call<ProfileResponse>, response: Response<ProfileResponse>) {
                    showLoading(false)
                    if (response.isSuccessful) {
                        val updated = response.body()
                        Log.d(TAG, "Upload response: ${Gson().toJson(updated)}")
                        if (updated != null) {
                            currentProfile = updated
                            // Rebind handles base64 fallback automatically
                            bindProfile(updated)
                            Toast.makeText(requireContext(), getString(R.string.profile_image_upload_success), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Image upload failed code=${response.code()} body=${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), getString(R.string.profile_image_upload_failed), Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ProfileResponse>, t: Throwable) {
                    showLoading(false)
                    Log.e(TAG, "Image upload network failure: ${t.message}", t)
                    Toast.makeText(requireContext(), getString(R.string.profile_image_upload_failed), Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val fileName = "profile_${System.currentTimeMillis()}.img"
            val tempFile = File(requireContext().cacheDir, fileName)
            requireContext().contentResolver.openInputStream(uri)?.use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytes: Int
                    while (input.read(buffer).also { bytes = it } != -1) {
                        output.write(buffer, 0, bytes)
                    }
                    output.flush()
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    override fun onHabitCreated(habit: HabitResponse) {
        // Refresh habits list after new habit is created
        currentProfile?.let { fetchHabits(it.id) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
