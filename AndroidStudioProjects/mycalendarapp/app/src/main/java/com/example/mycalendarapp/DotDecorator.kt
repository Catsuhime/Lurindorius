package com.example.mycalendarapp
//DotDecorator

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.spans.DotSpan

class DotDecorator(private val dates: Set<CalendarDay>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay?): Boolean {
        // Only decorate the dates that are in the set
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade?) {
        // Add a dot to the day (use any color you prefer)
        view?.addSpan(DotSpan(8f, Color.RED))  // Red dot below the date
    }
}



