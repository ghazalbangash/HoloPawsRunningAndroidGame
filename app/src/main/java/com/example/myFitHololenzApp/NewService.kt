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
import com.google.android.gms.fitness.HistoryApi
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.request.DataReadRequest
import java.util.Calendar


class NewService : Service() {


    private val TAG = "server"
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_HEART_RATE_BPM,FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        //.addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .build()

    private var heartRateListener: OnDataPointListener? = null
    private var stepCountListener: OnDataPointListener? = null

    private var lastStepCount = 0
    private var lastTimestamp = System.currentTimeMillis()

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
        //accessGoogleFit2(account)
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



                val currentStepCount = dataPoint.getValue(field).asInt()
                val currentTimestamp = System.currentTimeMillis()

                // Calculate cadence
                val timeDifference = (currentTimestamp - lastTimestamp) / 1000.0 // in seconds
                val stepDifference = currentStepCount - lastStepCount
                val cadence = if (timeDifference > 0) (stepDifference / (timeDifference / 60.0)) else 0.0

                Log.i(TAG, "Detected step count: $currentStepCount, Cadence: $cadence steps/min")

                lastStepCount = currentStepCount
                lastTimestamp = currentTimestamp

                sendData(mapOf("step_count" to dataPoint.getValue(field)))
                sendData(mapOf("cadence" to cadence))

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


    private fun registerStepCountCadenceListener(account: GoogleSignInAccount) {

        stepCountListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field).asInt()
                Log.i(TAG, "Detected step count: $value")
                sendData(mapOf("step_count" to dataPoint.getValue(field)))
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

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MINUTE, -30)
        val startTime = calendar.timeInMillis



        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .setTimeRange(1, System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(this, account)
            .readData(readRequest)
            .addOnSuccessListener { response ->
                val heartRateData = response.getDataSet(DataType.TYPE_HEART_RATE_BPM)
                processDataSet(heartRateData)
                Log.i(TAG, "Detected heart rate: $heartRateData")
                //sendHeartRateToServer(heartRateData)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }


    private fun accessGoogleFit2(account: GoogleSignInAccount) {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.MINUTE, -30)
        val startTime = calendar.timeInMillis
        Log.i(TAG, "here2")

        // Build the intent for viewing heart rate data
        val fitIntent = HistoryApi.ViewIntentBuilder(this, DataType.AGGREGATE_HEART_RATE_SUMMARY)
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .setPreferredApplication("com.xiaomi.hm.health")
            .build()

        startActivity(fitIntent)
    }



    private fun processDataSet(dataSet: DataSet) {
        for (dp in dataSet.dataPoints) {

            val dataSource = dp.originalDataSource
            val appPkgName = dataSource.appPackageName
            // Use appPkgName as needed
            Log.d("AppPackageName", "Data inserted by: $appPkgName")
            for (field in dp.dataType.fields) {
                val value = dp.getValue(field).asFloat()
                Log.i(TAG, "Detected heart rate: $value")
                //sendData("heart_rate", value)
            }
        }
    }




    private fun sendData(fields: Map<String, Any>) {
        Thread {
            try {
                val socket = Socket("10.150.34.218", 9090)
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
