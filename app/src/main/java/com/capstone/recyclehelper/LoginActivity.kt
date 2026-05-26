package com.capstone.recyclehelper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etLoginUsername)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val btnLogin   = findViewById<Button>(R.id.btnLoginSubmit)
        val tvError    = findViewById<TextView>(R.id.tvLoginError)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                tvError.text = "아이디와 비밀번호를 입력해주세요."
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            RetrofitClient.api.login(LoginRequest(username, password))
                .enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, res: Response<LoginResponse>) {
                        btnLogin.isEnabled = true
                        val body = res.body()
                        if (res.isSuccessful && body != null) {
                            TokenStore.token    = body.token
                            TokenStore.adminId  = body.adminId
                            TokenStore.username = body.username
                            TokenStore.name     = body.name
                            TokenStore.role     = body.role
                            TokenStore.floor    = body.floor

                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("admin_name", body.name ?: body.username ?: username)
                            startActivity(intent)
                            finish()
                        } else {
                            tvError.text = "아이디 또는 비밀번호가 틀렸습니다."
                            tvError.visibility = View.VISIBLE
                        }
                    }
                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        btnLogin.isEnabled = true
                        tvError.text = "서버 연결 실패: ${t.message}"
                        tvError.visibility = View.VISIBLE
                    }
                })
        }
    }
}