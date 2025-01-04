package com.example.mycalendarapp
//RegistrationActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayInputStream

class RegistrationActivity : AppCompatActivity() {

    private lateinit var codeEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        codeEditText = findViewById(R.id.codeEditText)
        nameEditText = findViewById(R.id.nameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        registerButton.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val code = codeEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (code.isEmpty() || name.isEmpty() || lastName.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val storageRef = FirebaseStorage.getInstance().reference
        val codesRef = storageRef.child("codes/$code.json")
        val usersRef = storageRef.child("users/$code.json")

        codesRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
            val codeStatus = String(data)
            if (codeStatus == "used") {
                Toast.makeText(this, "Code has already been used", Toast.LENGTH_SHORT).show()
            } else {
                // Save user data to Firebase Storage
                val userData = """
                    {
                        "name": "$name",
                        "lastName": "$lastName",
                        "password": "$password"
                    }
                """.trimIndent()

                usersRef.putStream(ByteArrayInputStream(userData.toByteArray()))
                    .addOnSuccessListener {
                        // Mark the code as used
                        codesRef.putStream(ByteArrayInputStream("used".toByteArray()))
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Registration", "Error saving user data: ${e.message}")
                        Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Invalid code", Toast.LENGTH_SHORT).show()
            Log.e("Registration", "Error checking code: ${e.message}")
        }
    }
}

