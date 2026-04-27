package com.capstone.recyclehelper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etLoginUsername)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin = findViewById<Button>(R.id.btnLoginSubmit)
        val tvError = findViewById<TextView>(R.id.tvLoginError)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                tvError.text = "아이디와 비밀번호를 입력해주세요."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (username == "admin" && password == "admin1234") {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("admin_name", username)
                startActivity(intent)
                finish()
            } else {
                tvError.text = "아이디 또는 비밀번호가 틀렸습니다."
                tvError.visibility = View.VISIBLE
            }
        }
    }
}