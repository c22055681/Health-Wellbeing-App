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



        setupSplitDialog(R.id.btnShouldersSplit, "Shoulders Workout Split", "Shoulder Press, Lateral Raise, Front Raise...")
        setupSplitDialog(R.id.btnLegsSplit, "Legs Workout Split", "Squats, Leg Press, Lunges...")
        setupSplitDialog(R.id.btnBackBicepsSplit, "Back & Biceps Split", "Deadlifts, Rows, Bicep Curls...")
        setupSplitDialog(R.id.btnChestTricepsSplit, "Chest & Triceps Split", "Bench Press, Dips, Tricep Extensions...")
    }

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
