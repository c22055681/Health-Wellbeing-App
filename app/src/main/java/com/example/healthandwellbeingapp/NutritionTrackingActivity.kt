package com.example.healthandwellbeingapp

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.healthandwellbeingapp.api.RetrofitClient
import kotlinx.coroutines.*
import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class NutritionTrackingActivity : AppCompatActivity() {

    private lateinit var resultsContainer: LinearLayout

    private val apiKey = "43a21b3738014f0eaccd1d509f11d108"
    private val apiSecret = "4d4ffdb2bc5a4cbcb2f439caa4198a4b"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nutrition_tracking)

        val editSearchFood = findViewById<EditText>(R.id.editSearchFood)
        val btnSearch = findViewById<Button>(R.id.btnSearch)
        resultsContainer = findViewById(R.id.resultsContainer)

        btnSearch.setOnClickListener {
            val query = editSearchFood.text.toString().trim()
            if (query.isNotEmpty()) {
                searchFood(query)
            }
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

        view.findViewById<TextView>(R.id.foodName).text = result.name
        view.findViewById<TextView>(R.id.foodCalories).text = "${result.calories} kcal per 100g"
        view.findViewById<TextView>(R.id.foodMacros).text =
            "P: ${result.protein}g  C: ${result.carbs}g  F: ${result.fats}g"

        resultsContainer.addView(view)
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

        val paramString = params.entries.joinToString("&") {
            "${encode(it.key)}=${encode(it.value)}"
        }

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
