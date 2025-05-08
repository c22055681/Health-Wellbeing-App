package com.example.healthandwellbeingapp

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import android.widget.EditText
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import java.util.*
import android.graphics.Color

class MacroDashboardActivity : AppCompatActivity() {

    private lateinit var macroChart: HorizontalBarChart
    private lateinit var totalCalories: TextView
    private lateinit var totalProtein: TextView
    private lateinit var totalCarbs: TextView
    private lateinit var totalFats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_macro_dashboard)

        // Assign views
        macroChart = findViewById(R.id.macroBarChart)
        totalCalories = findViewById(R.id.totalCalories)
        totalProtein = findViewById(R.id.totalProtein)
        totalCarbs = findViewById(R.id.totalCarbs)
        totalFats = findViewById(R.id.totalFats)

        loadDailyTotalsAndUpdateChart()
        loadTodaysIntake()

        // Back button
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadDailyTotalsAndUpdateChart() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("totals").document("dailyTotals")
            .get()
            .addOnSuccessListener { doc ->
                val dailyCalories = (doc.getLong("caloriesTotal") ?: 0).toFloat()
                val dailyProtein = (doc.getDouble("proteinTotal") ?: 0.0).toFloat()
                val dailyCarbs = (doc.getDouble("carbsTotal") ?: 0.0).toFloat()
                val dailyFats = (doc.getDouble("fatsTotal") ?: 0.0).toFloat()

                updateMacroBarChart(dailyCalories, dailyProtein, dailyCarbs, dailyFats)

                totalCalories.text = "Calories: ${dailyCalories.toInt()} kcal"
                totalProtein.text = "Protein: %.2f g".format(dailyProtein)
                totalCarbs.text = "Carbs: %.2f g".format(dailyCarbs)
                totalFats.text = "Fats: %.2f g".format(dailyFats)
            }
    }

    private fun loadTodaysIntake() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("foodEntries")
            .get()
            .addOnSuccessListener { documents ->
                val selectedFoodsContainer = findViewById<LinearLayout>(R.id.selectedFoodsContainer)
                selectedFoodsContainer.removeAllViews()

                for (doc in documents) {
                    val foodId = doc.id // <--- we need the document ID for editing/deleting
                    val foodName = doc.getString("name") ?: "Unknown"
                    val calories = (doc.getLong("calories") ?: 0).toInt()
                    val protein = (doc.getDouble("protein") ?: 0.0).toFloat()
                    val carbs = (doc.getDouble("carbs") ?: 0.0).toFloat()
                    val fats = (doc.getDouble("fats") ?: 0.0).toFloat()

                    val foodView = TextView(this)
                    foodView.text = "$foodName - ${calories}kcal (P:${protein}g, C:${carbs}g, F:${fats}g)"
                    foodView.textSize = 16f
                    foodView.setTextColor(Color.WHITE)

                    foodView.setOnClickListener {
                        showEditDeleteDialog(foodId, foodName, calories, protein, carbs, fats)
                    }

                    selectedFoodsContainer.addView(foodView)
                }
            }
    }

    private fun showEditDeleteDialog(foodId: String, name: String, calories: Int, protein: Float, carbs: Float, fats: Float) {
        val options = arrayOf("Edit", "Delete")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(name)
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> showEditFoodDialog(foodId, name, calories, protein, carbs, fats)
                1 -> deleteFoodEntry(foodId)
            }
        }
        builder.show()
    }
    private fun showEditFoodDialog(foodId: String, name: String, calories: Int, protein: Float, carbs: Float, fats: Float) {
        val input = EditText(this)
        input.hint = "Enter new grams (e.g., 150)"
        input.inputType = InputType.TYPE_CLASS_NUMBER
        AlertDialog.Builder(this)
            .setTitle("Edit $name")
            .setView(input)
            .setPositiveButton("Update") { dialog, _ ->
                val grams = input.text.toString().toFloatOrNull()
                if (grams != null) {
                    val factor = grams / 100
                    val newCalories = (calories * factor).toInt()
                    val newProtein = protein * factor
                    val newCarbs = carbs * factor
                    val newFats = fats * factor
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setPositiveButton
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId)
                        .collection("dailyIntake").document(date)
                        .collection("foodEntries").document(foodId)
                        .set(mapOf("name" to name, "calories" to newCalories, "protein" to newProtein, "carbs" to newCarbs, "fats" to newFats))
                        .addOnSuccessListener {
                            loadTodaysIntake()
                            loadDailyTotalsAndUpdateChart()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFoodEntry(foodId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("foodEntries").document(foodId)
            .delete()
            .addOnSuccessListener {
                loadTodaysIntake()
                loadDailyTotalsAndUpdateChart()
            }
    }


    private fun updateMacroBarChart(calories: Float, protein: Float, carbs: Float, fats: Float) {
        val labels = listOf("Calories", "Protein", "Carbs", "Fats")
        val actualValues = listOf(calories, protein, carbs, fats)

        val entries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()

        for (i in labels.indices) {
            entries.add(BarEntry(i.toFloat(), actualValues[i]))
            colors.add(Color.BLUE)
        }

        val dataSet = BarDataSet(entries, "Macro Progress")
        dataSet.colors = colors
        val data = BarData(dataSet)
        data.barWidth = 0.5f

        macroChart.data = data
        macroChart.description.isEnabled = false
        macroChart.legend.isEnabled = false
        macroChart.axisRight.isEnabled = false

        val xAxis = macroChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textSize = 12f

        macroChart.invalidate()
    }
}
