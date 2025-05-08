package com.example.healthandwellbeingapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.healthandwellbeingapp.api.RetrofitClient
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class NutritionTrackingActivity : AppCompatActivity() {

    private lateinit var resultsContainer: LinearLayout
    private lateinit var goalCaloriesET: EditText
    private lateinit var goalProteinET: EditText
    private lateinit var goalCarbsET: EditText
    private lateinit var goalFatsET: EditText

    private var dailyCalories = 0f
    private var dailyProtein = 0f
    private var dailyCarbs = 0f
    private var dailyFats = 0f

    private val apiKey = "43a21b3738014f0eaccd1d509f11d108"
    private val apiSecret = "4d4ffdb2bc5a4cbcb2f439caa4198a4b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutrition_tracking)

        val btnViewDashboard = findViewById<Button>(R.id.btnViewDashboard)
        btnViewDashboard.setOnClickListener {
            startActivity(Intent(this, MacroDashboardActivity::class.java))
        }

        resultsContainer = findViewById(R.id.resultsContainer)

        goalCaloriesET = findViewById(R.id.goalCalories)
        goalProteinET = findViewById(R.id.goalProtein)
        goalCarbsET = findViewById(R.id.goalCarbs)
        goalFatsET = findViewById(R.id.goalFats)

        val prefs = getSharedPreferences("DailyGoalsPrefs", MODE_PRIVATE)
        goalCaloriesET.setText(prefs.getString("goalCalories", ""))
        goalProteinET.setText(prefs.getString("goalProtein", ""))
        goalCarbsET.setText(prefs.getString("goalCarbs", ""))
        goalFatsET.setText(prefs.getString("goalFats", ""))

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            val query = findViewById<EditText>(R.id.editSearchFood).text.toString().trim()
            if (query.isNotEmpty()) searchFood(query)
        }

        goalCaloriesET.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateMacroBarChart() }
        goalProteinET.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateMacroBarChart() }
        goalCarbsET.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateMacroBarChart() }
        goalFatsET.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) updateMacroBarChart() }
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("DailyGoalsPrefs", MODE_PRIVATE)
        with(prefs.edit()) {
            putString("goalCalories", goalCaloriesET.text.toString())
            putString("goalProtein", goalProteinET.text.toString())
            putString("goalCarbs", goalCarbsET.text.toString())
            putString("goalFats", goalFatsET.text.toString())
            apply()
        }
    }
    private fun updateDailyTotals(result: NutritionResult) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()
        val totalsRef = db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("totals").document("dailyTotals")

        db.runTransaction { transaction ->
            val snapshot = transaction.get(totalsRef)
            val newCalories = (snapshot.getLong("caloriesTotal") ?: 0) + result.calories
            val newProtein = (snapshot.getDouble("proteinTotal") ?: 0.0) + result.protein
            val newCarbs = (snapshot.getDouble("carbsTotal") ?: 0.0) + result.carbs
            val newFats = (snapshot.getDouble("fatsTotal") ?: 0.0) + result.fats

            transaction.set(totalsRef, mapOf(
                "caloriesTotal" to newCalories,
                "proteinTotal" to newProtein,
                "carbsTotal" to newCarbs,
                "fatsTotal" to newFats
            ))
        }
    }


    private fun searchFood(query: String) {
        resultsContainer.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = (System.currentTimeMillis() / 1000).toString()
                val nonce = UUID.randomUUID().toString().replace("-", "")
                val signature = generateOAuthSignature(query, timestamp, nonce)

                val response = RetrofitClient.apiService.searchFood(
                    query = query,
                    apiKey = apiKey,
                    timestamp = timestamp,
                    nonce = nonce,
                    signature = signature
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.foods?.food != null) {
                        val foods = response.body()!!.foods.food
                        foods.forEach { food ->
                            val nutritionResult = NutritionResult(
                                name = food.food_name,
                                calories = food.food_description.extractCalories(),
                                protein = food.food_description.extractProtein(),
                                carbs = food.food_description.extractCarbs(),
                                fats = food.food_description.extractFat()
                            )
                            addResultView(nutritionResult)
                        }
                    } else {
                        Toast.makeText(this@NutritionTrackingActivity, "No results found.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NutritionTrackingActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addResultView(result: NutritionResult) {
        val view = layoutInflater.inflate(R.layout.nutrition_result_item, resultsContainer, false)

        val foodNameTextView = view.findViewById<TextView>(R.id.foodName)
        val foodCaloriesTextView = view.findViewById<TextView>(R.id.foodCalories)
        val foodMacrosTextView = view.findViewById<TextView>(R.id.foodMacros)

        foodNameTextView.text = result.name
        foodCaloriesTextView.text = "${result.calories} kcal per 100g"
        foodMacrosTextView.text = "P: ${result.protein}g  C: ${result.carbs}g  F: ${result.fats}g"

        foodNameTextView.setTextColor(Color.WHITE)
        foodCaloriesTextView.setTextColor(Color.WHITE)
        foodMacrosTextView.setTextColor(Color.WHITE)

        view.setOnClickListener {
            showGramInputDialog(result)
        }

        resultsContainer.addView(view)
    }

    private fun showGramInputDialog(result: NutritionResult) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter gram amount")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "e.g., 150"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val grams = input.text.toString().toFloatOrNull() ?: 100f
            val scaledResult = NutritionResult(
                name = result.name,
                calories = (result.calories * grams / 100).toInt(),
                protein = result.protein * grams / 100,
                carbs = result.carbs * grams / 100,
                fats = result.fats * grams / 100
            )
            addFoodToFirestore(scaledResult)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun addFoodToFirestore(result: NutritionResult) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        val foodEntry = hashMapOf(
            "name" to result.name,
            "calories" to result.calories,
            "protein" to result.protein,
            "carbs" to result.carbs,
            "fats" to result.fats
        )

        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("foodEntries").document()
            .set(foodEntry)
            .addOnSuccessListener {
                Toast.makeText(this, "${result.name} logged!", Toast.LENGTH_SHORT).show()
                updateDailyTotals(result)
                loadLoggedFoodsAndTotals()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMacroBarChart() {
        val goalCaloriesVal = goalCaloriesET.text.toString().toFloatOrNull() ?: 1f
        val goalProteinVal = goalProteinET.text.toString().toFloatOrNull() ?: 1f
        val goalCarbsVal = goalCarbsET.text.toString().toFloatOrNull() ?: 1f
        val goalFatsVal = goalFatsET.text.toString().toFloatOrNull() ?: 1f

        val labels = listOf("Calories", "Protein", "Carbs", "Fat")
        val actualValues = listOf(dailyCalories, dailyProtein, dailyCarbs, dailyFats)
        val goalValues = listOf(goalCaloriesVal, goalProteinVal, goalCarbsVal, goalFatsVal)

        val entries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()

        for (i in labels.indices) {
            val ratio = (actualValues[i] / goalValues[i]) * 100f
            entries.add(BarEntry(i.toFloat(), ratio))
            colors.add(
                when {
                    ratio < 50f -> Color.RED
                    ratio < 80f -> Color.parseColor("#FFA500")
                    else -> Color.GREEN
                }
            )
        }

        val dataSet = BarDataSet(entries, "Macro Progress")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String = "${value.toInt()}%"
        }

        val macroChart = findViewById<HorizontalBarChart>(R.id.macroBarChart)
        macroChart.data = BarData(dataSet)
        macroChart.barData.barWidth = 0.5f
        macroChart.description.isEnabled = false
        macroChart.legend.isEnabled = false
        macroChart.axisRight.isEnabled = false
        macroChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textSize = 12f
        }
        macroChart.axisLeft.isEnabled = false
        macroChart.invalidate()
    }

    private fun generateOAuthSignature(query: String, timestamp: String, nonce: String): String {
        val method = "GET"
        val baseUrl = "https://platform.fatsecret.com/rest/server.api"
        val params = sortedMapOf(
            "format" to "json",
            "method" to "foods.search",
            "oauth_consumer_key" to apiKey,
            "oauth_nonce" to nonce,
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to timestamp,
            "oauth_version" to "1.0",
            "search_expression" to query
        )
        val paramString = params.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" }
        val signatureBase = "$method&${encode(baseUrl)}&${encode(paramString)}"
        val signingKey = "${encode(apiSecret)}&"
        return calculateRFC2104HMAC(signatureBase, signingKey)
    }

    private fun calculateRFC2104HMAC(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return Base64.encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20").replace("*", "%2A").replace("%7E", "~")

    data class NutritionResult(
        val name: String,
        val calories: Int,
        val protein: Float,
        val carbs: Float,
        val fats: Float
    )

    private fun loadLoggedFoodsAndTotals() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("totals").document("dailyTotals")
            .get()
            .addOnSuccessListener { doc ->
                val caloriesTotal = doc.getLong("caloriesTotal") ?: 0
                val proteinTotal = doc.getDouble("proteinTotal") ?: 0.0
                val carbsTotal = doc.getDouble("carbsTotal") ?: 0.0
                val fatsTotal = doc.getDouble("fatsTotal") ?: 0.0

                // Update your internal variables so your chart updates
                dailyCalories = caloriesTotal.toFloat()
                dailyProtein = proteinTotal.toFloat()
                dailyCarbs = carbsTotal.toFloat()
                dailyFats = fatsTotal.toFloat()

                updateMacroBarChart()
            }
    }

    private fun String.extractCalories(): Int {
        return Regex("Calories: (\\d+)kcal", RegexOption.IGNORE_CASE)
            .find(this)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun String.extractProtein(): Float {
        return Regex("Protein: ([\\d.]+)g", RegexOption.IGNORE_CASE)
            .find(this)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    private fun String.extractCarbs(): Float {
        return Regex("Carbs: ([\\d.]+)g", RegexOption.IGNORE_CASE)
            .find(this)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    private fun String.extractFat(): Float {
        return Regex("Fat: ([\\d.]+)g", RegexOption.IGNORE_CASE)
            .find(this)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }
}
