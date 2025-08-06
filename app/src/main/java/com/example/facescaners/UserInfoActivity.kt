package com.example.facescaners

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


class UserInfoActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var imageProfile: ImageView
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var textSalary: TextView
    private lateinit var textHours: TextView
    private lateinit var statusCircle: View
    private lateinit var btnSignOut: Button
    private lateinit var tableReport: TableLayout

    private var googleId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        // Ініціалізація елементів
        imageProfile = findViewById(R.id.imageProfile)
        textName = findViewById(R.id.textName)
        textEmail = findViewById(R.id.textEmail)
        textSalary = findViewById(R.id.textSalary)
        textHours = findViewById(R.id.textHours)
        statusCircle = findViewById(R.id.statusCircle)
        btnSignOut = findViewById(R.id.btnSignOut)
        tableReport = findViewById(R.id.tableReport)

        // Google ID
        val account = GoogleSignIn.getLastSignedInAccount(this)
        val googleId = account?.id
        Log.d("GOOGLE_ID", "ID: $googleId")

        // Google Sign-In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("298440350587-9h2jbthcu31rgaln07ljmkme435eajdj.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Вихід з акаунту
        btnSignOut.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        googleId?.let {
            loadUserData(it)
            loadUserReport(it)
        }
    }

    override fun onResume() {
        super.onResume()
        googleId?.let {
            loadUserData(it)
            loadUserReport(it)
        }
    }

    private fun loadUserData(googleId: String) {
        val url = "http://192.168.3.7:5000/user/$googleId"
        val queue = Volley.newRequestQueue(this)

        val jsonRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                textName.text = response.getString("name")
                textEmail.text = response.getString("email")
                textSalary.text = "Очікувана зарплата: ${response.optString("total_salary_current_month", "—")}"
                textHours.text = "Годин: ${response.optString("total_minutes_current_month", "—")}"

                val status = response.optString("status", "offline")
                val avatarUrl = response.optString("avatar_url", "")
                Glide.with(this).load(avatarUrl).into(imageProfile)

                statusCircle.setBackgroundResource(
                    if (status == "active") R.drawable.green_circle else R.drawable.gray_circle
                )
            },
            { error ->
                Toast.makeText(this, "Помилка при завантаженні даних", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonRequest)
    }

    private fun loadUserReport(googleId: String) {
        val url = "http://192.168.3.7/user/$googleId/report"
        val queue = Volley.newRequestQueue(this)

        val jsonRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val reportArray = response.getJSONArray("report")

                for (i in 0 until reportArray.length()) {
                    val item = reportArray.getJSONObject(i)
                    val row = TableRow(this)

                    // Отримуємо дату з JSON
                    val rawDate = item.getString("date")

                    // Форматуємо дату
                    val formattedDate = try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC") // для правильного парсингу
                        val dateObj = inputFormat.parse(rawDate)

                        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        outputFormat.format(dateObj!!)
                    } catch (e: Exception) {
                        rawDate // fallback на випадок помилки
                    }

                    val date = TextView(this).apply {
                        text = formattedDate
                        setPadding(8, 8, 8, 8)
                    }

                    val salary = TextView(this).apply {
                        text = item.optString("salary", "—")
                        setPadding(8, 8, 8, 8)
                    }

                    val hours = TextView(this).apply {
                        text = item.optString("hours", "—")
                        setPadding(8, 8, 8, 8)
                    }

                    row.addView(date)
                    row.addView(salary)
                    row.addView(hours)
                    tableReport.addView(row)
                }
            },
            { error ->
                Toast.makeText(this, "Не вдалося завантажити звіт", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(jsonRequest)
    }

}
