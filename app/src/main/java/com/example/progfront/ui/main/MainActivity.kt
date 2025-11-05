package com.example.progfront.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.progfront.R
import com.example.progfront.data.repository.AuthRepository
import com.example.progfront.databinding.ActivityMainBinding
import com.example.progfront.ui.auth.login.LoginActivity
import com.example.progfront.ui.schedule.CreateScheduleActivity
import com.example.progfront.utils.TokenManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager
    private var isProfileDestination: Boolean = false
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Check if user is authenticated and set the appropriate start destination
        val hasToken = tokenManager.getAccessToken() != null
        val navGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
        navGraph.setStartDestination(
            if (hasToken) R.id.navigation_home else R.id.loginFragment
        )
        navController.graph = navGraph

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Hide/show bottom nav and FAB based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->

            isProfileDestination = destination.id == R.id.navigation_notifications

            // Hide bottom nav and FAB on login/register screens
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.navView.visibility = View.GONE
                    binding.fab.hide()
                    supportActionBar?.hide()
                }
                R.id.createScheduleFragment, R.id.scheduleDetailFragment -> {
                    // Full-screen content: hide bottom nav and FAB, keep action bar
                    binding.navView.visibility = View.GONE
                    binding.fab.hide()
                    supportActionBar?.show()
                }
                else -> {
                    binding.navView.visibility = View.VISIBLE
                    supportActionBar?.show()
                    if (isProfileDestination) {
                        binding.fab.hide()
                    } else {
                        binding.fab.show()
                    }
                }
            }

            invalidateOptionsMenu()
        }

        binding.fab.setOnClickListener {
            navController.navigate(R.id.action_global_createScheduleFragment)
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
        tokenManager.clearTokens()

        lifecycleScope.launch {
            authRepository.logout()
            // Navigate to login using navigation component
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        Toast.makeText(this, getString(R.string.profile_logout), Toast.LENGTH_SHORT).show()
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navController.navigate(R.id.action_profile_to_login)
    }
}
