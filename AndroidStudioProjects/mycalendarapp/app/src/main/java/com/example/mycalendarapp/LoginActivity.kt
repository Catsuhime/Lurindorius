package com.example.mycalendarapp
//LoginActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson

class LoginActivity : AppCompatActivity() {

    private lateinit var codeEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        codeEditText = findViewById(R.id.codeEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)

        loginButton.setOnClickListener { loginUser() }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    private fun loginUser() {
        val code = codeEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (code.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = FirebaseStorage.getInstance().reference.child("users/$code.json")

        userRef.getBytes(Long.MAX_VALUE).addOnSuccessListener { data ->
            val userData = String(data)
            val userMap = Gson().fromJson(userData, Map::class.java)

            val savedPassword = userMap["password"] as? String
            val name = userMap["name"] as? String

            if (savedPassword == password) {
                getSharedPreferences("MyCalendarApp", MODE_PRIVATE).edit().apply {
                    putBoolean("isLoggedIn", true)
                    putString("userName", name)
                    apply()
                }

                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            Log.e("Login", "Error fetching user data: ${e.message}")
        }
    }
}

