package com.example.mycalendarapp
// DocumentsActivity.kt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
import java.io.File
import java.util.*

class DocumentsActivity : AppCompatActivity() {

    private lateinit var uploadDocumentButton: Button
    private lateinit var documentsRecyclerView: RecyclerView
    private lateinit var companySpinner: Spinner
    private lateinit var manualCompanyEditText: EditText
    private val documents = mutableListOf<Document>()
    private val documentsAdapter = DocumentAdapter(documents, ::deleteDocument, ::downloadDocument)
    private val companyNames = mutableListOf<String>()
    private lateinit var pickDocumentLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documents)

        uploadDocumentButton = findViewById(R.id.uploadDocumentButton)
        documentsRecyclerView = findViewById(R.id.documentsRecyclerView)
        companySpinner = findViewById(R.id.companySpinner)
        manualCompanyEditText = findViewById(R.id.manualCompanyEditText)

        documentsRecyclerView.layoutManager = LinearLayoutManager(this)
        documentsRecyclerView.adapter = documentsAdapter

        pickDocumentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                uri?.let { uploadDocument(it) }
            }
        }

        uploadDocumentButton.setOnClickListener {
            openFilePicker()
        }

        loadDocumentsFromFirebaseStorage()
        fetchCompanyNames()

        requestStoragePermissions()  // Request storage permissions
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "application/msword", "application/vnd.ms-excel"))
        pickDocumentLauncher.launch(intent)
    }

    private fun fetchCompanyNames() {
        val storageRef = FirebaseStorage.getInstance().reference.child("notes")
        storageRef.listAll().addOnSuccessListener { listResult ->
            val companies = mutableSetOf<String>()
            val tasks = listResult.items.map { item ->
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val noteJson = String(data)
                    val note = convertJsonToNote(noteJson)
                    // Add the company name to the set
                    if (note.company.isNotEmpty()) {
                        companies.add(note.company)
                    }
                }.addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Error fetching note", e)
                }
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener {
                // Convert the set to a list and update the spinner
                companyNames.clear()
                companyNames.addAll(companies)
                companyNames.add("Add New Company") // Optional: add an option for manual entry

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, companyNames.toList())
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                companySpinner.adapter = adapter
            }
        }.addOnFailureListener { e ->
            Log.e("FirebaseStorage", "Error listing notes", e)
        }
    }

    private fun convertJsonToNote(json: String): Note {
        val gson = Gson()
        return gson.fromJson(json, Note::class.java)
    }

    private fun uploadDocument(fileUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference.child("documents/${UUID.randomUUID()}.pdf")
        val uploadTask = storageRef.putFile(fileUri)

        uploadTask.addOnSuccessListener {
            val documentId = UUID.randomUUID().toString()
            val uploadDate = System.currentTimeMillis()

            val company = if (manualCompanyEditText.text.isNotEmpty()) {
                manualCompanyEditText.text.toString()
            } else {
                companySpinner.selectedItem?.toString() ?: "Unknown Company"
            }

            val document = Document(documentId, uploadDate, company, storageRef.path)
            saveDocumentMetadata(document)
            loadDocumentsFromFirebaseStorage()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload document", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveDocumentMetadata(document: Document) {
        // Save the document metadata to Firestore or Realtime Database
        val metadataRef = FirebaseStorage.getInstance().reference.child("documents_metadata/${document.id}.json")
        val documentJson = Gson().toJson(document)
        metadataRef.putBytes(documentJson.toByteArray())
    }

    private fun loadDocumentsFromFirebaseStorage() {
        // Load document metadata from Firestore or Firebase
        val storageRef = FirebaseStorage.getInstance().reference.child("documents_metadata")

        documents.clear()
        storageRef.listAll().addOnSuccessListener { listResult ->
            for (item in listResult.items) {
                item.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
                    val documentJson = String(data)
                    val document = Gson().fromJson(documentJson, Document::class.java)
                    documents.add(document)
                    documentsAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun deleteDocument(document: Document) {
        val storageRef = FirebaseStorage.getInstance().reference.child(document.path)

        storageRef.delete().addOnSuccessListener {
            val metadataRef = FirebaseStorage.getInstance().reference.child("documents_metadata/${document.id}.json")
            metadataRef.delete()

            documents.remove(document)
            documentsAdapter.notifyDataSetChanged()
        }
    }

    private fun requestStoragePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with your action
            } else {
                Toast.makeText(this, "Permission denied to write to your storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadDocument(document: Document) {
        val storageRef = FirebaseStorage.getInstance().reference.child(document.path)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val localFile = File(downloadsDir, "${document.id}.pdf") // Change the extension if needed

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        storageRef.getFile(localFile).addOnSuccessListener {
            Toast.makeText(this, "Document downloaded to ${localFile.absolutePath}", Toast.LENGTH_LONG).show()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to download document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}



