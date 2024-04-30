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
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Value
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.android.gms.tasks.Task
import java.io.BufferedWriter
import java.io.IOException
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Scanner
import java.util.concurrent.TimeUnit


class NewService(): Service() {


    private val context1 = this
    private val TAG = "thread"
    var dataValue: Int = 0
    val dataLiveData = MutableLiveData<Int>()


    // socket variables
    val address = "10.190.35.107"
    val port = 8080
    //val connection: Socket = Socket(address, port)
    //var connected: Boolean = true



    //val acc = GoogleSignIn.requestPermissions(context1,GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,GoogleSignIn.getAccountForExtension(context1, fitnessOptions),fitnessOptions)
    var dataPointListener: OnDataPointListener? = null
    var fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
        .addDataType(DataType.TYPE_HEART_RATE_BPM)
        .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
        .addDataType(DataType.TYPE_LOCATION_SAMPLE).accessActivitySessions(0)
        .build()



// server in service



    //end of server function


    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "running onCreate")
    }

    fun sendData(fields: Map<String, Value>) {

        val thread = Thread {
            try {
                val socket = Socket("10.150.45.159", 9090)
                //val oos = ObjectOutputStream(socket.getOutputStream())
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
                for ((key, value) in fields) {
                    writer.write("$value")

//                    oos.flush();
//                    oos.reset();
//                    oos.writeObject("hello")


                    //oos.writeObject("$key = $value")
                }

                writer.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        thread.start()
    }





    private fun findFitnessDataSources() { // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
        //val fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_HEART_POINTS).accessActivitySessions(0).build()
        Fitness.getSensorsClient(this, GoogleSignIn.getAccountForExtension(context1, fitnessOptions))
            .findDataSources(
                DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build())
            .addOnSuccessListener { dataSources ->
                for (dataSource in dataSources) {
                    Log.i(TAG, "Data source found: $dataSource")
                    Log.i(TAG, "Data Source type: " + dataSource.dataType.name)
                    // Let's register a listener to receive Activity data!
                    if (dataSource.dataType == DataType.TYPE_STEP_COUNT_CUMULATIVE && dataPointListener == null) {
                        Log.i(TAG, "Data source for LOCATION_SAMPLE found!  Registering.")


                        val datasource = DataSource.Builder()
                            .setAppPackageName("com.google.android.gms")
                            .setDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                            .setType(DataSource.TYPE_DERIVED)
                            .setStreamName("estimated_steps")
                            .build()


                        val listener = OnDataPointListener { dataPoint ->
                            for (field in dataPoint.dataType.fields) {
                                val value = dataPoint.getValue(field)
                                dataValue = value as Int
                                Log.i(TAG, "Detected DataPoint field: ${field.name}")
                                Log.i(TAG, "Detected DataPoint value: $dataValue")
                                dataLiveData.postValue(dataValue)
                            }
                        }

                        Fitness.getSensorsClient(this, GoogleSignIn.getAccountForExtension(context1, fitnessOptions))
                            .add(
                                SensorRequest.Builder()
                                    .setDataSource(datasource) // Optional but recommended for custom data sets.
                                    .setDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE) // Can't be omitted.
                                    .setSamplingRate(10, TimeUnit.SECONDS)
                                    .build(),
                                listener)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.i(TAG, "Listener registered!")
                                } else {
                                    Log.e(TAG, "Listener not registered.", task.exception)
                                }
                            }

                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "failed", e) }
        // [END find_data_sources]
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {




        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION)




        // findFitnessDataSources()

        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing.", e)
            }


        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .listSubscriptions()
            .addOnSuccessListener { subscriptions ->
                for (sc in subscriptions) {
                    val dt = sc.dataType
                    if (dt != null) {
                        Log.i(TAG, "Active subscription for data type: ${dt.name}")
                    }
                }
            }


        Fitness.getSensorsClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .findDataSources(
                DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .build())
            .addOnSuccessListener { dataSources ->
                Log.i(TAG, "gjkhjhjhj: $dataSources")
                for (dataSource in dataSources) {
                    Log.i(TAG, "Data source found: $dataSource")
                    Log.i(TAG, "Data Source type: " + dataSource.dataType.name)
                    // Let's register a listener to receive Activity data!
                    if (dataSource.dataType == DataType.TYPE_STEP_COUNT_CUMULATIVE && dataPointListener == null) {
                        Log.i(TAG, "Data source for LOCATION_SAMPLE found!  Registering.")
                        registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "failed", e) }

        return START_NOT_STICKY
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun callStop(){
        stopSelf()
    }



    private fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType) {
        // [START register_data_listener]
        dataPointListener = OnDataPointListener { dataPoint ->
            Log.i(TAG, "Dasasfasfasfasf: ${dataPoint.dataType}")
//            val writer: OutputStream = connection.getOutputStream()
            val fields: HashMap<String, Value> = HashMap()

            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field)
                Log.i(TAG, "Detected DataPoint field: ${field.name}")
                Log.i(TAG, "Detected DataPoint value11: $value")
                //writer.write(("hellooo" + '\n').toByteArray(Charset.defaultCharset()))
                Log.i(TAG, "inside writer: $value")
                fields[field.name] = value
            }

            sendData(fields)

        }
        Fitness.getSensorsClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .add(
                SensorRequest.Builder()
                    .setDataSource(dataSource) // Optional but recommended for custom data sets.
                    .setDataType(dataType) // Can't be omitted.
                    .setSamplingRate(10, TimeUnit.SECONDS)
                    .build(),
                dataPointListener!!
            )
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i(TAG, "Listener registered!")
                } else {
                    Log.e(TAG, "Listener not registered.", task.exception)
                }
            }
        // [END register_data_listener]
    }
}