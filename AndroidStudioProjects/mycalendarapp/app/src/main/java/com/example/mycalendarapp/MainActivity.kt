
package com.example.mycalendarapp
//MainActivity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.storage.FirebaseStorage
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.google.gson.Gson
import org.threeten.bp.LocalDate
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var notesRecyclerView: RecyclerView
    private val notes = mutableListOf<Note>()
    private val notesAdapter = NoteAdapter(notes, ::deleteNote, ::editNote)
    private val noteDays = mutableMapOf<CalendarDay, ArrayList<Note>>()
    private val calendarDayDecorators = mutableSetOf<CalendarDay>()

    private lateinit var addNoteLauncher: ActivityResultLauncher<Intent>
    private lateinit var editNoteLauncher: ActivityResultLauncher<Intent>
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var addNoteButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        val provider = PlayIntegrityAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(provider)


        calendarView = findViewById(R.id.calendarView)
        addNoteButton = findViewById(R.id.addNoteButton)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        bottomNavigationView = findViewById(R.id.bottomNavigation)

        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = notesAdapter

        refreshData()

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

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all_notes -> {
                    val intent = Intent(this, AllNotesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_calculator -> {
                    val intent = Intent(this, CalculatorActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_documents -> {
                    val intent = Intent(this, DocumentsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        updateCalendarWithNotes()
    }

    //Main
    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        notes.clear()
        noteDays.clear()
        calendarDayDecorators.clear()
        loadNotesFromFirebaseStorage()
        updateCalendarWithNotes()
    }

    private fun handleNoteResult(data: Intent) {
        val noteId = data.getStringExtra("noteId") ?: UUID.randomUUID().toString()
        val dateString = data.getStringExtra("date") ?: return
        val title = data.getStringExtra("title") ?: return
        val description = data.getStringExtra("description") ?: return
        val color = data.getIntExtra("color", R.color.black)
        val company = data.getStringExtra("company") ?: ""

        val date = LocalDate.parse(dateString)
        val calendarDay = CalendarDay.from(date)
        val note = Note(
            id = noteId,
            date = calendarDay,
            title = title,
            description = description,
            color = color,
            company = company
        )

        if (notes.any { it.id == noteId }) {
            updateEditedNoteInCalendar(note)
        } else {
            addNoteToCalendar(note)
        }
    }


    private fun loadNotesFromFirebaseStorage() {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes")

        // Clear previous notes and data
        notes.clear()
        noteDays.clear()
        calendarDayDecorators.clear()

        storageRef.listAll().addOnSuccessListener { listResult ->
            val tasks = listResult.items.map { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val noteJson = String(data)
                    val note = convertJsonToNote(noteJson)
                    val noteDay = note.date

                    notes.add(note)
                    if (!noteDays.containsKey(noteDay)) {
                        noteDays[noteDay] = ArrayList()
                    }
                    noteDays[noteDay]?.add(note)
                    calendarDayDecorators.add(noteDay)
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Error fetching note", e)
                }
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener {
                notesAdapter.updateNotes(notes) // Update adapter with the new notes list
                updateCalendarWithNotes()
            }
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
        } else {
            Log.d("UpdateCalendar", "No decorators to add")
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
            putExtra("company", note.company)
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
