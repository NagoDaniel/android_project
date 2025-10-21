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
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.progfront.R
import com.example.progfront.data.Result
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.model.ProfileResponse
import com.example.progfront.data.model.UpdateProfileRequest
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
import com.example.progfront.databinding.FragmentProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProfileFragment : Fragment(), AddHabitDialogFragment.OnHabitCreatedListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var tokenManager: TokenManager
    private var currentProfile: ProfileResponse? = null
    private val habitsAdapter = ProfileHabitsAdapter()

    private val viewModel: ProfileViewModel by viewModels()

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
        setupObservers()
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

    private fun setupObservers() {
        viewModel.profile.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    Log.d(TAG, "Profile raw: ${Gson().toJson(result.data)}")
                    currentProfile = result.data
                    bindProfile(result.data)
                    viewModel.loadHabitsByUser(result.data.id)
                }
                is Result.Error -> {
                    Log.e(TAG, "Profile fetch failed: ${result.message}")
                    showError(result.message)
                }
            }
        }

        viewModel.habits.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Already showing loading
                }
                is Result.Success -> {
                    showLoading(false)
                    habitsAdapter.submit(result.data)
                    binding.textHabitsEmpty.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                    if (result.data.isNotEmpty()) {

                        viewModel.loadAllSchedules()
                    }
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), getString(R.string.profile_habits_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Delegate aggregation to ViewModel: when we receive schedules, tell VM to compute per-habit percents
        viewModel.allSchedules.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val habits = (viewModel.habits.value as? Result.Success)?.data ?: return@observe
                    viewModel.computeProgress(habits, result.data)
                    showLoading(false)
                }
                is Result.Error -> {
                    Log.e(TAG, "getAllSchedules failed: ${result.message}")
                    showLoading(false)
                }
                is Result.Loading -> {
                    // Already showing loading
                }
            }
        }

        // Observe computed percents and push them to the adapter
        viewModel.habitPercents.observe(viewLifecycleOwner) { percents ->
            habitsAdapter.updateProgress(percents)
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    showLoading(true)
                }
                is Result.Success -> {
                    showLoading(false)
                    currentProfile = result.data
                    bindProfile(result.data)
                    Toast.makeText(requireContext(), getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Update failed: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchProfile() {
        val token = tokenManager.getAccessToken()
        if (token.isNullOrBlank()) {
            navigateToLogin()
            return
        }
        viewModel.loadProfile()
    }

    private fun bindProfile(profile: ProfileResponse) {
        binding.textUsername.text = profile.username ?: "(no username)"
        binding.textEmail.text = profile.email
        binding.textDescription.text = profile.description.orEmpty()
        binding.textDescription.visibility = if (profile.description.isNullOrBlank()) View.GONE else View.VISIBLE
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
        val body = UpdateProfileRequest(username = username, description = description)
        viewModel.updateProfile(body)
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

    private fun handleImageSelected(uri: Uri) {
        val file = copyUriToTempFile(uri) ?: run {
            Toast.makeText(requireContext(), getString(R.string.profile_image_upload_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val reqBody = file.asRequestBody("image/*".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("profileImage", file.name, reqBody)
        viewModel.uploadProfileImage(part)
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
            Log.e(TAG, "copyUriToTempFile failed: ${e.message}")
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onHabitCreated(habit: HabitResponse) {
        // After a new habit is created, refresh the habits list (and progress)
        currentProfile?.id?.let { viewModel.loadHabitsByUser(it) }
    }
}
