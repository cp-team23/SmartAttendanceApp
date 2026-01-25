package com.smartattendance.student.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartattendance.student.R
import com.smartattendance.student.models.Attendance

class AttendanceAdapter(
    private var list: MutableList<Attendance>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    fun updateData(newList: List<Attendance>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val item = list[position]

        holder.tvSubject.text = item.subject
        holder.tvDate.text = item.date
        holder.tvStatus.text = item.status

        when (item.status) {
            "Present" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_present)
            "Absent" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_absent)
        }
    }

    override fun getItemCount(): Int = list.size

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }
}