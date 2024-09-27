package com.example.mycalendarapp
//AllNotesActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson

class AllNotesActivity : AppCompatActivity() {

    private lateinit var notesRecyclerView: RecyclerView
    private val notes = mutableListOf<Note>()
    private val filteredNotes = mutableListOf<Note>()
    private val notesAdapter = NoteAdapter(filteredNotes, ::deleteNote, ::editNote)

    private lateinit var companyFilterSpinner: Spinner
    private lateinit var backButton: Button
    private val companyList = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_notes)

        // Initialize RecyclerView
        notesRecyclerView = findViewById(R.id.allNotesRecyclerView)
        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = notesAdapter

        // Initialize Spinner and Back Button
        companyFilterSpinner = findViewById(R.id.companyFilterSpinner)
        backButton = findViewById(R.id.backButton)

        // Load notes from Firebase Storage
        loadAllNotesFromFirebaseStorage()

        // Back button click listener
        backButton.setOnClickListener {
            finish() // Finish activity and return to the previous screen
        }

        // Set listener for Spinner selection
        companyFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedCompany = parent?.getItemAtPosition(position).toString()
                filterNotesByCompany(selectedCompany)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }
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

            // After loading all notes, update UI
            tasks.lastOrNull()?.addOnCompleteListener {
                populateCompanyFilterSpinner()
                notesAdapter.updateNotes(notes) // Update RecyclerView with all notes
            }

        }.addOnFailureListener { e ->
            // Handle error
        }
    }

    private fun populateCompanyFilterSpinner() {
        // Convert companyList to ArrayAdapter for Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, companyList.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        companyFilterSpinner.adapter = adapter
    }

    private fun filterNotesByCompany(company: String) {
        filteredNotes.clear()
        if (company.isNotEmpty()) {
            filteredNotes.addAll(notes.filter { it.company == company })
        } else {
            filteredNotes.addAll(notes) // Show all notes if no company is selected
        }
        notesAdapter.notifyDataSetChanged()
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
        startActivity(intent)
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




