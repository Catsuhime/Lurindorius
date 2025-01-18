package com.example.mycalendarapp
//DocumentAdapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentAdapter(
    private val documents: MutableList<Document>,
    private val onDelete: (Document) -> Unit,
    private val onDownload: (Document) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentIdTextView: TextView = itemView.findViewById(R.id.documentIdTextView)
        val nameTextView: TextView = itemView.findViewById(R.id.documentNameTextView)
        val uploadDateTextView: TextView = itemView.findViewById(R.id.uploadDateTextView)
        val companyTextView: TextView = itemView.findViewById(R.id.companyTextView)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val downloadButton: Button = itemView.findViewById(R.id.downloadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documents[position]
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        holder.documentIdTextView.text = "ID: ${document.id}"
        holder.nameTextView.text = "Name: ${document.name}"
        holder.uploadDateTextView.text = "Uploaded: ${dateFormat.format(Date(document.uploadDate))}"
        holder.companyTextView.text = "Company: ${document.company}"

        holder.deleteButton.setOnClickListener { onDelete(document) }
        holder.downloadButton.setOnClickListener { onDownload(document) }
    }

    fun updateData(newDocuments: List<Document>) {
        Log.d("DocumentAdapter", "Updating adapter with ${newDocuments.size} documents.")
        documents.clear()
        documents.addAll(newDocuments.sortedByDescending { it.uploadDate })
        notifyDataSetChanged()
    }

    override fun getItemCount() = documents.size
}

