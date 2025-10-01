package com.example.progfront.ui.schedule

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.progfront.R
import com.example.progfront.data.model.HabitCategoryResponse
import com.squareup.picasso.Picasso

class CategorySpinnerAdapter(
    private val context: Context,
    private val categories: List<HabitCategoryResponse>
) : BaseAdapter() {

    override fun getCount(): Int = categories.size

    override fun getItem(position: Int): Any = categories[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_item_category, parent, false)

        val category = categories[position]
        val imageView = view.findViewById<ImageView>(R.id.imageViewCategoryIcon)
        val textView = view.findViewById<TextView>(R.id.textViewCategoryName)

        textView.text = category.name

        // Load category icon using Picasso or set a default icon
        if (!category.iconUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(category.iconUrl)
                .placeholder(R.drawable.ic_default_category)
                .error(R.drawable.ic_default_category)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_default_category)
        }

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return getView(position, convertView, parent)
    }
}

