package com.example.myFitHololenzApp

import android.app.Service
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import java.util.concurrent.TimeUnit
import kotlin.math.log


class NewService(): Service() {


    private val context1 = this
    private val fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_STEP_COUNT_DELTA).build()
    var GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 123456
    val REQUEST_CODE = 1
    private val TAG = "thread"


    //val acc = GoogleSignIn.requestPermissions(context1,GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,GoogleSignIn.getAccountForExtension(context1, fitnessOptions),fitnessOptions)
    private var dataPointListener: OnDataPointListener? = null


    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "running onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(TAG,"testtstts");




        Fitness.getSensorsClient(context1, GoogleSignIn.getAccountForExtension(context1, fitnessOptions))
            .findDataSources(
                DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build())
            .addOnSuccessListener { dataSources ->
                Log.i(TAG, "Data source found: $dataSources")
                for (dataSource in dataSources) {
                    Log.i(TAG, "Data source found: $dataSource")
                    Log.i(TAG, "Data Source type: " + dataSource.dataType.name)
                    // Let's register a listener to receive Activity data!
                    if (dataSource.dataType == DataType.TYPE_STEP_COUNT_DELTA && dataPointListener == null) {
                        Log.i(TAG, "Data source for LOCATION_SAMPLE found!  Registering.")
                        registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_DELTA,fitnessOptions)
                    }
                }
            }
            //               Log.v(TAG,dataSources.toString());
//                dataSources.forEach {
//                    Log.v(TAG,it.toString());
//
//                    if (it.dataType == DataType.TYPE_HEART_RATE_BPM) {
//                        Log.i(TAG, "Data source for STEP_COUNT_DELTA found!")
//
//
//                    }
//                }
//            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Find data sources request failed", e)
            }

        return START_NOT_STICKY
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun callStop(){
        stopSelf()
    }



    fun registerFitnessDataListener(dataSource: DataSource, dataType: DataType, fitnessOptions:FitnessOptions) {
        // [START register_data_listener]
        dataPointListener = OnDataPointListener { dataPoint ->
            for (field in dataPoint.dataType.fields) {
                val value = dataPoint.getValue(field)
                Log.i(TAG, "Detected DataPoint field: ${field.name}")
                Log.i(TAG, "Detected DataPoint value: $value")
            }
        }
        Fitness.getSensorsClient(context1,  GoogleSignIn.getAccountForExtension(context1, fitnessOptions))
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