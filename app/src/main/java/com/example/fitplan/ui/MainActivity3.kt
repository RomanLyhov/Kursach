package com.example.fitplan.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.fitplan.DataBase.DatabaseHelper
import com.example.fitplan.Models.User
import com.example.fitplan.R
import com.example.fitplan.ui.login.Login
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity3 : AppCompatActivity() {

    private lateinit var bottomPanel: LinearLayout
    private var currentTab: String = "food"
    var currentUser: User? = null

    private val sharedPref by lazy {
        getSharedPreferences("session", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        bottomPanel = findViewById(R.id.bottom_navigation)
        setupPanelClicks()
        savedInstanceState?.getString("currentTab")?.let {
            currentTab = it
        }

        val userId = sharedPref.getLong("user_id", -1L)

        if (userId == -1L) {
            openLogin()
        } else {
            loadCurrentUser(userId) { user ->
                if (user != null) {
                    currentUser = user
                    bottomPanel.visibility = View.VISIBLE
                    if (savedInstanceState == null) {
                        openTab(currentTab, addToBackStack = false)
                    } else {
                        setActive(currentTab)
                    }
                } else {
                    openLogin()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentTab", currentTab)
    }

    private fun loadCurrentUser(userId: Long, onLoaded: (User?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseHelper(this@MainActivity3)
            val user = db.getUserById(userId)
            withContext(Dispatchers.Main) {
                onLoaded(user)
            }
        }
    }

    fun onLoginSuccess(userId: Long) {
        sharedPref.edit().putLong("user_id", userId).apply()

        loadCurrentUser(userId) { user ->
            if (user != null) {
                currentUser = user
                bottomPanel.visibility = View.VISIBLE
                openTab("profile", addToBackStack = false)
            }
        }
    }

    private fun openLogin() {
        currentTab = "food"
        bottomPanel.visibility = View.GONE

        supportFragmentManager.commit {
            replace(R.id.fragment_container, Login())
            setReorderingAllowed(true)
        }
    }

    private fun setupPanelClicks() {
        findViewById<LinearLayout>(R.id.nav_food).setOnClickListener {
            openTab("food", addToBackStack = false)
        }
        findViewById<LinearLayout>(R.id.nav_workout).setOnClickListener {
            openTab("workout", addToBackStack = false)
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            openTab("profile", addToBackStack = false)
        }
    }

    private fun openTab(tab: String, addToBackStack: Boolean = false) {
        if (currentTab == tab) return
        currentTab = tab

        val fragment: Fragment = when (tab) {
            "food" -> NutritionFragment()
            "workout" -> WorkoutFragment()
            "profile" -> ProfileFragment()
            else -> return
        }

        replaceFragment(fragment, addToBackStack)
        setActive(tab)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, fragment)
            if (addToBackStack) {
                addToBackStack(null)
            }
            setReorderingAllowed(true)
        }
    }

    private fun setActive(item: String) {
        setColor(R.id.nav_food, R.color.gray)
        setColor(R.id.nav_workout, R.color.gray)
        setColor(R.id.nav_reports, R.color.gray)
        setColor(R.id.nav_profile, R.color.gray)

        when (item) {
            "food" -> setColor(R.id.nav_food, R.color.purple_500)
            "workout" -> setColor(R.id.nav_workout, R.color.purple_500)
            "reports" -> setColor(R.id.nav_reports, R.color.purple_500)
            "profile" -> setColor(R.id.nav_profile, R.color.purple_500)
        }
    }

    private fun setColor(buttonId: Int, colorId: Int) {
        val button = findViewById<LinearLayout>(buttonId)
        val icon = button.getChildAt(0) as ImageView
        val text = button.getChildAt(1) as TextView
        val color = ContextCompat.getColor(this, colorId)

        icon.setColorFilter(color)
        text.setTextColor(color)
    }
}