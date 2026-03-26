package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.smartattendance.student.adapters.AttendanceAdapter
import com.smartattendance.student.models.Attendance
import com.smartattendance.student.models.AttendanceResponse
import com.smartattendance.student.network.RetrofitClient
import kotlin.math.roundToInt

class AttendanceHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: AttendanceAdapter
    private var fullList: List<Attendance> = emptyList()

    private lateinit var recyclerAttendance: RecyclerView
    private lateinit var layoutNoData: View
    private lateinit var loadingAttendance: View
    private lateinit var progressAttendance: CircularProgressIndicator
    private lateinit var tvPercentage: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvPresent: TextView
    private lateinit var tvAbsent: TextView

    private var selectedMonth   = "All Months"
    private var selectedSubject = "All Subjects"
    private var selectedStatus  = "All"
    private var subjects: List<String> = listOf("All Subjects")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_history)

        recyclerAttendance = findViewById(R.id.recyclerAttendance)
        layoutNoData       = findViewById(R.id.layoutNoData)
        progressAttendance = findViewById(R.id.progressAttendance)
        tvPercentage       = findViewById(R.id.tvPercentage)
        tvTotal            = findViewById(R.id.tvTotal)
        tvPresent          = findViewById(R.id.tvPresent)
        tvAbsent           = findViewById(R.id.tvAbsent)
        loadingAttendance  = findViewById(R.id.loadingAttendance)

        recyclerAttendance.layoutManager = LinearLayoutManager(this)
        recyclerAttendance.itemAnimator  = DefaultItemAnimator()

        adapter = AttendanceAdapter(mutableListOf())
        recyclerAttendance.adapter = adapter

        loadAttendance()

        findViewById<FloatingActionButton>(R.id.fabFilter).setOnClickListener {
            showFilterBottomSheet()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.menu.setGroupCheckable(0, false, true)

        bottomNav.setOnItemSelectedListener { item ->
            val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java), options.toBundle())
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java), options.toBundle())
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // FIX: Wrapped in try-catch to prevent crash on unexpected date format from server
    private fun formatDate(date: String): String {
        return try {
            val parts    = date.split("-")
            val year     = parts[0]
            val month    = parts[1].toInt()
            val day      = parts[2]
            val monthName = when (month) {
                1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
                5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
                9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; else -> "Dec"
            }
            "$day $monthName $year"
        } catch (e: Exception) {
            date  // fallback: return raw string rather than crashing
        }
    }

    // FIX: Wrapped in try-catch to prevent crash on unexpected time format from server
    private fun formatTime(time: String): String {
        return try {
            val parts  = time.split(":")
            var hour   = parts[0].toInt()
            val minute = parts[1]
            val ampm   = if (hour >= 12) "PM" else "AM"
            if (hour > 12) hour -= 12
            if (hour == 0) hour = 12
            String.format("%02d:%s %s", hour, minute, ampm)
        } catch (e: Exception) {
            time  // fallback: return raw string rather than crashing
        }
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view   = layoutInflater.inflate(R.layout.bottomsheet_attendance_filter, null)
        dialog.setContentView(view)

        val spinnerMonth   = view.findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerSubject = view.findViewById<Spinner>(R.id.spinnerSubject)
        val chipGroupStatus = view.findViewById<ChipGroup>(R.id.chipGroupStatus)

        spinnerMonth.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("All Months","January","February","March","April","May","June",
                "July","August","September","October","November","December")
        )
        spinnerSubject.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, subjects
        )

        view.findViewById<View>(R.id.btnApplyFilter).setOnClickListener {
            selectedMonth   = spinnerMonth.selectedItem.toString()
            selectedSubject = spinnerSubject.selectedItem.toString()
            selectedStatus  = when (chipGroupStatus.checkedChipId) {
                R.id.chipPresent -> "Present"
                R.id.chipAbsent  -> "Absent"
                else             -> "All"
            }
            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadAttendance() {
        loadingAttendance.visibility  = View.VISIBLE
        recyclerAttendance.visibility = View.GONE

        RetrofitClient.create(this).getAllAttendance()
            .enqueue(object : Callback<AttendanceResponse> {

                override fun onResponse(call: Call<AttendanceResponse>, response: Response<AttendanceResponse>) {
                    loadingAttendance.visibility  = View.GONE
                    recyclerAttendance.visibility = View.VISIBLE

                    if (!response.isSuccessful || response.body() == null) return

                    val backendList = response.body()!!.response.sortedByDescending { it.attendanceDate }

                    subjects = listOf("All Subjects") + backendList.map { it.subjectName }.distinct()

                    fullList = backendList.map {
                        val month = try { it.attendanceDate.substring(5, 7).toInt() } catch (e: Exception) { 1 }
                        val monthName = when (month) {
                            1 -> "January"; 2 -> "February"; 3 -> "March"; 4 -> "April"
                            5 -> "May"; 6 -> "June"; 7 -> "July"; 8 -> "August"
                            9 -> "September"; 10 -> "October"; 11 -> "November"; else -> "December"
                        }
                        Attendance(
                            subject = it.subjectName,
                            date    = formatDate(it.attendanceDate) + " • " + formatTime(it.attendanceTime),
                            status  = if (it.status == "PRESENT") "Present" else "Absent",
                            month   = monthName,
                            teacher = it.teacherName
                        )
                    }

                    applyFilters()
                }

                override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                    loadingAttendance.visibility = View.GONE
                    Toast.makeText(this@AttendanceHistoryActivity, "Failed to load attendance", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun applyFilters() {
        val filtered = fullList.filter {
            (selectedMonth   == "All Months"   || it.month   == selectedMonth) &&
                    (selectedSubject == "All Subjects" || it.subject == selectedSubject) &&
                    (selectedStatus  == "All"          || it.status  == selectedStatus)
        }

        adapter.updateData(filtered)

        if (filtered.isEmpty()) {
            recyclerAttendance.visibility = View.GONE
            layoutNoData.visibility       = View.VISIBLE
        } else {
            recyclerAttendance.visibility = View.VISIBLE
            layoutNoData.visibility       = View.GONE
        }

        updateSummary(filtered)
    }

    private fun updateSummary(list: List<Attendance>) {
        val total   = list.size
        val present = list.count { it.status == "Present" }
        val absent  = list.count { it.status == "Absent" }
        val percentage = if (total == 0) 0 else ((present.toFloat() / total) * 100).roundToInt()

        tvTotal.text   = "Total\n$total"
        tvPresent.text = "Present\n$present"
        tvAbsent.text  = "Absent\n$absent"
        tvPercentage.text = "$percentage%"

        val color = when {
            percentage >= 75 -> "#4CAF50"
            percentage >= 50 -> "#FFC107"
            else             -> "#F44336"
        }
        val parsedColor = Color.parseColor(color)
        progressAttendance.setIndicatorColor(parsedColor)
        tvPercentage.setTextColor(parsedColor)

        progressAttendance.setProgressCompat(0, false)
        progressAttendance.postDelayed({
            progressAttendance.setProgressCompat(percentage, true)
        }, 200)
    }
}