package com.example.healthandwellbeingapp
import com.example.healthandwellbeingapp.goals.GoalSettingActivity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import android.widget.Toast
import android.widget.Button



class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)

        toolbar.setNavigationOnClickListener {
            // Already on MainActivity – do nothing or refresh
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }


        // Cards
        val weightliftingCard = findViewById<TextView>(R.id.btnWeightlifting)
        val nutritionCard = findViewById<TextView>(R.id.btnNutritionTracking)
        val goalsCard = findViewById<TextView>(R.id.btnGoalSetting)


        weightliftingCard.setOnClickListener {
            startActivity(Intent(this, WeightliftingActivity::class.java))
        }

        nutritionCard.setOnClickListener {
            startActivity(Intent(this, NutritionTrackingActivity::class.java))
        }

        goalsCard.setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }

        val challenges = listOf(
            "Do 20 squats before lunch!",
            "Drink 2L of water today!",
            "Go for a 10-minute walk.",
            "Stretch for 5 minutes.",
            "Try a 1-minute plank challenge!"
        )

        val challengeText = findViewById<TextView>(R.id.txtDailyChallenge)
        val randomChallenge = challenges.random()
        challengeText.text = randomChallenge

        val btnComplete = findViewById<Button>(R.id.btnCompleteChallenge)
        btnComplete.setOnClickListener {
            Toast.makeText(this, "Nice job completing your challenge! 💪", Toast.LENGTH_SHORT).show()
        }

    }

}
