package com.example.healthandwellbeingapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class GoalSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_setting)

        val goalNameInput = findViewById<EditText>(R.id.goalName)
        val startValueInput = findViewById<EditText>(R.id.startValue)
        val goalValueInput = findViewById<EditText>(R.id.goalValue)
        val btnAddGoal = findViewById<Button>(R.id.btnAddGoal)
        val goalListContainer = findViewById<LinearLayout>(R.id.goalListContainer)

        btnAddGoal.setOnClickListener {
            val goalName = goalNameInput.text.toString()
            val startValue = startValueInput.text.toString().toFloatOrNull()
            val goalValue = goalValueInput.text.toString().toFloatOrNull()

            if (goalName.isNotEmpty() && startValue != null && goalValue != null && startValue < goalValue) {
                addGoalView(goalName, startValue, goalValue, goalListContainer)
            }
        }
    }

    private fun addGoalView(goalName: String, startValue: Float, goalValue: Float, container: LinearLayout) {
        val goalTextView = TextView(this).apply {
            text = "$goalName: $startValue / $goalValue"
            textSize = 18f
            setPadding(10, 10, 10, 10)
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = (goalValue - startValue).toInt()
            progress = 0
            setPadding(10, 10, 10, 10)
        }

        container.addView(goalTextView)
        container.addView(progressBar)
    }
}