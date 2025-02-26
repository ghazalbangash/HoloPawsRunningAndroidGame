package com.example.myFitHololenzApp

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.TimeUnit

class NewService : Service() {

    companion object {
        private const val TAG = "FitnessService"
        private const val SERVER_IP = "192.168.175.186"
        private const val SERVER_PORT = 9090
        private const val MAX_BUFFER_SIZE = 5
        private const val MIN_CADENCE = 30.0
        private const val MAX_CADENCE = 200.0
        private const val SMOOTHING_FACTOR = 0.5
    }

    // Fitness data tracking
    private val cadenceBuffer = ArrayDeque<Double>()
    private lateinit var dataLogger: DataLogger

    // Google Fit Options and Listeners
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_HEART_RATE_BPM)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .build()
    private var heartRateListener: OnDataPointListener? = null
    private var stepCountListener: OnDataPointListener? = null

    // Step tracking
    private var lastStepCount = 0
    private var totalSteps = 0
    private var lastTimestamp = System.currentTimeMillis()

    // User data
    private var steps: String? = null
    private var age: String? = null
    private var height: String? = null
    private var weight: String? = null
    private var userInputsSent = false

    // Network
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        if (!checkPermissions()) {
            Log.e(TAG, "Necessary permissions not granted")
            return
        }

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        registerHeartRateListener(account)
        registerStepCountListener(account)
        accessGoogleFit(account)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        dataLogger = DataLogger(this, "fitness_data_log1.txt")

        if (intent == null) {
            Log.e(TAG, "Intent is null in onStartCommand")
            return START_NOT_STICKY
        }

        // Retrieve user inputs from the Intent
        extractUserDataFromIntent(intent)

        return START_NOT_STICKY
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterListeners()
        closeConnection()
    }

    private fun checkPermissions(): Boolean {
        return !(ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    }

    private fun extractUserDataFromIntent(intent: Intent) {
        steps = intent.getStringExtra("steps")
        age = intent.getStringExtra("age")
        height = intent.getStringExtra("height")
        weight = intent.getStringExtra("weight")

        Log.i(TAG, "Received data - Steps: $steps, Age: $age, Height: $height, Weight: $weight")

        if (steps.isNullOrEmpty() || age.isNullOrEmpty() || height.isNullOrEmpty() || weight.isNullOrEmpty()) {
            Log.e(TAG, "One or more data fields are null or empty")
        } else {
            Log.i(TAG, "Data received successfully")
        }
    }

    private fun logFitnessData(stepCount: Int, cadence: Double) {
        val logEntry = "Time: ${System.currentTimeMillis()}, Steps: $stepCount, Cadence: $cadence"
        dataLogger.logData(logEntry)
    }

    private fun smoothCadence(newCadence: Double): Double {
        if (cadenceBuffer.size == MAX_BUFFER_SIZE) {
            cadenceBuffer.removeFirst() // Remove the oldest value
        }
        cadenceBuffer.addLast(newCadence)
        return cadenceBuffer.average() // Compute average of the buffer
    }

    private fun registerHeartRateListener(account: GoogleSignInAccount) {
        Fitness.getRecordingClient(this, account)
            .subscribe(DataType.TYPE_HEART_RATE_BPM)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed to heart rate!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing to heart rate.", e)
            }

        heartRateListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field).asFloat()
                Log.i(TAG, "Detected heart rate: $value")
            }
        }

        val sensorRequest = SensorRequest.Builder()
            .setDataType(DataType.TYPE_HEART_RATE_BPM)
            .setSamplingRate(1, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(this, account)
            .add(sensorRequest, heartRateListener!!)
            .addOnSuccessListener {
                Log.i(TAG, "Heart rate listener registered")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register heart rate listener", e)
            }
    }

    private fun registerStepCountListener(account: GoogleSignInAccount) {
        stepCountListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                processStepCount(dataPoint.getValue(field).asInt())
            }
        }

        val sensorRequest = SensorRequest.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setSamplingRate(1, TimeUnit.SECONDS)
            .build()

        Fitness.getSensorsClient(this, account)
            .add(sensorRequest, stepCountListener!!)
            .addOnSuccessListener {
                Log.i(TAG, "Step count listener registered")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to register step count listener", e)
            }
    }

    private fun processStepCount(currentStepCount: Int) {
        val currentTimestamp = System.currentTimeMillis()

        // Initialize step count if needed
        if (lastStepCount == 0) {
            lastStepCount = currentStepCount
            lastTimestamp = currentTimestamp
            Log.i(TAG, "Initial step count: $currentStepCount")
            return
        }

        // Calculate step difference
        val stepDifference = currentStepCount - lastStepCount

        // Update total steps only if step difference is positive
        if (stepDifference > 0) {
            totalSteps += stepDifference
            Log.i(TAG, "Step difference: $stepDifference, Total steps: $totalSteps")
        }

        // Update cadence
        val rawCadence = calculateCadence(currentStepCount, currentTimestamp)
        val smoothedCadence = smoothCadence(rawCadence)

        Log.i(TAG, "Current step count: $currentStepCount, Smoothed Cadence: $smoothedCadence steps/min")

        // Update last values for next iteration
        lastStepCount = currentStepCount
        lastTimestamp = currentTimestamp

        // Log and send data
        logFitnessData(totalSteps, rawCadence)
        sendData(totalSteps, smoothedCadence)
    }

    private fun calculateCadence(currentStepCount: Int, currentTimestamp: Long): Double {
        val stepDifference = currentStepCount - lastStepCount
        val timeDifference = (currentTimestamp - lastTimestamp) / 1000.0 // in seconds

        // Skip invalid values
        if (stepDifference <= 0 || timeDifference <= 0) {
            return 0.0
        }

        // Handle step anomalies
        if (stepDifference > 50) {  // Adjust threshold as needed
            Log.e(TAG, "Unusual step difference: $stepDifference")
            return 0.0
        }

        // Calculate cadence (steps per minute)
        val cadence = (stepDifference / (timeDifference / 60.0))

        // Apply exponential moving average
        val smoothedCadence = if (cadenceBuffer.isNotEmpty()) {
            SMOOTHING_FACTOR * cadence + (1 - SMOOTHING_FACTOR) * cadenceBuffer.last()
        } else {
            cadence
        }

        // Filter out unlikely values
        return if (smoothedCadence in MIN_CADENCE..MAX_CADENCE) smoothedCadence else 0.0
    }

    private fun unregisterListeners() {
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        heartRateListener?.let {
            Fitness.getSensorsClient(this, account).remove(it)
                .addOnSuccessListener {
                    Log.i(TAG, "Heart rate listener unregistered")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unregister heart rate listener", e)
                }
        }

        stepCountListener?.let {
            Fitness.getSensorsClient(this, account).remove(it)
                .addOnSuccessListener {
                    Log.i(TAG, "Step count listener unregistered")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to unregister step count listener", e)
                }
        }
    }

    private fun accessGoogleFit(account: GoogleSignInAccount) {
        Fitness.getRecordingClient(this, account)
            .subscribe(DataType.TYPE_HEART_RATE_BPM)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed to heart rate!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing to heart rate.", e)
            }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.HOURS.toMillis(1)  // Last hour

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                Log.i(TAG, "Data read successfully: ${dataReadResponse.dataSets.size} datasets")
                dataReadResponse.dataSets.forEach { processDataSet(it) }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to read data", e)
            }
    }

    private fun processDataSet(dataSet: DataSet) {
        for (dp in dataSet.dataPoints) {
            for (field in dp.dataType.fields) {
                val value = dp.getValue(field).asFloat()
                Log.i(TAG, "Historical heart rate: $value")
            }
        }
    }

    private fun sendData(stepCount: Int, cadence: Double) {
        Log.i(TAG, "Sending: Steps: $steps, Age: $age, Height: $height, Weight: $weight, Total Steps: $stepCount, Cadence: $cadence")

        Thread {
            try {
                establishConnectionIfNeeded()

                // Only send full user data once
                if (!userInputsSent && steps != null && age != null && weight != null) {
                    writer?.write("Steps: $steps, Age: $age, Weight: $weight, $stepCount,$cadence\n")
                    writer?.flush()
                    userInputsSent = true
                } else {
                    writer?.write("$stepCount,$cadence\n")
                    writer?.flush()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending data", e)
                resetConnection()
            }
        }.start()
    }

    private fun establishConnectionIfNeeded() {
        if (socket == null || socket!!.isClosed) {
            socket = Socket(SERVER_IP, SERVER_PORT)
            writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream(), "UTF-8"))
        }
    }

    private fun resetConnection() {
        try {
            writer?.close()
            socket?.close()
            socket = null
            writer = null
        } catch (closeException: IOException) {
            Log.e(TAG, "Error closing socket", closeException)
        }
    }

    private fun closeConnection() {
        try {
            writer?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket", e)
        }
    }
}