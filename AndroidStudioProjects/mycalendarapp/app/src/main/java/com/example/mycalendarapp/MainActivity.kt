
package com.example.mycalendarapp
//MainActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.google.gson.Gson
import org.threeten.bp.LocalDate
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var addNoteButton: Button
    private lateinit var notesRecyclerView: RecyclerView
    private val notes = mutableListOf<Note>()
    private val notesAdapter = NoteAdapter(notes, ::deleteNote) // Pass the deleteNote function to the adapter
    private val noteDays = mutableMapOf<CalendarDay, ArrayList<Note>>()
    private val calendarDayDecorators = mutableSetOf<CalendarDay>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarView = findViewById(R.id.calendarView)
        addNoteButton = findViewById(R.id.addNoteButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)

        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = notesAdapter

        // Load notes from Firebase Storage when app starts
        loadNotesFromFirebaseStorage()

        calendarView.setOnDateChangedListener(OnDateSelectedListener { widget, date, selected ->
            loadNotesForDate(date)
        })

        addNoteButton.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    private fun loadNotesFromFirebaseStorage() {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes")
        storageRef.listAll().addOnSuccessListener { listResult ->
            val notes = mutableListOf<Note>()
            listResult.items.forEach { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val noteJson = String(data)
                    val note = convertJsonToNote(noteJson)
                    val noteDay = note.date

                    if (!noteDays.containsKey(noteDay)) {
                        noteDays[noteDay] = ArrayList()
                    }
                    noteDays[noteDay]?.add(note)
                    notes.add(note)
                    calendarDayDecorators.add(noteDay)
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Error fetching note", e)
                }
            }
            // Ensure all notes are processed before updating the calendar
            notesAdapter.notifyDataSetChanged()
            updateCalendarWithNotes()
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error listing notes", e)
        }
    }

    private fun convertJsonToNote(json: String): Note {
        val gson = Gson()
        return gson.fromJson(json, Note::class.java)
    }

    private fun loadNotesForDate(date: CalendarDay) {
        notes.clear()
        notes.addAll(noteDays[date] ?: emptyList())
        notesAdapter.notifyDataSetChanged()
    }

    private fun updateCalendarWithNotes() {
        calendarView.removeDecorators()  // Clear existing decorators
        calendarView.addDecorator(DotDecorator(calendarDayDecorators))  // Add updated decorators
        Log.d("MainActivity", "Updated calendar with notes: $calendarDayDecorators")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            data?.let {
                val dateString = it.getStringExtra("date") ?: return@let
                val title = it.getStringExtra("title") ?: ""
                val description = it.getStringExtra("description") ?: ""
                val color = it.getIntExtra("color", R.color.black)
                val noteId = it.getStringExtra("noteId") ?: UUID.randomUUID().toString() // Handle note ID

                val date = LocalDate.parse(dateString)
                val note = Note(
                    id = noteId,
                    date = CalendarDay.from(date),
                    title = title,
                    description = description,
                    color = color
                )
                addNoteToCalendar(note)
            }
        }
    }

    private fun addNoteToCalendar(note: Note) {
        val date = note.date
        if (!noteDays.containsKey(date)) {
            noteDays[date] = ArrayList()
        }
        noteDays[date]?.add(note)
        calendarDayDecorators.add(date)
        updateCalendarWithNotes()
        loadNotesForDate(date)
    }

    private fun deleteNote(note: Note) {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes/${note.id}.json") // Updated path with .json

        storageRef.delete().addOnSuccessListener {
            // Successfully deleted note from Firebase Storage
            notes.remove(note)  // Remove note from the local list
            notesAdapter.notifyDataSetChanged()  // Notify adapter to refresh the list

            // Update calendar view
            noteDays[note.date]?.remove(note)
            if (noteDays[note.date]?.isEmpty() == true) {
                noteDays.remove(note.date)
                calendarDayDecorators.remove(note.date)
            }
            updateCalendarWithNotes()  // Refresh the calendar

            Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error deleting note", e)
            Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show()
        }
    }

}
