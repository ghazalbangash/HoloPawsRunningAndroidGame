package com.example.myFitHololenzApp


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.ui.AppBarConfiguration
import com.example.myFitHololenzApp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataSourcesRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileWriter
import java.io.IOException




class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    var GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 0x1001
    val REQUEST_CODE = 1
    private val TAG = "MyActivity"
    private var fitnessOptions = FitnessOptions.builder().addDataType(DataType.TYPE_LOCATION_SAMPLE).build()
//    lateinit var StepInput: EditText
//    lateinit var AgeInput: EditText
//    lateinit var HeightInput: EditText
    companion object {
        const val REQUEST_WRITE_STORAGE_PERMISSION = 1002
    }

    private lateinit var inputAge: TextInputEditText
    private lateinit var inputHeight: TextInputEditText
    private lateinit var inputWeight: TextInputEditText
    private lateinit var inputStepGoal: TextInputEditText
    private lateinit var nextButton: MaterialButton
    private val _cadenceLiveData = MutableLiveData<Double>()
    val cadenceLiveData: LiveData<Double> = _cadenceLiveData
    private lateinit var cadenceProgressBar: ProgressBar
    private lateinit var cadenceTextView: TextView



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

        // Check if the WRITE_EXTERNAL_STORAGE permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            // If not, request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE_PERMISSION
            )
        } else {
            // Permission is already granted, you can initialize your logger or proceed with file operations
            initializeLoggerAndProceed()
        }

        inputAge = findViewById<TextInputEditText>(R.id.input_age)
        inputHeight = findViewById<TextInputEditText>(R.id.input_height)
        inputWeight = findViewById<TextInputEditText>(R.id.input_weight)
        inputStepGoal = findViewById<TextInputEditText>(R.id.input_step_goal)

//        nextButton = findViewById<MaterialButton>(R.id.next_button)
//        nextButton.setOnClickListener { createLogFileAndNavigate() }

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        // Initialize ProgressBar and TextView


        // Start the PermissionActivity to request permissions and then start the service
//        val intent = Intent(this, PermissionActivity::class.java)
//        startActivity(intent)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this, // your activity
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                account,
                fitnessOptions)
        }


        setFitnessOption();
        checkFitInstalled();


        binding.root.findViewById<Button>(R.id.next_button).setOnClickListener { view ->
            createLogFileAndNavigate()
            val serviceIntent = Intent(this, NewService::class.java)


            //val connection: Socket = Socket(address, port)
            //val writer: OutputStream = connection.getOutputStream()
            // writer.write(("hellooo" + '\n').toByteArray(Charset.defaultCharset()))
            val steps = inputStepGoal.text.toString()
            val age = inputAge.text.toString()
            val height = inputHeight.text.toString()
            val weight = inputWeight.text.toString()

            serviceIntent.putExtra("steps", steps)  // Hardcoded value
            serviceIntent.putExtra("age", age)
            serviceIntent.putExtra("height", height)
            serviceIntent.putExtra("weight", weight)
            startService(serviceIntent)


        }







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
    private fun startCadenceService() {
        val serviceIntent = Intent(this, NewService::class.java)
        startService(serviceIntent)
    }

    private fun updateCadenceUI(cadence: Double) {
        val MAX_CADENCE= 2000
        val progress = (cadence * 100 / MAX_CADENCE).toInt() // Assuming MAX_CADENCE is a constant you define
        cadenceProgressBar.progress = progress
        cadenceTextView.text = "${cadence.toInt()} steps/min"
    }

    fun checkFitInstalled() {
        if (isGoogleFitPermissionGranted()) {
            Log.i(TAG, "Granted")
        } else {
            requestGoogleFitPermission()
        }
    }
    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with file operations
                initializeLoggerAndProceed()
            } else {
                // Permission denied, show a message to the user or handle it gracefully
            }
        }
    }

    private fun initializeLoggerAndProceed() {
        // Initialize your DataLogger or other components that require the permission
    }
    private fun createLogFileAndNavigate() {
        val userAge = inputAge.text.toString()
        val userHeight = inputHeight.text.toString()
        val userWeight = inputWeight.text.toString()
        val stepGoal = inputStepGoal.text.toString()

        val logFile = File(filesDir, "user_data.log")

        try {
            if (!logFile.exists()) {
                logFile.createNewFile()
            }

            val writer = FileWriter(logFile, true)
            writer.append("Age: $userAge, Height: $userHeight, Weight: $userWeight, Step Goal: $stepGoal\n")
            writer.close()

            Toast.makeText(this, "Log file created successfully!", Toast.LENGTH_SHORT).show()

            // Pass the step goal to the FirstActivity
            val intent = Intent(this, FirstActivity::class.java).apply {
                putExtra("STEP_GOAL", stepGoal.toInt())
            }
            startActivity(intent)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating log file!", Toast.LENGTH_SHORT).show()
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


}