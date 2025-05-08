package com.example.healthandwellbeingapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class WeightliftingActivity : AppCompatActivity() {

    private lateinit var videoShoulders: VideoView
    private lateinit var videoLegs: VideoView
    private lateinit var videoBack: VideoView
    private lateinit var videoChest: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weightlifting)


        videoShoulders = findViewById(R.id.videoShoulders)
        videoLegs = findViewById(R.id.videoLegs)
        videoBack = findViewById(R.id.videoBack)
        videoChest = findViewById(R.id.videoChest)

        // Setup workout dialogs for each split
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

        // Setup videos for each VideoView
        setupVideo(videoShoulders, R.raw.legs_video)
        setupVideo(videoLegs, R.raw.chest_video)
        setupVideo(videoBack, R.raw.back_video)
        setupVideo(videoChest, R.raw.legs_video)
    }

    private fun setupVideo(videoView: VideoView, videoResource: Int) {
        val uri = Uri.parse("android.resource://${packageName}/$videoResource")
        videoView.setVideoURI(uri)

        // Add MediaControls
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true

        }
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

    override fun onDestroy() {
        super.onDestroy()
        videoShoulders.stopPlayback()
        videoLegs.stopPlayback()
        videoBack.stopPlayback()
        videoChest.stopPlayback()
    }
}