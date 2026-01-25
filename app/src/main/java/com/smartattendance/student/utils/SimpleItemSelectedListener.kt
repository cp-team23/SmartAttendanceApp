package com.smartattendance.student.utils

import android.view.View
import android.widget.AdapterView

class SimpleItemSelectedListener(
    private val callback: () -> Unit
) : AdapterView.OnItemSelectedListener {

    override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
    ) {
        callback()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
