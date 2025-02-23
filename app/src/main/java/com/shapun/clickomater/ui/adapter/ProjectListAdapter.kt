package com.shapun.clickomater.ui.adapter

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shapun.clickomater.databinding.RowTaskBinding
import com.shapun.clickomater.model.TaskData

class ProjectListAdapter(val data: List<TaskData>): RecyclerView.Adapter<ProjectListAdapter.ViewHolder>() {

     inner class ViewHolder(val binding: RowTaskBinding) : RecyclerView.ViewHolder(binding.root)
    var mItemClickListener: ((Int) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowTaskBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvName.text = data[position].name
        holder.binding.imageView.setImageBitmap(BitmapFactory.decodeFile(data[position].imagePath))
        holder.binding.tvCreatedTime.text = "Created "+DateUtils.getRelativeTimeSpanString(data[position].createdTime,System.currentTimeMillis(),DateUtils.SECOND_IN_MILLIS).toString()
        holder.binding.root.setOnClickListener { _->
           mItemClickListener?.let { it(position) }
        }
    }

    fun setItemClickListener(itemClickListener: ((Int) -> Unit)?){
        mItemClickListener = itemClickListener
    }

    override fun getItemCount(): Int = data.size
}