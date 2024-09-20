
package com.example.mycalendarapp
//MainActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
    private val notesAdapter = NoteAdapter(notes, ::deleteNote, ::editNote)
    private val noteDays = mutableMapOf<CalendarDay, ArrayList<Note>>()
    private val calendarDayDecorators = mutableSetOf<CalendarDay>()

    private lateinit var addNoteLauncher: ActivityResultLauncher<Intent>
    private lateinit var editNoteLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendarView = findViewById(R.id.calendarView)
        addNoteButton = findViewById(R.id.addNoteButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)

        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = notesAdapter

        loadNotesFromFirebaseStorage()

        calendarView.setOnDateChangedListener(OnDateSelectedListener { _, date, _ ->
            loadNotesForDate(date)
        })

        addNoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.let { handleNoteResult(it) }
            }
        }

        editNoteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.let { handleNoteResult(it) }
            }
        }

        addNoteButton.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            addNoteLauncher.launch(intent)
        }

        updateCalendarWithNotes()
    }

    private fun handleNoteResult(data: Intent) {
        val noteId = data.getStringExtra("noteId") ?: UUID.randomUUID().toString()
        val dateString = data.getStringExtra("date") ?: return
        val title = data.getStringExtra("title") ?: return
        val description = data.getStringExtra("description") ?: return
        val color = data.getIntExtra("color", R.color.black)

        val date = LocalDate.parse(dateString)
        val calendarDay = CalendarDay.from(date)
        val note = Note(
            id = noteId,
            date = calendarDay,
            title = title,
            description = description,
            color = color
        )

        if (notes.any { it.id == noteId }) {
            updateEditedNoteInCalendar(note)
        } else {
            addNoteToCalendar(note)
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
                    calendarDayDecorators.add(noteDay)
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Error fetching note", e)
                }
            }
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

    private fun addNoteToCalendar(note: Note) {
        val date = note.date

        noteDays[date]?.removeIf { it.id == note.id }
        if (!noteDays.containsKey(date)) {
            noteDays[date] = ArrayList()
        }
        noteDays[date]?.add(note)
        calendarDayDecorators.add(date)

        notesAdapter.notifyItemInserted(notes.size - 1)
        updateCalendarWithNotes()
        loadNotesForDate(date)
    }

    private fun updateEditedNoteInCalendar(note: Note) {
        notes.removeIf { it.id == note.id }
        noteDays[note.date]?.removeIf { it.id == note.id }

        notes.add(note)
        if (!noteDays.containsKey(note.date)) {
            noteDays[note.date] = ArrayList()
        }
        noteDays[note.date]?.add(note)

        notesAdapter.notifyDataSetChanged()
    }

    private fun updateCalendarWithNotes() {
        calendarView.removeDecorators()
        if (calendarDayDecorators.isNotEmpty()) {
            calendarView.addDecorator(DotDecorator(calendarDayDecorators))
        }
        calendarView.invalidate()
    }

    private fun editNote(note: Note) {
        val intent = Intent(this, AddNoteActivity::class.java).apply {
            putExtra("noteId", note.id)
            putExtra("title", note.title)
            putExtra("description", note.description)
            putExtra("color", note.color)
            putExtra("date", note.date.date.toString())
        }
        editNoteLauncher.launch(intent)
    }

    private fun deleteNote(note: Note) {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes/${note.id}.json")

        storageRef.delete().addOnSuccessListener {
            notes.remove(note)
            notesAdapter.notifyItemRemoved(notes.indexOf(note))
            noteDays[note.date]?.removeIf { it.id == note.id }

            if (noteDays[note.date]?.isEmpty() == true) {
                noteDays.remove(note.date)
                calendarDayDecorators.remove(note.date)
            }

            updateCalendarWithNotes()
            loadNotesForDate(note.date)

            Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error deleting note", e)
            Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show()
        }
    }
}


