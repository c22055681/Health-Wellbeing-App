package com.example.healthandwellbeingapp

import android.os.Bundle
import android.text.InputType
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
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import android.graphics.Color

class NutritionTrackingActivity : AppCompatActivity() {

    private lateinit var resultsContainer: LinearLayout
    private lateinit var selectedFoodsContainer: LinearLayout

    // New member variables for daily totals
    private var dailyCalories = 0f
    private var dailyProtein = 0f
    private var dailyCarbs = 0f
    private var dailyFats = 0f

    // Hold references to goal inputs and chart for later updates
    private lateinit var goalCaloriesET: EditText
    private lateinit var goalProteinET: EditText
    private lateinit var goalCarbsET: EditText
    private lateinit var goalFatsET: EditText
    private lateinit var macroChart: HorizontalBarChart

    private val apiKey = "43a21b3738014f0eaccd1d509f11d108"
    private val apiSecret = "4d4ffdb2bc5a4cbcb2f439caa4198a4b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutrition_tracking)

        val editSearchFood = findViewById<EditText>(R.id.editSearchFood)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        resultsContainer = findViewById(R.id.resultsContainer)
        selectedFoodsContainer = findViewById(R.id.selectedFoodsContainer)

        // Initialize goal EditTexts and the chart
        goalCaloriesET = findViewById(R.id.goalCalories)
        goalProteinET = findViewById(R.id.goalProtein)
        goalCarbsET = findViewById(R.id.goalCarbs)
        goalFatsET = findViewById(R.id.goalFats)
        macroChart = findViewById(R.id.macroBarChart)

        val prefs = getSharedPreferences("DailyGoalsPrefs", MODE_PRIVATE)
        goalCaloriesET.setText(prefs.getString("goalCalories", ""))
        goalProteinET.setText(prefs.getString("goalProtein", ""))
        goalCarbsET.setText(prefs.getString("goalCarbs", ""))
        goalFatsET.setText(prefs.getString("goalFats", ""))

        updateMacroBarChart() // refreshes the chart with the stored values

        btnSearch.setOnClickListener {
            val query = editSearchFood.text.toString().trim()
            if (query.isNotEmpty()) {
                searchFood(query)
            }
        }

        // Load food entries and totals from Firestore
        loadLoggedFoodsAndTotals()

        // Update chart on focus change
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
        // Inflate the custom item layout for search results
        val view = layoutInflater.inflate(R.layout.nutrition_result_item, resultsContainer, false)
        view.findViewById<TextView>(R.id.foodName).text = result.name
        view.findViewById<TextView>(R.id.foodCalories).text = "${result.calories} kcal per 100g"
        view.findViewById<TextView>(R.id.foodMacros).text =
            "P: ${result.protein}g  C: ${result.carbs}g  F: ${result.fats}g"

        // On click, show a dialog to let the user specify a gram amount
        view.setOnClickListener {
            showGramInputDialog(result)
        }
        resultsContainer.addView(view)
    }

    private fun showGramInputDialog(result: NutritionResult) {
        val builder = AlertDialog.Builder(this@NutritionTrackingActivity)
        builder.setTitle("Enter gram amount")

        val input = EditText(this@NutritionTrackingActivity)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "e.g., 150"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val grams = input.text.toString().toFloatOrNull() ?: 100f
            val factor = grams / 100f

            val scaledResult = NutritionResult(
                name = result.name,
                calories = (result.calories * factor).toInt(),
                protein = result.protein * factor,
                carbs = result.carbs * factor,
                fats = result.fats * factor
            )

            // Add to Firestore and show in Today's Intake
            addFoodToFirestore(scaledResult)
            addSelectedFoodView(scaledResult)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun addSelectedFoodView(result: NutritionResult) {
        // Inflate the same layout for selected (logged) food items
        val view = layoutInflater.inflate(R.layout.nutrition_result_item, selectedFoodsContainer, false)
        view.findViewById<TextView>(R.id.foodName).text = result.name
        view.findViewById<TextView>(R.id.foodCalories).text = "${result.calories} kcal"
        view.findViewById<TextView>(R.id.foodMacros).text =
            "P: ${result.protein}g  C: ${result.carbs}g  F: ${result.fats}g"

        selectedFoodsContainer.addView(view)
    }

    private fun loadLoggedFoodsAndTotals() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val db = FirebaseFirestore.getInstance()

        // Load Food Entries
        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("foodEntries")
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val nutritionResult = NutritionResult(
                        name = doc.getString("name") ?: "",
                        calories = (doc.getLong("calories") ?: 0L).toInt(),
                        protein = (doc.getDouble("protein") ?: 0.0).toFloat(),
                        carbs = (doc.getDouble("carbs") ?: 0.0).toFloat(),
                        fats = (doc.getDouble("fats") ?: 0.0).toFloat()
                    )
                    // Show these previously logged items under "Today's Intake"
                    addSelectedFoodView(nutritionResult)
                }
            }

        // Load Totals
        db.collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("totals").document("dailyTotals")
            .get()
            .addOnSuccessListener { doc ->
                val caloriesTotal = doc.getLong("caloriesTotal") ?: 0
                val proteinTotal = doc.getDouble("proteinTotal") ?: 0.0
                val carbsTotal = doc.getDouble("carbsTotal") ?: 0.0
                val fatsTotal = doc.getDouble("fatsTotal") ?: 0.0

                findViewById<TextView>(R.id.totalCalories).text = "Calories: $caloriesTotal kcal"
                findViewById<TextView>(R.id.totalProtein).text = "Protein: ${"%.2f".format(proteinTotal)} g"
                findViewById<TextView>(R.id.totalCarbs).text = "Carbs: ${"%.2f".format(carbsTotal)} g"
                findViewById<TextView>(R.id.totalFats).text = "Fats: ${"%.2f".format(fatsTotal)} g"

                // Store these totals for chart update
                dailyCalories = caloriesTotal.toFloat()
                dailyProtein = proteinTotal.toFloat()
                dailyCarbs = carbsTotal.toFloat()
                dailyFats = fatsTotal.toFloat()

                // Update the chart
                updateMacroBarChart()
            }
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
            .set(foodEntry).addOnSuccessListener {
                Toast.makeText(this, "${result.name} logged!", Toast.LENGTH_SHORT).show()
                updateDailyTotals(result)
            }.addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDailyTotals(result: NutritionResult) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val totalsRef = FirebaseFirestore.getInstance().collection("users").document(userId)
            .collection("dailyIntake").document(date)
            .collection("totals").document("dailyTotals")

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(totalsRef)
            val updatedTotals = mapOf(
                "caloriesTotal" to (snapshot.getLong("caloriesTotal") ?: 0) + result.calories,
                "proteinTotal" to (snapshot.getDouble("proteinTotal") ?: 0.0) + result.protein,
                "carbsTotal" to (snapshot.getDouble("carbsTotal") ?: 0.0) + result.carbs,
                "fatsTotal" to (snapshot.getDouble("fatsTotal") ?: 0.0) + result.fats
            )
            transaction.set(totalsRef, updatedTotals, SetOptions.merge())
        }
    }

    private fun updateMacroBarChart() {
        // Parse user goal inputs; default to 1f if empty (to avoid division by zero)
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
            val actual = actualValues[i]
            val goal = goalValues[i]
            val ratio = (actual / goal) * 100f
            entries.add(BarEntry(i.toFloat(), ratio))
            val color = when {
                ratio < 50f -> Color.RED
                ratio < 80f -> Color.parseColor("#FFA500")
                else -> Color.GREEN
            }
            colors.add(color)
        }

        val dataSet = BarDataSet(entries, "Macro Progress")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()}%"
            }
        }
        val data = BarData(dataSet)
        data.barWidth = 0.5f

        macroChart.data = data
        macroChart.description.isEnabled = false
        macroChart.legend.isEnabled = false
        macroChart.setDrawGridBackground(false)
        macroChart.setDrawValueAboveBar(true)
        macroChart.axisRight.isEnabled = false

        val xAxis = macroChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
        xAxis.labelCount = labels.size
        xAxis.textSize = 12f

        val leftAxis = macroChart.axisLeft
        leftAxis.axisMinimum = 0f
        leftAxis.axisMaximum = 150f
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.isEnabled = false

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
        val algorithm = "HmacSHA1"
        val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)
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
