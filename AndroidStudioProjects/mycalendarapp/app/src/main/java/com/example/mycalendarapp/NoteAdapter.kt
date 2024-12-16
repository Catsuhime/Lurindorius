package com.example.mycalendarapp
//NoteAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.prolificinteractive.materialcalendarview.CalendarDay

class NoteAdapter(
    val notes: MutableList<Note>,
    private val onDelete: (Note) -> Unit,
    private val onEdit: (Note) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val companyTextView: TextView = itemView.findViewById(R.id.companyTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView) // New TextView for date
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val editButton: Button = itemView.findViewById(R.id.editButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.titleTextView.text = note.title
        holder.descriptionTextView.text = note.description
        holder.companyTextView.text = note.company

        val formattedDate = formatCalendarDay(note.date)
        holder.dateTextView.text = formattedDate

        holder.deleteButton.setOnClickListener {
            onDelete(note)
        }
        holder.editButton.setOnClickListener {
            onEdit(note)
        }
    }

    override fun getItemCount() = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes.clear()
        notes.addAll(newNotes)
        notifyDataSetChanged()
    }

    private fun formatCalendarDay(calendarDay: CalendarDay): String {
        return "${calendarDay.year}-${calendarDay.month}-${calendarDay.day}"
    }
}
