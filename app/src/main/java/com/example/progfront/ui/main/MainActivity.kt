package com.example.progfront.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.progfront.R
import com.example.progfront.databinding.ActivityMainBinding
import com.example.progfront.ui.auth.login.LoginActivity
import com.example.progfront.ui.schedule.CreateScheduleActivity
import com.example.progfront.utils.TokenManager
import com.example.progfront.data.remote.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager
    private var isProfileDestination: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Hide FAB on profile
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Profile screen is mapped to navigation_notifications per existing setup
            isProfileDestination = destination.id == R.id.navigation_notifications
            invalidateOptionsMenu()
            if (isProfileDestination) {
                binding.fab.hide()
            } else {
                binding.fab.show()
            }
        }

        binding.fab.setOnClickListener {
            val intent = Intent(this, CreateScheduleActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate logout only on Profile destination
        if (isProfileDestination) {
            menuInflater.inflate(R.menu.profile_menu, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> { confirmLogout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.profile_logout_confirm_title))
            .setMessage(getString(R.string.profile_logout_confirm_message))
            .setPositiveButton(getString(R.string.profile_logout_yes)) { d, _ ->
                d.dismiss(); performLogout()
            }
            .setNegativeButton(getString(R.string.profile_logout_no)) { d, _ -> d.dismiss() }
            .show()
    }

    private fun performLogout() {
        val bearer = tokenManager.getBearerToken()
        tokenManager.clearTokens()
        if (bearer == null) { navigateToLogin(); return }
        RetrofitClient.instance.logout(bearer)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    navigateToLogin()
                }
                override fun onFailure(call: Call<Void>, t: Throwable) {
                    navigateToLogin()
                }
            })
    }

    private fun navigateToLogin() {
        Toast.makeText(this, getString(R.string.profile_logout), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
