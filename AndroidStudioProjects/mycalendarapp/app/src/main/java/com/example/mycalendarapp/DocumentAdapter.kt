package com.example.mycalendarapp
//DocumentAdapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

class DocumentAdapter(
    private val documents: List<Document>,
    private val onDelete: (Document) -> Unit,
    private val onDownload: (Document) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

    class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentIdTextView: TextView = itemView.findViewById(R.id.documentIdTextView)
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
        holder.documentIdTextView.text = document.id
        holder.uploadDateTextView.text = Date(document.uploadDate).toString()
        holder.companyTextView.text = document.company

        holder.deleteButton.setOnClickListener { onDelete(document) }
        holder.downloadButton.setOnClickListener { onDownload(document) }
    }

    override fun getItemCount() = documents.size
}
