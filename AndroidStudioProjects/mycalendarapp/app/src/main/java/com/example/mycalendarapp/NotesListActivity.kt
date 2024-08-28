package com.example.mycalendarapp
//NotesListActivity

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDate
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.Date

class NotesListActivity : AppCompatActivity() {

    private lateinit var dateTextView: TextView
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var notesAdapter: NoteAdapter
    private lateinit var db: FirebaseFirestore

    // Extension function to convert java.util.Date to org.threeten.bp.LocalDate
    fun Date.toLocalDate(): LocalDate {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(this.time), ZoneId.systemDefault()).toLocalDate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidThreeTen.init(this)
        setContentView(R.layout.activity_notes_list)  // Ensure this matches your XML layout file name

        // Reference the views by their IDs defined in the XML layout
        dateTextView = findViewById(R.id.dateTextView)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        notesRecyclerView.layoutManager = LinearLayoutManager(this)

        db = FirebaseFirestore.getInstance()

        val dateStr = intent.getStringExtra("date")

        if (dateStr != null) {
            dateTextView.text = dateStr
            fetchNotes(dateStr)
        } else {
            // Handle the case where dateStr is null, maybe show a default message
            dateTextView.text = "Date not available"
        }
    }

    private fun fetchNotes(dateStr: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        storageRef.child("notes").listAll().addOnSuccessListener { listResult ->
            val notes = mutableListOf<Note>()
            val tasks = listResult.items.map { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val noteJson = String(data)
                    val note = convertJsonToNote(noteJson)
                    if (note.date.date.toString() == dateStr) {
                        notes.add(note)
                    }
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Error fetching note", e)
                }
            }

            // Wait for all tasks to complete before setting up the adapter
            tasks.last().addOnCompleteListener {
                notesAdapter = NoteAdapter(notes) { note ->
                    deleteNote(note)
                }
                notesRecyclerView.adapter = notesAdapter
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error listing notes", e)
        }
    }

    private fun convertJsonToNote(json: String): Note {
        val gson = Gson()
        return gson.fromJson(json, Note::class.java)
    }

    private fun deleteNote(note: Note) {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes/${note.id}.json") // Updated path with .json

        storageRef.delete().addOnSuccessListener {
            // Successfully deleted note from Firebase Storage
            val index = notesAdapter.notes.indexOf(note)
            if (index != -1) {
                notesAdapter.notes.removeAt(index)
                notesAdapter.notifyItemRemoved(index)
            }
            Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error deleting note", e)
            Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show()
        }
    }
}
