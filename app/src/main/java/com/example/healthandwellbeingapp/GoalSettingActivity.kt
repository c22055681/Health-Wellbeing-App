package com.example.healthandwellbeingapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class GoalSettingActivity : AppCompatActivity() {

    private val goals = mutableListOf<Goal>()
    private lateinit var goalListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_setting)

        val goalNameInput = findViewById<EditText>(R.id.goalName)
        val startValueInput = findViewById<EditText>(R.id.startValue)
        val goalValueInput = findViewById<EditText>(R.id.goalValue)
        val btnAddGoal = findViewById<Button>(R.id.btnAddGoal)
        val startDateButton = findViewById<Button>(R.id.btnStartDate)
        val endDateButton = findViewById<Button>(R.id.btnEndDate)
        goalListContainer = findViewById(R.id.goalListContainer)

        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var startDate: Calendar? = null
        var endDate: Calendar? = null

        startDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                startDate = Calendar.getInstance().apply { set(year, month, day) }
                startDateButton.text = "Start Date: ${dateFormatter.format(startDate!!.time)}"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        endDateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                endDate = Calendar.getInstance().apply { set(year, month, day) }
                endDateButton.text = "End Date: ${dateFormatter.format(endDate!!.time)}"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnAddGoal.setOnClickListener {
            val goalName = goalNameInput.text.toString()
            val startValue = startValueInput.text.toString().toFloatOrNull()
            val goalValue = goalValueInput.text.toString().toFloatOrNull()

            if (goalName.isNotEmpty() && startValue != null && goalValue != null && startValue < goalValue && startDate != null && endDate != null) {
                val daysRemaining = ((endDate!!.timeInMillis - startDate!!.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                val dailyImprovement = if (daysRemaining > 0) (goalValue - startValue) / daysRemaining else 0f

                val goal = Goal(
                    goalName,
                    startValue,
                    goalValue,
                    startValue,
                    startDate!!.timeInMillis,
                    endDate!!.timeInMillis,
                    dailyImprovement
                )

                goals.add(goal)
                addGoalView(goal)
            }
        }
    }

    private fun addGoalView(goal: Goal) {
        val goalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
        }

        val goalTextView = TextView(this).apply {
            text = "${goal.name}: ${goal.currentValue} / ${goal.targetValue}\n" +
                    "Target Daily Improvement: ${"%.2f".format(goal.dailyImprovement)}"
            textSize = 18f
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = (goal.targetValue - goal.startValue).toInt()
            progress = (goal.currentValue - goal.startValue).toInt()
        }

        val editGoalInput = EditText(this).apply {
            hint = "Enter new value"
            visibility = View.GONE
        }

        val btnUpdateGoal = Button(this).apply {
            text = "Update Goal"
            setOnClickListener {
                if (editGoalInput.visibility == View.GONE) {
                    editGoalInput.visibility = View.VISIBLE
                } else {
                    val newValue = editGoalInput.text.toString().toFloatOrNull()
                    if (newValue != null && newValue <= goal.targetValue) {
                        goal.currentValue = newValue
                        goalTextView.text = "${goal.name}: ${goal.currentValue} / ${goal.targetValue}\n" +
                                "Target Daily Improvement: ${"%.2f".format(goal.dailyImprovement)}"
                        progressBar.progress = (goal.currentValue - goal.startValue).toInt()
                        editGoalInput.visibility = View.GONE
                    }
                }
            }
        }

        goalLayout.apply {
            addView(goalTextView)
            addView(progressBar)
            addView(editGoalInput)
            addView(btnUpdateGoal)
        }

        goalListContainer.addView(goalLayout)
    }
}

data class Goal(
    val name: String,
    val startValue: Float,
    val targetValue: Float,
    var currentValue: Float,
    val startDate: Long,
    val endDate: Long,
    val dailyImprovement: Float
)
