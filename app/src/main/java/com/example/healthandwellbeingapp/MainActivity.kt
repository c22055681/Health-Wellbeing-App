package com.example.healthandwellbeingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Link UI elements
        val btnWeightlifting = findViewById<TextView>(R.id.btnWeightlifting)
        val btnNutritionTracking = findViewById<TextView>(R.id.btnNutritionTracking)
        val btnGoalSetting = findViewById<TextView>(R.id.btnGoalSetting)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Button click listeners
        btnWeightlifting.setOnClickListener {
            startActivity(Intent(this, WeightliftingActivity::class.java))
        }

        btnNutritionTracking.setOnClickListener {
            startActivity(Intent(this, NutritionTrackingActivity::class.java))
        }

        btnGoalSetting.setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
