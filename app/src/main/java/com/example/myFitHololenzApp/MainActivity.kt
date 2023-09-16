package com.example.myFitHololenzApp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myFitHololenzApp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 123456
    val REQUEST_CODE = 1
    private val TAG = "MyActivity"
    private var fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_main)

        //setSupportActionBar(binding.toolbar)
        setFitnessOption();
        checkFitInstalled();
        binding.root.findViewById<Button>(R.id.button1).setOnClickListener { view ->

            val request = OneTimeWorkRequestBuilder<Dataworker>().build()
            WorkManager.getInstance(this).enqueue(request)


            WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
                .observe(this, Observer {

                    val status: String = it.state.name
                    Toast.makeText(this,status, Toast.LENGTH_SHORT).show()
                })

            binding.root.findViewById<TextView>(R.id.textView1).setText("agcgjcjgcjgcjgf")
        }




//       val fitnessOptions = FitnessOptions.builder()
//            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//            .build()
//
//
//
//
//        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
//        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
//            GoogleSignIn.requestPermissions(
//                this, // your activity
//                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
//                account,
//                fitnessOptions)
//        }
////        else {
////            accessGoogleFit(fitnessOptions)
////
////        }



    }

    fun checkFitInstalled() {
        if (isGoogleFitPermissionGranted()) {
            // do whatever you need here
        } else {
            requestGoogleFitPermission()
        }
    }

    fun setFitnessOption() {
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .build()
    }

    fun isGoogleFitPermissionGranted(): Boolean {
        return if (GoogleSignIn.hasPermissions(
                GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)
        ) {
            true
        } else {
            false
        }
    }



    fun requestGoogleFitPermission() {
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        GoogleSignIn.requestPermissions(
            this,
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
            account,
            fitnessOptions
        )
    }
//    private fun accessGoogleFit(fitnessOptions:FitnessOptions ) {
//        val end = LocalDateTime.now()
//        val start = end.minusYears(1)
//        val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
//        val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()
//
//        val readRequest = DataReadRequest.Builder()
//            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
//            .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
//            .bucketByTime(1, TimeUnit.DAYS)
//            .build()
//
//        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
//        val response = Fitness.getHistoryClient(this, account)
//            .readData(readRequest)
//            .addOnSuccessListener({ response ->
//                // Use response data here
//                Log.i(TAG, "OnSuccess()")
//            })
//            .addOnFailureListener({ e -> Log.d(TAG, "OnFailure()", e) })
//
//
////        val readDataResponse = Tasks.await<DataReadResponse>(response)
////        val dataSet = readDataResponse.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
//        //FirstFragment.test(response)
//
//        val request = OneTimeWorkRequestBuilder<Dataworker>().build()
//        WorkManager.getInstance(this).enqueue(request)
//
//
//        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
//        .observe(this, Observer {
//
//            val status: String = it.state.name
//            Toast.makeText(this,status, Toast.LENGTH_SHORT).show()
//        })
//
//
//    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}