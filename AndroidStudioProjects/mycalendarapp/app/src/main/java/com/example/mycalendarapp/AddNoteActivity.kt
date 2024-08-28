package com.example.mycalendarapp
//AddNoteActivity

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.prolificinteractive.materialcalendarview.CalendarDay
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import java.util.*

class AddNoteActivity : AppCompatActivity() {

    private lateinit var dateButton: Button
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var colorSpinner: Spinner
    private lateinit var timePicker: TimePicker
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private lateinit var selectedDate: LocalDate

    private val REQUEST_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        dateButton = findViewById(R.id.dateButton)
        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        colorSpinner = findViewById(R.id.colorSpinner)
        timePicker = findViewById(R.id.timePicker)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        selectedDate = LocalDate.now()
        updateDateButtonText()

        // Populate the Spinner with color options
        val colorAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.colors_array,
            android.R.layout.simple_spinner_item
        )
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        colorSpinner.adapter = colorAdapter

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    updateDateButtonText()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        saveButton.setOnClickListener {
            val title = titleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val selectedColorName = colorSpinner.selectedItem.toString()
            val color = getColorFromSpinner(selectedColorName)
            val time = LocalTime.of(timePicker.hour, timePicker.minute)
            val dateTime = ZonedDateTime.of(selectedDate, time, ZoneId.systemDefault())

            if (title.isBlank() || description.isBlank()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.SCHEDULE_EXACT_ALARM
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (canScheduleExactAlarms()) {
                    setAlarm(dateTime, title, description)
                } else {
                    // Handle devices that do not support exact alarms
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM),
                    REQUEST_PERMISSION_CODE
                )
            }

            val noteId = UUID.randomUUID().toString() // Generate a unique ID for the note
            val note = Note(
                id = noteId,
                date = CalendarDay.from(selectedDate),
                title = title,
                description = description,
                color = color
            )

            saveNoteToFirebaseStorage(note)
        }


        backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateDateButtonText() {
        dateButton.text = selectedDate.toString()
    }

    private fun getColorFromSpinner(colorName: String): Int {
        return when (colorName) {
            "Red" -> R.color.red
            "Green" -> R.color.green
            "Blue" -> R.color.blue
            else -> R.color.black
        }
    }

    private fun setAlarm(dateTime: ZonedDateTime, title: String, description: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("description", description)
        }
    }

    private fun saveNoteToFirebaseStorage(note: Note) {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes/${note.id}.json")
        val gson = Gson()
        val noteJson = gson.toJson(note)

        storageRef.putBytes(noteJson.toByteArray())
            .addOnSuccessListener {
                Log.d("FirebaseStorage", "Note uploaded successfully")
                setResult(RESULT_OK, Intent().apply {
                    putExtra("date", selectedDate.toString())
                    putExtra("title", note.title)
                    putExtra("description", note.description)
                    putExtra("color", note.color)
                })
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseStorage", "Error uploading note", e)
                Toast.makeText(this, "Failed to save note", Toast.LENGTH_SHORT).show()
            }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}


