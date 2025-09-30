package com.example.progfront

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.progfront.network.HabitCategoryResponse
import com.example.progfront.network.HabitRequest
import com.example.progfront.network.HabitResponse
import com.example.progfront.network.RetrofitClient
import com.example.progfront.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddHabitDialogFragment : DialogFragment() {

    interface OnHabitCreatedListener {
        fun onHabitCreated(habit: HabitResponse)
    }

    private var listener: OnHabitCreatedListener? = null
    private lateinit var categories: List<HabitCategoryResponse>
    private lateinit var tokenManager: TokenManager

    fun setOnHabitCreatedListener(listener: OnHabitCreatedListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_habit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tokenManager = TokenManager(requireContext())

        val editTextHabitName = view.findViewById<EditText>(R.id.editTextHabitName)
        val editTextHabitDescription = view.findViewById<EditText>(R.id.editTextHabitDescription)
        val editTextHabitGoal = view.findViewById<EditText>(R.id.editTextHabitGoal)
        val spinnerHabitCategory = view.findViewById<Spinner>(R.id.spinnerHabitCategory)
        val buttonCreateHabit = view.findViewById<Button>(R.id.buttonCreateHabit)
        val buttonCancelHabit = view.findViewById<Button>(R.id.buttonCancelHabit)

        // Load habit categories
        loadHabitCategories(spinnerHabitCategory)

        // Setup button click listeners
        buttonCreateHabit.setOnClickListener {
            createHabit()
        }

        buttonCancelHabit.setOnClickListener {
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Create New Habit")
        return dialog
    }

    private fun loadHabitCategories(spinner: Spinner) {
        val token = tokenManager.getBearerToken()

        if (token == null) {
            Toast.makeText(context, "Please log in again", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        RetrofitClient.instance.getHabitCategories(token)
            .enqueue(object : Callback<List<HabitCategoryResponse>> {
                override fun onResponse(call: Call<List<HabitCategoryResponse>>, response: Response<List<HabitCategoryResponse>>) {
                    Log.d("AddHabitDialog", "Categories response: ${response.body()}")
                    if (response.isSuccessful) {
                        response.body()?.let { categoryList ->
                            categories = categoryList
                            val adapter = CategorySpinnerAdapter(requireContext(), categoryList)
                            spinner.adapter = adapter
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
        val view = requireView()
        val editTextHabitName = view.findViewById<EditText>(R.id.editTextHabitName)
        val editTextHabitDescription = view.findViewById<EditText>(R.id.editTextHabitDescription)
        val editTextHabitGoal = view.findViewById<EditText>(R.id.editTextHabitGoal)
        val spinnerHabitCategory = view.findViewById<Spinner>(R.id.spinnerHabitCategory)

        val name = editTextHabitName.text.toString().trim()
        val description = editTextHabitDescription.text.toString().trim()
        val goal = editTextHabitGoal.text.toString().trim()
        val selectedCategoryIndex = spinnerHabitCategory.selectedItemPosition

        if (name.isEmpty() || goal.isEmpty() || selectedCategoryIndex < 0) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryId = categories[selectedCategoryIndex].id
        val habitRequest = HabitRequest(
            name = name,
            categoryId = categoryId,
            goal = goal,
            description = description.ifEmpty { null }
        )

        Log.d("AddHabitDialog", "Creating habit: $habitRequest")

        val token = tokenManager.getBearerToken()

        if (token == null) {
            Toast.makeText(context, "Please log in again", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        RetrofitClient.instance.createHabit(token, habitRequest)
            .enqueue(object : Callback<HabitResponse> {
                override fun onResponse(call: Call<HabitResponse>, response: Response<HabitResponse>) {
                    Log.d("AddHabitDialog", "Create habit response: ${response.body()}")
                    if (response.isSuccessful) {
                        response.body()?.let { habit ->
                            listener?.onHabitCreated(habit)
                            dismiss()
                        }
                    } else {
                        Log.e("AddHabitDialog", "Failed to create habit: ${response.errorBody()?.string()}")
                        Toast.makeText(context, "Failed to create habit", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<HabitResponse>, t: Throwable) {
                    Log.e("AddHabitDialog", "Network error creating habit: ${t.message}", t)
                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
