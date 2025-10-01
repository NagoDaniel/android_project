package com.example.progfront.ui.schedule

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.progfront.data.model.HabitCategoryResponse
import com.example.progfront.data.model.HabitRequest
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.data.remote.RetrofitClient
import com.example.progfront.databinding.DialogAddHabitBinding
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddHabitDialogFragment : DialogFragment() {

    interface OnHabitCreatedListener {
        fun onHabitCreated(habit: HabitResponse)
    }

    private var listener: OnHabitCreatedListener? = null
    private var categories: List<HabitCategoryResponse> = emptyList()
    private lateinit var tokenManager: TokenManager
    private var _binding: DialogAddHabitBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnHabitCreatedListener
        if (listener == null) {
            throw RuntimeException("$context must implement OnHabitCreatedListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        loadHabitCategories()

        binding.buttonCreateHabit.setOnClickListener {
            createHabit()
        }

        binding.buttonCancelHabit.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Create New Habit")
        return dialog
    }

    private fun loadHabitCategories() {
        val token = tokenManager.getBearerToken()

        if (token == null) {
            Toast.makeText(context, "Please log in again", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        RetrofitClient.instance.getHabitCategories(token)
            .enqueue(object : Callback<List<HabitCategoryResponse>> {
                override fun onResponse(call: Call<List<HabitCategoryResponse>>, response: Response<List<HabitCategoryResponse>>) {
                    if (response.isSuccessful) {
                        response.body()?.let { categoryList ->
                            categories = categoryList
                            val adapter = CategorySpinnerAdapter(requireContext(), categoryList)
                            binding.spinnerHabitCategory.adapter = adapter
                        }
                    } else {
                        Log.e("AddHabitDialog", "Failed to load categories: ${response.errorBody()?.string()}")
                        Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<HabitCategoryResponse>>, t: Throwable) {
                    Log.e("AddHabitDialog", "Network error loading categories: ${t.message}", t)
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createHabit() {
        val token = tokenManager.getBearerToken()
        if (token == null) {
            Toast.makeText(context, "Please log in again", Toast.LENGTH_SHORT).show()
            return
        }

        val name = binding.editTextHabitName.text.toString()
        val description = binding.editTextHabitDescription.text.toString()
        val goal = binding.editTextHabitGoal.text.toString()

        if (name.isEmpty() || goal.isEmpty()) {
            Toast.makeText(context, "Name and goal are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (binding.spinnerHabitCategory.selectedItem == null) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategory = binding.spinnerHabitCategory.selectedItem as HabitCategoryResponse

        val habitRequest = HabitRequest(
            name = name,
            description = description,
            goal = goal,
            categoryId = selectedCategory.id
        )

        RetrofitClient.instance.createHabit(token, habitRequest)
            .enqueue(object : Callback<HabitResponse> {
                override fun onResponse(call: Call<HabitResponse>, response: Response<HabitResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            listener?.onHabitCreated(it)
                            dismiss()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create habit", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<HabitResponse>, t: Throwable) {
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
