package com.example.mycalendarapp
// DocumentsActivity.kt

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import android.Manifest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DocumentsActivity : AppCompatActivity() {

    private lateinit var documentsRecyclerView: RecyclerView
    private lateinit var companyFilterSpinner: Spinner
    private lateinit var dateFilterButton: Button
    private lateinit var clearFiltersButton: Button
    private lateinit var filterMessage: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var uploadDocumentButton: Button
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    private val documents = mutableListOf<Document>()
    private val documentsAdapter = DocumentAdapter(documents, ::deleteDocument, ::downloadDocument)
    private val companyNames = mutableListOf<String>()
    private var selectedCompany: String? = null
    private var selectedDate: Long? = null
    private var isLoading = false

    companion object {
        private val REQUEST_CODE_STORAGE_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents)

        documentsRecyclerView = findViewById(R.id.documentsRecyclerView)
        companyFilterSpinner = findViewById(R.id.companyFilterSpinner)
        dateFilterButton = findViewById(R.id.dateFilterButton)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)
        filterMessage = findViewById(R.id.filterMessage)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        uploadDocumentButton = findViewById(R.id.uploadDocumentButton)

        documentsRecyclerView.layoutManager = LinearLayoutManager(this)
        documentsRecyclerView.adapter = documentsAdapter

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { fileUri ->
                    Log.d("DocumentsActivity", "File selected: $fileUri")
                    uploadDocument(fileUri)  // Proceed with uploading the selected file
                }
            } else {
                Log.d("DocumentsActivity", "No file selected or selection failed")
            }
        }

        uploadDocumentButton.setOnClickListener {
            showCompanySelectionDialog { selectedCompanyName ->
                selectedCompany = selectedCompanyName
                openFilePicker()
            }
        }

        // Fetch initial data
        loadDocumentsFromFirebaseStorage()
        fetchCompanyNames()

        setupFilters()
        setupEndlessScrolling()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                openFilePicker()
            } else {
                // Permission was denied
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    // User denied permission permanently
                    showPermissionSettingsDialog()
                } else {
                    // Permission denied temporarily
                    Toast.makeText(this, "Permission is required to select a file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPermissionSettingsDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs permission to access your files. Please enable the permission in app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Direct user to app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/pdf" // Specify the type of file you want to open
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }



    private fun showCompanySelectionDialog(onCompanySelected: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_company, null)
        val companySpinner = dialogView.findViewById<Spinner>(R.id.companySpinner)
        val customCompanyEditText = dialogView.findViewById<EditText>(R.id.customCompanyEditText)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, companyNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        companySpinner.adapter = adapter

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Company")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedCompany = when {
                    // Custom company entered
                    customCompanyEditText.text.isNotEmpty() -> customCompanyEditText.text.toString()
                    // Pre-selected company
                    companySpinner.selectedItemPosition > 0 -> companyNames[companySpinner.selectedItemPosition]
                    else -> null
                }

                if (!selectedCompany.isNullOrEmpty()) {
                    this.selectedCompany = selectedCompany // Save the selected company globally
                    onCompanySelected(selectedCompany)
                } else {
                    Toast.makeText(this, "Please select or enter a company name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun uploadDocument(fileUri: Uri) {
        val documentId = UUID.randomUUID().toString()
        val internalFile = File(filesDir, "$documentId.pdf")  // Save file to internal storage

        // Copy file from URI to internal storage
        contentResolver.openInputStream(fileUri)?.use { inputStream ->
            internalFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        val storageRef = FirebaseStorage.getInstance().reference.child("documents/$documentId.pdf")
        val uploadTask = storageRef.putFile(Uri.fromFile(internalFile))

        if (selectedCompany.isNullOrEmpty()) {
            Toast.makeText(this, "Company not selected. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        uploadTask.addOnSuccessListener {
            val uploadDate = System.currentTimeMillis()
            val document = Document(
                id = documentId,
                uploadDate = uploadDate,
                company = selectedCompany ?: "Unknown",
                path = "documents/$documentId.pdf"
            )
            saveDocumentMetadata(document)
            loadDocumentsFromFirebaseStorage()
            Toast.makeText(this, "Document uploaded successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { exception ->
            Log.e("UploadDocument", "Failed to upload: ${exception.message}")
            Toast.makeText(this, "Failed to upload document", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDocumentMetadata(document: Document) {
        val metadataRef = FirebaseStorage.getInstance().reference.child("documents_metadata/${document.id}.json")
        val documentJson = Gson().toJson(document)
        metadataRef.putBytes(documentJson.toByteArray())
            .addOnSuccessListener {
                Toast.makeText(this, "Document metadata saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("SaveMetadata", "Failed to save metadata: ${exception.message}")
                Toast.makeText(this, "Failed to save document metadata", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupFilters() {
        companyFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCompany = if (position > 0) companyNames[position] else null
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        dateFilterButton.setOnClickListener {
            showDatePicker()
        }

        clearFiltersButton.setOnClickListener {
            selectedCompany = null
            selectedDate = null
            companyFilterSpinner.setSelection(0)
            filterMessage.visibility = View.GONE
            applyFilters()
        }
    }

    private fun setupEndlessScrolling() {
        documentsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }


    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val pickedDate = Calendar.getInstance()
                pickedDate.set(year, month, day)
                selectedDate = pickedDate.timeInMillis
                filterMessage.visibility = View.VISIBLE
                filterMessage.text = "Filtering by date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pickedDate.time)}"
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchCompanyNames() {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes")
        storageRef.listAll().addOnSuccessListener { listResult ->
            val companies = mutableSetOf<String>()
            val tasks = listResult.items.map { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val noteJson = String(data)
                    val note = Gson().fromJson(noteJson, Note::class.java)
                    if (note.company.isNotEmpty()) {
                        companies.add(note.company)
                    }
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Error fetching note", e)
                }
            }
            tasks.forEach { task -> task.addOnCompleteListener { updateCompanySpinner(companies) } }
        }
    }

    private fun updateCompanySpinner(companies: Set<String>) {
        companyNames.clear()
        companyNames.add("All Companies")
        companyNames.addAll(companies)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, companyNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        companyFilterSpinner.adapter = adapter
    }

    private fun loadDocumentsFromFirebaseStorage() {
        if (isLoading) return // Prevent further loading if already loading

        isLoading = true
        loadingProgressBar.visibility = View.VISIBLE

        val storageRef = FirebaseStorage.getInstance().reference.child("documents_metadata")
        storageRef.listAll().addOnSuccessListener { listResult ->
            // Handle document loading
            val newDocuments = mutableListOf<Document>()
            val tasks = listResult.items.map { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val documentJson = String(data)
                    val document = Gson().fromJson(documentJson, Document::class.java)
                    newDocuments.add(document)
                }
            }
            // After all tasks are complete
            Tasks.whenAll(tasks).addOnCompleteListener {
                documents.addAll(newDocuments) // Add the newly loaded documents to the list
                isLoading = false
                loadingProgressBar.visibility = View.GONE

                // Apply any active filters
                applyFilters()

                // Notify adapter that data has changed
                documentsAdapter.notifyDataSetChanged()

                Log.d("DocumentsActivity", "Loaded documents: ${newDocuments.size}")
            }
        }.addOnFailureListener { exception ->
            Log.e("DocumentsActivity", "Error loading documents: ${exception.message}")
            isLoading = false
            loadingProgressBar.visibility = View.GONE
        }
    }

    private fun applyFilters() {
        Log.d("DocumentsActivity", "Applying filters: Company = $selectedCompany, Date = $selectedDate")

        val filteredDocuments = documents.filter { document ->
            val matchesCompany = selectedCompany?.let { document.company == it } ?: true
            val matchesDate = selectedDate?.let {
                // Calculate the start and end of the selected day
                val selectedDayStart = it - (it % (24 * 60 * 60 * 1000)) // Start of the day in milliseconds
                val selectedDayEnd = selectedDayStart + (24 * 60 * 60 * 1000) - 1 // End of the day
                document.uploadDate in selectedDayStart..selectedDayEnd
            } ?: true

            matchesCompany && matchesDate
        }

        Log.d("DocumentsActivity", "Filtered documents count: ${filteredDocuments.size}")

        if (filteredDocuments.isEmpty() && (selectedCompany != null || selectedDate != null)) {
            filterMessage.text = "No documents match the filter"
            filterMessage.visibility = View.VISIBLE
        } else {
            filterMessage.visibility = View.GONE
        }

        documentsAdapter.updateData(filteredDocuments)
    }



    private fun deleteDocument(document: Document) {
        val storageRef = FirebaseStorage.getInstance().reference.child("documents_metadata/${document.id}.json")
        storageRef.delete().addOnSuccessListener {
            documents.remove(document)
            documentsAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Document deleted successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to delete document", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadDocument(document: Document) {
        val storageRef = FirebaseStorage.getInstance().reference.child(document.path)
        storageRef.getFile(File(Environment.getExternalStorageDirectory(), document.id))
            .addOnSuccessListener {
                Toast.makeText(this, "Document downloaded successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to download document", Toast.LENGTH_SHORT).show()
            }
    }
}



