package com.example.myFitHololenzApp

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myFitHololenzApp.databinding.FragmentFirstBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var playerGoals: PlayerGoals
    private val binding get() = _binding!!
    private val TAG = "server"

    private var lastStepCount = 0
    private var lastTimestamp = System.currentTimeMillis()
    private var totalSteps = 0

    private lateinit var dataLogger: DataLogger

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.i(TAG, "Fragment created here")
        val walkingDog = binding.walkingDog
        val theDogAnimation = walkingDog.background as AnimationDrawable
        theDogAnimation.start()

        dataLogger = DataLogger(requireContext(), "fitness_data_log.txt")

        // Request Google Fit permissions
        val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
           accessGoogleFitData(account)
        }

        // Example usage: Assume the user enters a total step goal of 3000
        val stepGoal = activity?.intent?.getIntExtra("STEP_GOAL", 0) ?: 0
        playerGoals = PlayerGoals(stepGoal)
        Log.i("PlayerGoals", "step goal in frag: $stepGoal")

        // Get steps required for each activity level
        val briskWalkingSteps = playerGoals.getStepsRequiredForLevel(ActivityLevel.BriskWalking)
        val joggingSteps = playerGoals.getStepsRequiredForLevel(ActivityLevel.Jogging)
        val runningSteps = playerGoals.getStepsRequiredForLevel(ActivityLevel.Running)

        // Optionally, log or display these values
        Log.i("PlayerGoals", "Brisk Walking Steps: $briskWalkingSteps")
        Log.i("PlayerGoals", "Jogging Steps: $joggingSteps")
        Log.i("PlayerGoals", "Running Steps: $runningSteps")

        updateLevelStatusUI(totalSteps)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
            val account = GoogleSignIn.getAccountForExtension(requireContext(), fitnessOptions)
            if (resultCode == Activity.RESULT_OK) {
                accessGoogleFitData(account)
            } else {
                Log.i(TAG, "Permission not granted.")
            }
        }
    }

    private fun accessGoogleFitData(account: GoogleSignInAccount) {
        // Register listener for step count

        val stepCountListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val currentStepCount = dataPoint.getValue(field).asInt()
                val currentTimestamp = System.currentTimeMillis()
                binding.textStepCount.text = "${currentStepCount.toInt()}"

                // If this is the first step count reading, store it as the base value to subtract later
                if (lastStepCount == 0) {
                    lastStepCount = currentStepCount // This becomes your baseline
                    lastTimestamp = currentTimestamp
                    Log.i(TAG, "Initial step count: $currentStepCount")
                    return@OnDataPointListener
                }

                // Calculate step difference relative to the first reading
                val stepDifference = currentStepCount - lastStepCount // Adjusted for the first reading
                val timeDifference = (currentTimestamp - lastTimestamp) / 1000.0 // in seconds

                if (stepDifference > 0) {
                    totalSteps += stepDifference
                    Log.i(TAG, "step diff: $stepDifference, total: $totalSteps")
                }

                val cadence = if (timeDifference > 0) (stepDifference / (timeDifference / 60.0)) else 0.0

                // Update UI with cadence
                updateCadenceUI(cadence)
                binding.textStepCount.text = "${totalSteps.toInt()}"
                logFitnessData(totalSteps, cadence)
                updateLevelStatusUI(totalSteps)

                lastStepCount = currentStepCount
                lastTimestamp = currentTimestamp
            }
        }

        val sensorRequest = SensorRequest.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setSamplingRate(1, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(requireContext(), account)
            .add(sensorRequest, stepCountListener)
            .addOnSuccessListener {
                Log.i(TAG, "Step count listener registered")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register step count listener", e)
            }
    }

    private fun updateLevelStatusUI(totalSteps: Int) {
        // Check the current level based on totalSteps and update UI
        val currentLevel = playerGoals.getCurrentLevel(totalSteps)

        when (currentLevel) {
            ActivityLevel.BriskWalking -> {
                binding.textHeartRateLabel.text = "Brisk Walking Level "
                binding.textHeartRate.text = "${totalSteps.toInt()} "
            }
            ActivityLevel.Jogging -> {
                binding.textHeartRateLabel.text = "Jogging  level "
                binding.textHeartRate.text = "${totalSteps.toInt()} "
            }
            ActivityLevel.Running -> {
                binding.textHeartRateLabel.text = "Running  level"
                binding.textHeartRate.text = "${totalSteps.toInt()} "
            }
        }
    }
    private fun updateCadenceUI(cadence: Double) {
        val MAX_CADENCE = 200.0
        val progress = (cadence * 100 / MAX_CADENCE).toInt()
        binding.progressBarCadence.progress = progress
        binding.textCadenceValue.text = "${cadence.toInt()} steps/min"
    }
    private fun logFitnessData(stepCount: Int, cadence: Double) {
        val logEntry = "Time: ${System.currentTimeMillis()}, Steps: $stepCount, Cadence: $cadence"
        dataLogger.logData(logEntry)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1001
    }
}

