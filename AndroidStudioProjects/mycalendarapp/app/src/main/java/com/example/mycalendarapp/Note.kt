package com.example.mycalendarapp
//Note

import com.prolificinteractive.materialcalendarview.CalendarDay

data class Note(
    val id: String,
    val date: CalendarDay,
    val title: String,
    val description: String,
    val color: Int,
    val company: String
)
