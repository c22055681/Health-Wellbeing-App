package com.example.healthandwellbeingapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WeightliftingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weightlifting)

        // Setup detailed workout dialogs for each split
        setupSplitDialog(
            R.id.btnShouldersSplit,
            "Shoulders Workout Split",
            """
            🔸 Shoulder Press – 4 sets of 10 reps
            🔸 Lateral Raises – 3 sets of 12 reps
            🔸 Front Raises – 3 sets of 12 reps
            🔸 Rear Delt Fly – 3 sets of 15 reps
            🔸 Shrugs – 4 sets of 12 reps
            """.trimIndent()
        )

        setupSplitDialog(
            R.id.btnLegsSplit,
            "Legs Workout Split",
            """
            🔸 Squats – 4 sets of 8 reps
            🔸 Leg Press – 4 sets of 12 reps
            🔸 Walking Lunges – 3 sets (each leg)
            🔸 Leg Curls – 3 sets of 15 reps
            🔸 Calf Raises – 4 sets of 20 reps
            """.trimIndent()
        )

        setupSplitDialog(
            R.id.btnBackSplit,
            "Back Workout Split",
            """
            🔸 Deadlifts – 4 sets of 6-8 reps
            🔸 Lat Pulldowns – 4 sets of 10 reps
            🔸 Barbell Rows – 3 sets of 12 reps
            🔸 Seated Cable Rows – 3 sets of 15 reps
            🔸 Face Pulls – 3 sets of 20 reps
            """.trimIndent()
        )

        setupSplitDialog(
            R.id.btnChestSplit,
            "Chest Workout Split",
            """
            🔸 Bench Press – 4 sets of 8-10 reps
            🔸 Incline Dumbbell Press – 3 sets of 10 reps
            🔸 Chest Fly – 3 sets of 12 reps
            🔸 Dips – 3 sets to failure
            🔸 Tricep Pushdowns – 3 sets of 15 reps
            """.trimIndent()
        )
    }

    // If using local videos, call this with your video resources
    private fun setupVideo(videoViewId: Int, videoResource: Int) {
        val videoView = findViewById<VideoView>(videoViewId)
        val uri = Uri.parse("android.resource://${packageName}/$videoResource")
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener { it.isLooping = true }
        videoView.start()
    }

    private fun setupSplitDialog(buttonId: Int, title: String, message: String) {
        findViewById<Button>(buttonId).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Close", null)
                .show()
        }
    }
}
