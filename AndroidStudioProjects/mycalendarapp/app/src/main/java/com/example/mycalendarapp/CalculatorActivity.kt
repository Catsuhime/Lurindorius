package com.example.mycalendarapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class CalculatorActivity : AppCompatActivity() {

    private lateinit var invoicesReceivedInput: EditText
    private lateinit var invoicesIssuedInput: EditText
    private lateinit var bankDocumentsInput: EditText
    private lateinit var cashRegisterDocsInput: EditText
    private lateinit var advancePaymentsInput: EditText
    private lateinit var fixedAssetsInput: EditText
    private lateinit var authorContractsInput: EditText
    private lateinit var transportMeansInput: EditText
    private lateinit var localBusinessTripsInput: EditText
    private lateinit var internationalBusinessTripsInput: EditText
    private lateinit var payrollOperationsInput: EditText
    private lateinit var resultTextView: TextView
    private lateinit var calculateButton: Button

    // Prices for each document type
    private val documentPrices = mapOf(
        "invoicesReceived" to 1,
        "invoicesIssued" to 1,
        "bankDocuments" to 1,
        "cashRegisterDocs" to 1,
        "advancePayments" to 2,
        "fixedAssets" to 2,
        "authorContracts" to 2,
        "transportMeans" to 2,
        "localBusinessTrips" to 2,
        "internationalBusinessTrips" to 3,
        "payrollOperations" to 15
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        // Initialize input fields and result view
        invoicesReceivedInput = findViewById(R.id.invoicesReceivedInput)
        invoicesIssuedInput = findViewById(R.id.invoicesIssuedInput)
        bankDocumentsInput = findViewById(R.id.bankDocumentsInput)
        cashRegisterDocsInput = findViewById(R.id.cashRegisterDocsInput)
        advancePaymentsInput = findViewById(R.id.advancePaymentsInput)
        fixedAssetsInput = findViewById(R.id.fixedAssetsInput)
        authorContractsInput = findViewById(R.id.authorContractsInput)
        transportMeansInput = findViewById(R.id.transportMeansInput)
        localBusinessTripsInput = findViewById(R.id.localBusinessTripsInput)
        internationalBusinessTripsInput = findViewById(R.id.internationalBusinessTripsInput)
        payrollOperationsInput = findViewById(R.id.payrollOperationsInput)
        resultTextView = findViewById(R.id.resultTextView)
        calculateButton = findViewById(R.id.calculateButton)

        calculateButton.setOnClickListener { calculateTotalPrice() }

        // Initialize result text view
        resultTextView.text = "Total Price: €0"

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_all_notes -> {
                    val intent = Intent(this, AllNotesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_back -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_documents -> {
                    startActivity(Intent(this, DocumentsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun calculateTotalPrice() {
        var totalPrice = 0

        totalPrice += calculateCost(invoicesReceivedInput, "invoicesReceived")
        totalPrice += calculateCost(invoicesIssuedInput, "invoicesIssued")
        totalPrice += calculateCost(bankDocumentsInput, "bankDocuments")
        totalPrice += calculateCost(cashRegisterDocsInput, "cashRegisterDocs")
        totalPrice += calculateCost(advancePaymentsInput, "advancePayments")
        totalPrice += calculateCost(fixedAssetsInput, "fixedAssets")
        totalPrice += calculateCost(authorContractsInput, "authorContracts")
        totalPrice += calculateCost(transportMeansInput, "transportMeans")
        totalPrice += calculateCost(localBusinessTripsInput, "localBusinessTrips")
        totalPrice += calculateCost(internationalBusinessTripsInput, "internationalBusinessTrips")
        totalPrice += calculateCost(payrollOperationsInput, "payrollOperations")

        // Add flat fee of 100 euros
        totalPrice += 100

        // Display the total price
        resultTextView.text = String.format("Total Price: €%d", totalPrice)

        // Print total price to terminal/logcat
        //Log.d("CalculatorActivity", "Total Price: €$totalPrice")
    }

    private fun calculateCost(inputField: EditText, documentType: String): Int {
        val inputValue = inputField.text.toString().toIntOrNull() ?: 0
        val pricePerDocument = documentPrices[documentType] ?: 0

        val calculatedCost = when (documentType) {
            "invoicesReceived", "invoicesIssued", "cashRegisterDocs", "fixedAssets" -> {
                // Ignore first 10 documents, charge for extra
                if (inputValue > 10) {
                    pricePerDocument * (inputValue - 10) // Charge for extra documents
                } else {
                    0 // No charge for up to 10 documents
                }
            }
            "bankDocuments" -> {
                // Ignore first 150 documents, charge for extra
                if (inputValue > 150) {
                    pricePerDocument * (inputValue - 150)
                } else {
                    0
                }
            }
            "advancePayments", "authorContracts", "transportMeans", "payrollOperations" -> {
                // Ignore first 2 documents, charge for extra
                if (inputValue > 2) {
                    pricePerDocument * (inputValue - 2)
                } else {
                    0
                }
            }
            "localBusinessTrips" -> {
                // Ignore first 4 documents, charge for extra
                if (inputValue > 4) {
                    pricePerDocument * (inputValue - 4)
                } else {
                    0
                }
            }
            "internationalBusinessTrips" -> {
                // Ignore first 1 document, charge for extra
                if (inputValue > 1) {
                    pricePerDocument * (inputValue - 1)
                } else {
                    0
                }
            }
            else -> 0 // Default case if no match
        }

        // Print individual document calculation to terminal/logcat
        //Log.d("CalculatorActivity", "Document Type: $documentType, Number: $inputValue, Cost: €$calculatedCost")

        return calculatedCost
    }
}






