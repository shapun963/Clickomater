package com.shapun.clickomater.ui.fragment

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.shapun.clickomater.databinding.FragmentEditTaskDelayDialogListDialogBinding
import com.shapun.clickomater.databinding.FragmentEditTaskDelayDialogListDialogItemBinding
import com.shapun.clickomater.model.Task
import com.shapun.clickomater.ui.activity.SaveTaskActivity
import com.shapun.clickomater.util.Utils
import org.json.JSONArray
import java.lang.Long.min
import kotlin.math.max

// TODO: Customize parameter argument names
const val ARG_DATA = "item_count"

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    EditTaskDelayDialogFragment.newInstance(30).show(supportFragmentManager, "dialog")
 * </pre>
 */
class EditTaskDelayDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditTaskDelayDialogListDialogBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTaskDelayDialogListDialogBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.list.layoutManager =
            LinearLayoutManager(context)
        val data = arguments?.getString(ARG_DATA,null)?.let { JSONArray(it) }!!
        binding.list.adapter = data.let { ItemAdapter(it) }
        binding.btnTrim.setOnClickListener {
            val reduceDelay = binding.edittextDuration.text.toString().toLong()
            if(reduceDelay > 0){
                for(i in 0 until  data.length()){
                    val delay = data.getJSONObject(i).getLong("delay")
                    data.getJSONObject(i).put("delay", max(delay-reduceDelay,min(delay,100L)))
                    binding.list.adapter?.notifyItemChanged(i)
                }
            }
            activity?.let { act->
                if(act is SaveTaskActivity){
                    act.updateData(data)
                }else {
                    val frg = parentFragment
                    if(frg is TaskInfoDialog){
                        frg.updateData(data)
                    }
                }
            }
        }
    }

    private inner class ViewHolder(binding: FragmentEditTaskDelayDialogListDialogItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val tvAction: TextView = binding.tvAction
        val tvDuration: TextView = binding.tvDuration
    }

    private inner class ItemAdapter(private val data: JSONArray) :
        RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            return ViewHolder(
                FragmentEditTaskDelayDialogListDialogItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val obj = data.getJSONObject(position)
            holder.tvAction.text = Task.Action.values().first { it.value == obj.getInt("action") }.name
            holder.tvDuration.text = obj.getLong("delay").toString()
            holder.itemView.setOnClickListener {
                val ctx = it.context
                val edittext = EditText(ctx)
                edittext.hint = "Duration in milliseconds"
                edittext.setText(obj.getLong("delay").toString())
                edittext.inputType = InputType.TYPE_CLASS_NUMBER
                //edittext.in
                edittext.setPadding(Utils.dpToPx(holder.itemView.context,8))
                MaterialAlertDialogBuilder(holder.itemView.context)
                    .setTitle("Set delay duration")
                    .setView(FrameLayout(ctx).apply {
                        setPadding(Utils.dpToPx(ctx,8))
                        addView(edittext)
                    })
                    .setPositiveButton("Save"){_,_->
                        obj.put("delay",edittext.text.toString().toLong())
                        holder.tvDuration.text = obj.getLong("delay").toString()
                        activity?.let { act->
                            if(act is SaveTaskActivity){
                                act.updateData(data)
                            }else {
                                val frg = parentFragment
                                if(frg is TaskInfoDialog){
                                    frg.updateData(data)
                                }
                            }
                        }
                    }
                    .setNegativeButton("Cancel",null)
                    .show()
            }
        }
        override fun getItemCount(): Int {
            return data.length()
        }
    }

    companion object {
        // TODO: Customize parameters
        fun newInstance(data: String): EditTaskDelayDialogFragment =
            EditTaskDelayDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATA, data)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}