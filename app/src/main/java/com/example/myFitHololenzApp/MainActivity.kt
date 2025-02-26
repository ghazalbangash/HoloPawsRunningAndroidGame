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

    companion object {
        private const val TAG = "MainActivity"
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 0x1001
        const val REQUEST_WRITE_STORAGE_PERMISSION = 1002
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // UI components
    private lateinit var inputAge: TextInputEditText
    private lateinit var inputHeight: TextInputEditText
    private lateinit var inputWeight: TextInputEditText
    private lateinit var inputStepGoal: TextInputEditText
    private lateinit var nextButton: MaterialButton
    private lateinit var cadenceProgressBar: ProgressBar
    private lateinit var cadenceTextView: TextView

    // LiveData
    private val _cadenceLiveData = MutableLiveData<Double>()
    val cadenceLiveData: LiveData<Double> = _cadenceLiveData

    // Google Fit options
    private var fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_LOCATION_SAMPLE)
        .build()

    private val fitnessSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
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

        // Initialize UI components
        initializeViews()

        // Check permissions
        checkStoragePermission()
        checkGoogleFitPermissions()

        // Setup button click listeners
        setupButtonListeners()
    }

    private fun initializeViews() {
        inputAge = findViewById(R.id.input_age)
        inputHeight = findViewById(R.id.input_height)
        inputWeight = findViewById(R.id.input_weight)
        inputStepGoal = findViewById(R.id.input_step_goal)
        // Initialize other UI components as needed
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE_PERMISSION
            )
        } else {
            initializeLoggerAndProceed()
        }
    }

    private fun initializeLoggerAndProceed() {
        // Initialize your DataLogger or other components that require the permission
    }

    private fun checkGoogleFitPermissions() {
        initializeGoogleFitOptions()

        if (!isGoogleFitPermissionGranted()) {
            requestGoogleFitPermission()
        }
    }

    private fun initializeGoogleFitOptions() {
        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
            .addDataType(DataType.TYPE_HEART_RATE_BPM)
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .build()
    }

    private fun isGoogleFitPermissionGranted(): Boolean {
        return GoogleSignIn.hasPermissions(
            GoogleSignIn.getLastSignedInAccount(this),
            fitnessOptions
        )
    }

    private fun requestGoogleFitPermission() {
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        GoogleSignIn.requestPermissions(
            this,
            GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
            account,
            fitnessOptions
        )
    }

    private fun setupButtonListeners() {
        binding.root.findViewById<Button>(R.id.next_button).setOnClickListener {
            createLogFileAndNavigate()
            startFitnessService()
        }
    }

    private fun startFitnessService() {
        val serviceIntent = Intent(this, NewService::class.java).apply {
            putExtra("steps", inputStepGoal.text.toString())
            putExtra("age", inputAge.text.toString())
            putExtra("height", inputHeight.text.toString())
            putExtra("weight", inputWeight.text.toString())
        }
        startService(serviceIntent)
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

            FileWriter(logFile, true).use { writer ->
                writer.append("Age: $userAge, Height: $userHeight, Weight: $userWeight, Step Goal: $stepGoal\n")
            }

            Toast.makeText(this, "Log file created successfully!", Toast.LENGTH_SHORT).show()

            // Navigate to FirstActivity with step goal
            val intent = Intent(this, FirstActivity::class.java).apply {
                putExtra("STEP_GOAL", stepGoal.toIntOrNull() ?: 0)
            }
            startActivity(intent)
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating log file!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCadenceUI(cadence: Double) {
        val MAX_CADENCE = 2000
        val progress = (cadence * 100 / MAX_CADENCE).toInt()
        cadenceProgressBar.progress = progress
        cadenceTextView.text = "${cadence.toInt()} steps/min"
    }

    private fun subscribeToCadenceUpdates() {
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        Fitness.getRecordingClient(this, account)
            .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully subscribed to step count!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing to step count.", e)
            }

        Fitness.getSensorsClient(this, account)
            .findDataSources(
                DataSourcesRequest.Builder()
                    .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                    .setDataSourceTypes(DataSource.TYPE_RAW)
                    .build()
            )
            .addOnSuccessListener { dataSources ->
                dataSources.forEach {
                    Log.i(TAG, "Data source found: ${it.streamIdentifier}")
                    Log.i(TAG, "Data Source type: ${it.dataType.name}")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Find data sources request failed", e)
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeLoggerAndProceed()
                } else {
                    Toast.makeText(
                        this,
                        "Storage permission is required for logging",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}