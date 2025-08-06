package com.example.facescaners

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException


class MainActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 100
        private const val TAG = "GoogleSignIn"
        private const val WEB_CLIENT_ID = "298440350587-9h2jbthcu31rgaln07ljmkme435eajdj.apps.googleusercontent.com"
    }

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Конфігурація Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("298440350587-9h2jbthcu31rgaln07ljmkme435eajdj.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Перевірка, чи користувач вже входив
        checkExistingSignIn()

        findViewById<com.google.android.gms.common.SignInButton>(R.id.btnGoogleSignIn).setOnClickListener {
            signIn()
        }
    }

    private fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            handleSignInSuccess(account)
        }
    }


    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.let {
                handleSignInSuccess(it)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=${e.statusCode}", e)
            showToast("Sign in failed: ${e.statusCode}")
        }
    }

    private fun handleSignInSuccess(account: GoogleSignInAccount) {
        val intent = Intent(this, UserInfoActivity::class.java).apply {
            putExtra("name", account.displayName)
            putExtra("email", account.email)
        }
        startActivity(intent)

        // Якщо хочеш — можеш й тут викликати sendTokenToServer(idToken)
        val idToken = account.idToken
        if (idToken != null) {
            sendTokenToServer(idToken)
        }
    }



    private fun sendTokenToServer(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.authWithGoogle(AuthRequest(token = idToken))
                withContext(Dispatchers.Main) {
                    showToast("Welcome ${response.user.name}")
                    // Тут можна перейти до наступного екрану
                    // startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    // finish()
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HTTP error: ${e.code()}", e)
                showToast("Server error: ${e.code()}")
            } catch (e: Exception) {
                Log.e(TAG, "Auth failed", e)
                showToast("Auth failed: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}