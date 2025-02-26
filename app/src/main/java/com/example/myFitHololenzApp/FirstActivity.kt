package com.example.myFitHololenzApp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
class FirstActivity : AppCompatActivity() {
    private val TAG = "act"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first) // Use the activity layout



        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FirstFragment()) // Load your fragment into the container
                .commit()
        }
    }
}