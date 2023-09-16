package com.example.myFitHololenzApp

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.myFitHololenzApp.databinding.FragmentFirstBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    var GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 123456
    val REQUEST_CODE = 1
    // This property is only valid between onCreateView and
    // onDestroyView.
    //private lateinit var phoneData: String
    private val binding get() = _binding!!


    companion object DataFromPhone{
        var phoneData= ""


        fun test(response: Task<DataReadResponse>) {
            val readDataResponse = Tasks.await<DataReadResponse>(response)
            val dataSet = readDataResponse.getDataSet(DataType.TYPE_STEP_COUNT_DELTA)
            phoneData = dataSet.toString()

            TODO("Not yet implemented")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }


        // find the toast_button by its ID and set a click listener
//        view.findViewById<Button>(R.id.button_first).setOnClickListener {
//            // create a Toast with some text, to appear for a short time
//
//            view.findViewById<TextView>(R.id.textview_first).setText(phoneData)
//
//            // show the Toast
//
//        }
    }

    fun test(){
        print(1)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}