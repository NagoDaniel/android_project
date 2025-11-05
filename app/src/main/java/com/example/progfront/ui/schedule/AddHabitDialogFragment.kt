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
import androidx.fragment.app.viewModels
import com.example.progfront.data.Result
import com.example.progfront.data.model.HabitCategoryResponse
import com.example.progfront.data.model.HabitRequest
import com.example.progfront.data.model.HabitResponse
import com.example.progfront.databinding.DialogAddHabitBinding
import com.example.progfront.utils.TokenManager

class AddHabitDialogFragment : DialogFragment() {

    interface OnHabitCreatedListener {
        fun onHabitCreated(habit: HabitResponse)
    }

    private var listener: OnHabitCreatedListener? = null
    private var categories: List<HabitCategoryResponse> = emptyList()
    private lateinit var tokenManager: TokenManager
    private var _binding: DialogAddHabitBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScheduleViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? OnHabitCreatedListener
        if (listener == null) {
            listener = context as? OnHabitCreatedListener
        }
    }

    fun setOnHabitCreatedListener(l: OnHabitCreatedListener) {
        listener = l
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        setupObservers()
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

    private fun setupObservers() {
        viewModel.habitCategories.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Could show loading indicator
                }
                is Result.Success -> {
                    categories = result.data
                    val adapter = CategorySpinnerAdapter(requireContext(), result.data)
                    binding.spinnerHabitCategory.adapter = adapter
                }
                is Result.Error -> {
                    Log.e("AddHabitDialog", "Failed to load categories: ${result.message}")
                    Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.createHabitResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.buttonCreateHabit.isEnabled = false
                }
                is Result.Success -> {
                    binding.buttonCreateHabit.isEnabled = true
                    listener?.onHabitCreated(result.data)
                    dismiss()
                }
                is Result.Error -> {
                    binding.buttonCreateHabit.isEnabled = true
                    Toast.makeText(context, "Failed to create habit: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadHabitCategories() {
        val token = tokenManager.getBearerToken()

        if (token == null) {
            Toast.makeText(context, "Please log in again", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        viewModel.loadHabitCategories()
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

        viewModel.createHabit(habitRequest)
    }
}
