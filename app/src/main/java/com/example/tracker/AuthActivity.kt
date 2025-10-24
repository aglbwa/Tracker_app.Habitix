package com.example.tracker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var btnGuest: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        initViews()
        setupClickListeners()

        if (auth.currentUser != null) {
            startMainActivity()
        }
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        btnGuest = findViewById(R.id.btnGuest)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            loginUser()
        }

        btnRegister.setOnClickListener {
            registerUser()
        }

        btnGuest.setOnClickListener {
            loginAsGuest()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Ошибка входа: ${getAuthErrorMessage(task.exception)}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(email)) {
            Toast.makeText(this, "Введите корректный email", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val newUser = hashMapOf<String, Any>(
                            "userId" to it.uid,
                            "userName" to email.substringBefore("@"),
                            "email" to email,
                            "totalPoints" to 0,
                            "isGuest" to false
                        )
                        saveUserToFirestore(newUser)
                    }
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Ошибка регистрации: ${getAuthErrorMessage(task.exception)}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginAsGuest() {
        showLoading(true)
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val guestUser = hashMapOf<String, Any>(
                            "userId" to it.uid,
                            "userName" to "Гость",
                            "email" to "",
                            "totalPoints" to 0,
                            "isGuest" to true
                        )
                        saveUserToFirestore(guestUser)
                    }
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Ошибка гостевого входа: ${getAuthErrorMessage(task.exception)}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirestore(user: HashMap<String, Any>) {
        val userId = user["userId"] as String
        db.collection("users")
            .document(userId)
            .set(user)
            .addOnSuccessListener {
                // Пользователь сохранен
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения пользователя", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !show
        btnRegister.isEnabled = !show
        btnGuest.isEnabled = !show
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun getAuthErrorMessage(exception: Exception?): String {
        return when {
            exception?.message?.contains("INVALID_EMAIL") == true -> {
                "Неверный формат email"
            }
            exception?.message?.contains("EMAIL_NOT_FOUND") == true -> {
                "Пользователь с таким email не найден"
            }
            exception?.message?.contains("WRONG_PASSWORD") == true -> {
                "Неверный пароль"
            }
            exception?.message?.contains("EMAIL_EXISTS") == true -> {
                "Пользователь с таким email уже существует"
            }
            exception?.message?.contains("WEAK_PASSWORD") == true -> {
                "Пароль слишком слабый"
            }
            else -> exception?.message ?: "Неизвестная ошибка"
        }
    }
}