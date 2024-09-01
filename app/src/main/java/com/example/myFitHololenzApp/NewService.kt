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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.TimeUnit

class NewService : Service() {

    // Binder given to clients


    private val TAG = "server"
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_HEART_RATE_BPM)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .build()

    private var heartRateListener: OnDataPointListener? = null
    private var stepCountListener: OnDataPointListener? = null

    private var lastStepCount = 0
    private var totalSteps = 0
    private var lastTimestamp = System.currentTimeMillis()

    // Member variables to store user inputs and a flag to check if they have been sent
    private var steps: String? = null
    private var age: String? = null
    private var height: String? = null
    private var weight: String? = null
    private var userInputsSent = false

    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private val REQUEST_LOCATION_PERMISSION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    // Class used for the client Binder.

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//
//        // Initialize the LocationCallback
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult) {
//                for (location in locationResult.locations) {
//                    Log.i(TAG, "Location: ${location.latitude}, ${location.longitude}")
//                    //sendLocationData(location.latitude, location.longitude)
//                }
//            }
//        }

        // Check for necessary permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Necessary permissions not granted")
            // todo add a toast for this message
            return
        }
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        registerHeartRateListener(account)
        registerStepCountListener(account)
        accessGoogleFit(account)
        //startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")

        // Check if the intent is null
        if (intent == null) {
            Log.e(TAG, "Intent is null in onStartCommand")
            return START_NOT_STICKY
        }

        // Retrieve the user inputs from the Intent if they haven't been set yet
        steps = intent.getStringExtra("steps")
        age = intent.getStringExtra("age")
        height = intent.getStringExtra("height")
        weight = intent.getStringExtra("weight")

        Log.i(TAG, "Received data - Steps: $steps, Age: $age, Height: $height, Weight: $weight")

        // Check if the data was received correctly
        if (steps == null || age == null || height == null || weight == null) {
            Log.e(TAG, "One or more data fields are null")
        } else {
            Log.i(TAG, "Data received successfully: Steps: $steps, Age: $age, Height: $height, Weight: $weight")
        }

        return START_NOT_STICKY
    }


    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterListeners()
    }

    private fun registerHeartRateListener(account: GoogleSignInAccount) {
        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_HEART_RATE_BPM)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing.", e)
            }

        heartRateListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field).asFloat()
                Log.i(TAG, "Detected heart rate: $value")
                //sendData(mapOf("heart_rate" to dataPoint.getValue(field)))
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
                val currentStepCount = dataPoint.getValue(field).asInt()
                val currentTimestamp = System.currentTimeMillis()

                if (lastStepCount == 0) {
                    lastStepCount = currentStepCount
                    lastTimestamp = currentTimestamp
                    Log.i(TAG, "Initial step count: $currentStepCount")
                    return@OnDataPointListener
                }

                // Calculate step difference
                val stepDifference = currentStepCount - lastStepCount
                Log.i(TAG, "total step: $totalSteps, step dif : $stepDifference")
                Log.i(TAG, "curstep: $currentStepCount, last : $lastStepCount")

                if (stepDifference > 0) {
                    totalSteps += stepDifference
                    Log.i(TAG, "step diff: $stepDifference, total: $totalSteps")
                }
                // Calculate cadence
                val timeDifference = (currentTimestamp - lastTimestamp) / 1000.0 // in seconds
                val cadence = calculateCadence(currentStepCount, currentTimestamp)
                Log.i(TAG, "Cadence: $cadence steps/min")

                Log.i(TAG, "Detected step count: $currentStepCount, Cadence: $cadence steps/min, stepDifference: $stepDifference")

                lastStepCount = currentStepCount
                lastTimestamp = currentTimestamp
                // Send both step count and cadence
                sendData(totalSteps, cadence)
            }
        }

        val sensorRequest = SensorRequest.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .setSamplingRate(10, TimeUnit.SECONDS)
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
    fun calculateCadence(currentStepCount: Int, currentTimestamp: Long): Double {
        if (lastStepCount == 0) {
            lastStepCount = currentStepCount
            lastTimestamp = currentTimestamp
            Log.i(TAG, "Initial step count: $currentStepCount")
            return 0.0
        }

        val stepDifference = currentStepCount - lastStepCount
        val timeDifference = (currentTimestamp - lastTimestamp) / 1000.0 // in seconds

        lastStepCount = currentStepCount
        lastTimestamp = currentTimestamp

        return if (timeDifference > 0) (stepDifference / (timeDifference / 60.0)) else 0.0
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

        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_HEART_RATE_BPM)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed here !")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing.", e)
            }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.HOURS.toMillis(1)  // Fetch data from the past hour

        Log.i(TAG, "Fetching data from $startTime to $endTime")

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                Log.i(TAG, "Data read successfully: ${dataReadResponse.dataSets.size} datasets")
                val heartRateData = dataReadResponse.getDataSet(DataType.TYPE_HEART_RATE_BPM)
                Log.i(TAG, "heart rate: $heartRateData datasets")
                for (dataSet in dataReadResponse.dataSets) {
                    processDataSet(dataSet)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to read data", e)
            }
    }

    private fun processDataSet(dataSet: DataSet) {
        for (dp in dataSet.dataPoints) {
            for (field in dp.dataType.fields) {
                val value = dp.getValue(field).asFloat()
                Log.i(TAG, "Detected heart rate1: $value")
                //sendData("heart_rate", value)
            }
        }
    }



    private fun sendData(stepCount: Int, cadence: Double) {
        Log.i(TAG, "sendingy: Steps: $steps, Age: $age, Height: $height, Weight: $weight")
        Thread {
            try {
                if (socket == null || socket!!.isClosed) {
                    socket = Socket("10.150.33.37", 9090)
                    writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream(), "UTF-8"))
                }

                // Send user inputs only once
                if (!userInputsSent && steps != null && age != null && weight != null) {
                    writer!!.write("Steps: $steps, Age: $age, Weight: $weight, $stepCount,$cadence\n")
                    writer!!.flush()
                    userInputsSent = true // Mark user inputs as sent
                } else {
                    writer!!.write("$stepCount,$cadence\n")
                    writer!!.flush()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error sending data", e)
                // Optionally, close socket and writer on error
                try {
                    writer?.close()
                    socket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Error closing socket", closeException)
                }
            }
        }.start()
    }
    // Don't forget to properly close the socket and writer when they are no longer needed
    fun closeConnection() {
        try {
            writer?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket", e)
        }
    }

//    private fun startLocationUpdates() {
//        val locationRequest = LocationRequest.create().apply {
//            interval = 10000 // 10 seconds
//            fastestInterval = 5000 // 5 seconds
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//        }
//
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
//            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
//        } else {
//            Log.e(TAG, "Location permissions not granted")
//        }
//    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



//    private fun sendLocationData(latitude: Double, longitude: Double) {
//        Thread {
//            try {
//                if (socket == null || socket!!.isClosed) {
//                    socket = Socket("10.150.32.157", 9090)
//                    writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream(), "UTF-8"))
//                }
//                writer!!.write("Location: $latitude, $longitude\n")
//                writer!!.flush()
//            } catch (e: IOException) {
//                Log.e(TAG, "Error sending location data", e)
//                try {
//                    writer?.close()
//                    socket?.close()
//                } catch (closeException: IOException) {
//                    Log.e(TAG, "Error closing socket", closeException)
//                }
//            }
//        }.start()
//    }




}


