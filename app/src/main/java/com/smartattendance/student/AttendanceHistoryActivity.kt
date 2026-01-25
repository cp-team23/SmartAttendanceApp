package com.smartattendance.student

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.smartattendance.student.adapters.AttendanceAdapter
import com.smartattendance.student.models.Attendance
import kotlin.math.roundToInt

class AttendanceHistoryActivity : AppCompatActivity() {

    private lateinit var adapter: AttendanceAdapter
    private lateinit var fullList: List<Attendance>

    private lateinit var recyclerAttendance: RecyclerView
    private lateinit var layoutNoData: View

    // Summary views
    private lateinit var progressAttendance: CircularProgressIndicator
    private lateinit var tvPercentage: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvPresent: TextView
    private lateinit var tvAbsent: TextView

    // Filter state
    private var selectedMonth = "All Months"
    private var selectedSubject = "All Subjects"
    private var selectedStatus = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_history)

        // Views
        recyclerAttendance = findViewById(R.id.recyclerAttendance)
        layoutNoData = findViewById(R.id.layoutNoData)

        progressAttendance = findViewById(R.id.progressAttendance)
        tvPercentage = findViewById(R.id.tvPercentage)
        tvTotal = findViewById(R.id.tvTotal)
        tvPresent = findViewById(R.id.tvPresent)
        tvAbsent = findViewById(R.id.tvAbsent)

        recyclerAttendance.layoutManager = LinearLayoutManager(this)

        // Dummy data (backend later)
        fullList = listOf(
            Attendance("DS", "12 Sep 2025", "Present", "September"),
            Attendance("OS", "13 Sep 2025", "Absent", "September"),
            Attendance("DBMS", "01 Oct 2025", "Present", "October"),
            Attendance("CN", "02 Oct 2025", "Absent", "October")
        )

        adapter = AttendanceAdapter(fullList.toMutableList())
        recyclerAttendance.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabFilter).setOnClickListener {
            showFilterBottomSheet()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Bottom nav (no tab selected)
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

        applyFilters()
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(
            R.layout.bottomsheet_attendance_filter,
            null
        )
        dialog.setContentView(view)

        val spinnerMonth = view.findViewById<Spinner>(R.id.spinnerMonth)
        val spinnerSubject = view.findViewById<Spinner>(R.id.spinnerSubject)
        val chipGroupStatus = view.findViewById<ChipGroup>(R.id.chipGroupStatus)

        val monthAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All Months", "September", "October")
        )
        spinnerMonth.adapter = monthAdapter
        spinnerMonth.setSelection(monthAdapter.getPosition(selectedMonth))

        val subjectAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("All Subjects", "DS", "OS", "DBMS", "CN")
        )
        spinnerSubject.adapter = subjectAdapter
        spinnerSubject.setSelection(subjectAdapter.getPosition(selectedSubject))

        when (selectedStatus) {
            "Present" -> chipGroupStatus.check(R.id.chipPresent)
            "Absent" -> chipGroupStatus.check(R.id.chipAbsent)
            else -> chipGroupStatus.check(R.id.chipAll)
        }

        view.findViewById<View>(R.id.btnApplyFilter).setOnClickListener {

            selectedMonth = spinnerMonth.selectedItem.toString()
            selectedSubject = spinnerSubject.selectedItem.toString()

            selectedStatus = when (chipGroupStatus.checkedChipId) {
                R.id.chipPresent -> "Present"
                R.id.chipAbsent -> "Absent"
                else -> "All"
            }

            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyFilters() {
        val filtered = fullList.filter {
            (selectedMonth == "All Months" || it.month == selectedMonth) &&
                    (selectedSubject == "All Subjects" || it.subject == selectedSubject) &&
                    (selectedStatus == "All" || it.status == selectedStatus)
        }

        adapter.updateData(filtered)

        // No data handling
        if (filtered.isEmpty()) {
            recyclerAttendance.visibility = View.GONE
            layoutNoData.visibility = View.VISIBLE
        } else {
            recyclerAttendance.visibility = View.VISIBLE
            layoutNoData.visibility = View.GONE
        }

        updateSummary(filtered)
    }
    private fun updateSummary(list: List<Attendance>) {
        val total = list.size
        val present = list.count { it.status == "Present" }
        val absent = list.count { it.status == "Absent" }

        val percentage =
            if (total == 0) 0
            else ((present.toFloat() / total) * 100).roundToInt()

        tvTotal.text = "Total\n$total"
        tvPresent.text = "Present\n$present"
        tvAbsent.text = "Absent\n$absent"
        tvPercentage.text = "$percentage%"

        // 🎨 Dynamic color
        val color = when {
            percentage >= 75 -> "#4CAF50"   // Green
            percentage >= 50 -> "#FFC107"   // Yellow
            else -> "#F44336"               // Red
        }

        val parsedColor = Color.parseColor(color)

        progressAttendance.setIndicatorColor(parsedColor)
        tvPercentage.setTextColor(parsedColor)

        progressAttendance.setProgressCompat(percentage, true)
    }

}
