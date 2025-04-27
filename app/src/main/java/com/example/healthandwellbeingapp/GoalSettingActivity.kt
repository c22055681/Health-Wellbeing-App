package com.example.healthandwellbeingapp.goals

import android.app.DatePickerDialog
import android.content.Intent
import com.example.healthandwellbeingapp.MainActivity
import com.example.healthandwellbeingapp.LoginActivity
import com.google.android.material.appbar.MaterialToolbar
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthandwellbeingapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GoalSettingActivity : AppCompatActivity() {

    private lateinit var adapter: GoalAdapter
    private val goals = mutableListOf<Goal>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_setting)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.title = "Goal Setting"

// Handle Home (Navigation) icon
        toolbar.setNavigationOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)) // or use finish() to go back
        }

// Handle Logout button in toolbar menu
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

        // Views
        val goalNameInput = findViewById<EditText>(R.id.goalName)
        val startValueInput = findViewById<EditText>(R.id.startValue)
        val goalValueInput = findViewById<EditText>(R.id.goalValue)
        val btnStartDate = findViewById<Button>(R.id.btnStartDate)
        val btnEndDate = findViewById<Button>(R.id.btnEndDate)
        val btnAddGoal = findViewById<Button>(R.id.btnAddGoal)
        val recyclerGoals = findViewById<RecyclerView>(R.id.recyclerGoals)

        // Date logic
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var startDate: Calendar? = null
        var endDate: Calendar? = null

        btnStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                startDate = Calendar.getInstance().apply { set(y, m, d) }
                btnStartDate.text = dateFormatter.format(startDate!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnEndDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                endDate = Calendar.getInstance().apply { set(y, m, d) }
                btnEndDate.text = dateFormatter.format(endDate!!.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Recycler setup
        adapter = GoalAdapter(goals, ::showUpdateDialog, ::confirmDelete)
        recyclerGoals.layoutManager = LinearLayoutManager(this)
        recyclerGoals.adapter = adapter

        // Add goal
        btnAddGoal.setOnClickListener {
            val name = goalNameInput.text.toString()
            val start = startValueInput.text.toString().toFloatOrNull()
            val target = goalValueInput.text.toString().toFloatOrNull()
            val uid = auth.currentUser?.uid ?: return@setOnClickListener

            if (name.isNotEmpty() && start != null && target != null && start < target && startDate != null && endDate != null) {
                val days = ((endDate!!.timeInMillis - startDate!!.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                val daily = if (days > 0) (target - start) / days else 0f

                val goal = Goal(
                    name = name,
                    startValue = start,
                    targetValue = target,
                    currentValue = start,
                    startDate = startDate!!.timeInMillis,
                    endDate = endDate!!.timeInMillis,
                    dailyImprovement = daily
                )

                db.collection("users").document(uid).collection("goals")
                    .add(goal)
                    .addOnSuccessListener {
                        goal.id = it.id
                        goals.add(goal)
                        adapter.notifyItemInserted(goals.lastIndex)
                        Toast.makeText(this, "Goal added!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } else {
                Toast.makeText(this, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
            }
        }

        loadGoals()
    }

    private fun loadGoals() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("goals")
            .get()
            .addOnSuccessListener { documents ->
                goals.clear()
                for (doc in documents) {
                    val goal = doc.toObject(Goal::class.java)
                    goal.id = doc.id
                    goals.add(goal)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load goals.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUpdateDialog(goal: Goal) {
        val input = EditText(this)
        input.hint = "Enter new value"
        AlertDialog.Builder(this)
            .setTitle("Update Goal")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newValue = input.text.toString().toFloatOrNull()
                if (newValue != null && newValue in goal.startValue..goal.targetValue) {
                    goal.currentValue = newValue
                    updateGoal(goal)
                } else {
                    Toast.makeText(this, "Invalid value", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(goal: Goal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                deleteGoal(goal)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateGoal(goal: Goal) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("goals").document(goal.id)
            .set(goal)
            .addOnSuccessListener {
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Goal updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteGoal(goal: Goal) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("goals").document(goal.id)
            .delete()
            .addOnSuccessListener {
                val index = goals.indexOf(goal)
                goals.remove(goal)
                adapter.notifyItemRemoved(index)
                Toast.makeText(this, "Goal deleted", Toast.LENGTH_SHORT).show()
            }
    }
}
