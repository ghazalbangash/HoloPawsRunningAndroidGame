package com.example.myFitHololenzApp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.myFitHololenzApp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataSourcesRequest
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.Socket


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 0x1001
    val REQUEST_CODE = 1
    private val TAG = "MyActivity"
    private var fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_LOCATION_SAMPLE).build()

    val fitnessOptions2 = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(
            Fitness.SCOPE_BODY_READ_WRITE,
            Fitness.SCOPE_ACTIVITY_READ_WRITE
        )
        .build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setContentView(R.layout.activity_main)

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)



        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                account,
                fitnessOptions)
        }
//        else {
//            accessGoogleFit(fitnessOptions)
//
//        }


        setFitnessOption();
        checkFitInstalled();


        binding.root.findViewById<Button>(R.id.button1).setOnClickListener { view ->

            //val connection: Socket = Socket(address, port)
            //val writer: OutputStream = connection.getOutputStream()
           // writer.write(("hellooo" + '\n').toByteArray(Charset.defaultCharset()))

//            Log.v(TAG, "index=" + 1);
            val serviceIntent = Intent(this, NewService::class.java)
            val supportedType = DataType.getMimeType(DataType.AGGREGATE_HEART_RATE_SUMMARY)


            if (Intent.ACTION_VIEW == intent.action && supportedType == intent.type) {
                // Get the intent extras
                val startTime = intent.getLongExtra("vnd.google.gms.fitness.start_time", 0L)
                val endTime = intent.getLongExtra("vnd.google.gms.fitness.end_time", 0L)
                val dataSource = intent.getParcelableExtra<DataSource>("vnd.google.gms.fitness.data_source")

                // Process the data
                Log.i("IntentData", "Start time: $startTime, End time: $endTime, Data Source: $dataSource")
            }

            startService(serviceIntent)
//            //test123()



        }

        binding.root.findViewById<Button>(R.id.button2).setOnClickListener { view ->

            //setOnwTimeWorkRequest()


        }

        binding.root.findViewById<Button>(R.id.button3).setOnClickListener { view ->


        }




//       val fitnessOptions = FitnessOptions.builder()
//            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
//            .build()
//
//
//
//




    }



//    @SuppressLint("SuspiciousIndentation")
//    private fun setOnwTimeWorkRequest(){
//
//        val uploadRequest = OneTimeWorkRequest.Builder(HololensClient::class.java)
//            .build()
//        val workManager = WorkManager.getInstance(applicationContext)
//        workManager.enqueue(uploadRequest)
//
//    }
    fun test123(){

        var fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .addDataType(DataType.TYPE_HEART_RATE_BPM)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .build()




        PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION)




        // findFitnessDataSources()

        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
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



// Note: Fitness.SensorsApi.findDataSources() requires the
// ACCESS_FINE_LOCATION permission.
        Fitness.getSensorsClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .findDataSources(
                DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build())
            .addOnSuccessListener { dataSources ->
                dataSources.forEach {
                    Log.i(TAG, "Data source found: ${it.streamIdentifier}")
                    Log.i(TAG, "Data Source type: ${it.dataType.name}")


                    if (it.dataType == DataType.TYPE_STEP_COUNT_DELTA) {
                        Log.i(TAG, "Data source for STEP_COUNT_DELTA found!")


                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Find data sources request failed", e)
            }


    }

    fun checkFitInstalled() {
        if (isGoogleFitPermissionGranted()) {
            Log.i(TAG, "Granted")
        } else {
            requestGoogleFitPermission()
        }
    }

    fun setFitnessOption() {
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .addDataType(DataType.TYPE_HEART_RATE_BPM)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
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