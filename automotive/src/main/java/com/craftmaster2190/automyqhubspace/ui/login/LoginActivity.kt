package com.craftmaster2190.automyqhubspace.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.craftmaster2190.automyqhubspace.HubSpaceClient
import com.craftmaster2190.automyqhubspace.MainActivity
import com.craftmaster2190.automyqhubspace.MyQClient
import com.craftmaster2190.automyqhubspace.databinding.ActivityLoginBinding

data class AppCredentials(
    val myqUsername: String,
    val myqPassword: String,
    val hubspaceUsername: String,
    val hubspacePassword: String
) {
    companion object {
        @JvmStatic
        fun fromContext(context: Context): AppCredentials? {
            val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
            val myqUsername = prefs.getString("myqUsername", null)
            val myqPassword = prefs.getString("myqPassword", null)
            val hubspaceUsername = prefs.getString("hubspaceUsername", null)
            val hubspacePassword = prefs.getString("hubspacePassword", null)
            if (myqUsername != null && myqPassword != null && hubspaceUsername != null && hubspacePassword != null) {
                return AppCredentials(myqUsername, myqPassword, hubspaceUsername, hubspacePassword)
                    .also { Log.i(AppCredentials::class.java.simpleName, it.toString()) }
            }
            return null
        }
    }

    fun persist(context: Context) {
        val prefs = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("myqUsername", myqUsername)
            .putString("myqPassword", myqPassword)
            .putString("hubspaceUsername", hubspaceUsername)
            .putString("hubspacePassword", hubspacePassword)
            .apply()
    }
}

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.login.setOnClickListener {
            val myqUsername = binding.myqUsername.text.toString().takeIf { it.isNotBlank() }
            val myqPassword = binding.myqPassword.text.toString().takeIf { it.isNotBlank() }
            val hubspaceUsername =
                binding.hubspaceUsername.text.toString().takeIf { it.isNotBlank() }
            val hubspacePassword =
                binding.hubspacePassword.text.toString().takeIf { it.isNotBlank() }
            if (myqUsername != null && myqPassword != null && hubspaceUsername != null && hubspacePassword != null) {
                AppCredentials(myqUsername, myqPassword, hubspaceUsername, hubspacePassword).persist(this)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}