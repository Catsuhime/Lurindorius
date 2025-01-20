package com.example.mycalendarapp
//AllNotesActivity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.text.SimpleDateFormat
import java.util.*

class AllNotesActivity : AppCompatActivity() {

    private lateinit var notesRecyclerView: RecyclerView
    private val notes = mutableListOf<Note>()
    private val filteredNotes = mutableListOf<Note>()
    private val notesAdapter = NoteAdapter(filteredNotes, ::deleteNote, ::editNote)

    private lateinit var companyFilterSpinner: Spinner
    private lateinit var titleFilterEditText: EditText
    private lateinit var dateFilterButton: Button
    private val companyList = mutableSetOf<String>()

    private var selectedDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_notes)

        // Initialize UI elements
        notesRecyclerView = findViewById(R.id.allNotesRecyclerView)
        companyFilterSpinner = findViewById(R.id.companyFilterSpinner)
        titleFilterEditText = findViewById(R.id.titleFilterEditText)
        dateFilterButton = findViewById(R.id.dateFilterButton)

        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = notesAdapter

        loadAllNotesFromFirebaseStorage()

        companyFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        titleFilterEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        companyFilterSpinner.setSelection(0)

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_all_notes -> {
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
                R.id.nav_logout -> {
                    logOut()
                    true
                }
                else -> false
            }
        }

        dateFilterButton.setOnClickListener { showDatePicker() }
    }

    private fun logOut() {
        // Clear the login status from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyCalendarApp", MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", false)
            remove("userName")
            apply()
        }

        // Redirect to LoginActivity
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
        finish()
    }

    private fun loadAllNotesFromFirebaseStorage() {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes")

        storageRef.listAll().addOnSuccessListener { listResult ->
            val tasks = listResult.items.map { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val noteJson = String(data)
                    val note = convertJsonToNote(noteJson)

                    notes.add(note)
                    companyList.add(note.company) // Add companies to the list
                }
            }

            // Check if notes are loaded correctly
            tasks.lastOrNull()?.addOnCompleteListener {
                populateCompanyFilterSpinner()
                notesAdapter.updateNotes(notes) // Update RecyclerView with all notes
                Log.d("AllNotesActivity", "Total Notes Loaded: ${notes.size}")
            }

        }.addOnFailureListener { e ->
            Log.e("AllNotesActivity", "Error loading notes: ${e.message}")
        }
    }

    private fun populateCompanyFilterSpinner() {
        // Add "All Companies" as the first option, followed by the sorted company list
        val companyOptions = mutableListOf("All Companies").apply {
            addAll(companyList.sorted()) // Add other companies alphabetically
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, companyOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        companyFilterSpinner.adapter = adapter

        // Set "All Companies" as the default selection
        companyFilterSpinner.setSelection(0)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            dateFilterButton.text = selectedDate
            applyFilters()
        }, year, month, day).show()
    }

    private fun applyFilters() {
        val selectedCompany = companyFilterSpinner.selectedItem?.toString()
        val titleQuery = titleFilterEditText.text.toString()
        val dateQuery = selectedDate

        filteredNotes.clear()
        filteredNotes.addAll(notes.filter { note ->
            val noteDate = formatDate(note.date)

            (selectedCompany == "All Companies" || note.company == selectedCompany) &&
                    (titleQuery.isEmpty() || note.title.contains(titleQuery, ignoreCase = true)) &&
                    (dateQuery == null || noteDate == dateQuery)
        })
        notesAdapter.notifyDataSetChanged()
    }


    private fun formatDate(calendarDay: CalendarDay): String {
        val calendar = Calendar.getInstance()
        calendar.set(calendarDay.year, calendarDay.month - 1, calendarDay.day) // Month is zero-based
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }


    private fun convertJsonToNote(json: String): Note {
        val gson = Gson()
        return gson.fromJson(json, Note::class.java)
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
        startActivityForResult(intent, 2)
    }

    private fun deleteNote(note: Note) {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes/${note.id}.json")

        storageRef.delete().addOnSuccessListener {
            notes.remove(note)
            filteredNotes.remove(note)
            notesAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            // Handle deletion error
        }
    }
}