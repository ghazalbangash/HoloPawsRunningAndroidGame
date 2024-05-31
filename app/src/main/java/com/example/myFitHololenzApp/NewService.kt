package com.example.myFitHololenzApp

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Value
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.util.concurrent.TimeUnit

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.request.DataReadRequest

class NewService : Service() {

    private val TAG = "server"
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_HEART_RATE_BPM)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .build()

    private var heartRateListener: OnDataPointListener? = null
    private var stepCountListener: OnDataPointListener? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")

        // Check for necessary permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "BODY_SENSORS permission not granted")
            // Handle the case where permission is not granted
            return
        }

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        registerHeartRateListener(account)
        registerStepCountListener(account)
        accessGoogleFit(account)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
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
                val value = dataPoint.getValue(field).asInt()
                Log.i(TAG, "Detected step count: $value")
                //sendData(mapOf("step_count" to dataPoint.getValue(field)))
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




    private fun sendData(fields: Map<String, Value>) {
        Thread {
            try {
                val socket = Socket("10.150.33.11", 9090)
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
                for ((_, value) in fields) {
                    writer.write("$value\n")
                }
                writer.flush()
                writer.close()
                socket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error sending data", e)
            }
        }.start()
    }
}
