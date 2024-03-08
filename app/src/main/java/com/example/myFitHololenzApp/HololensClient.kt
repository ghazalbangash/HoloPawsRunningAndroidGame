package com.example.myFitHololenzApp


import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner
import kotlin.concurrent.thread

//fun main(args: Array<String>) {
//    val address = "10.190.33.114"
//    val port = 139
//
////    val client = HololensClient(address, port)
////    client.run()
//}
class HololensClient(context : Context, params : WorkerParameters): Worker(context,params){
    val address = "10.190.35.107"
    val port = 8080
    private val TAG = "thread"
    val connection: Socket = Socket(address, port)
    var connected: Boolean = true
    private val context1 = this
    override fun doWork(): Result {
        val reader: Scanner = Scanner(connection.getInputStream())
        val writer: OutputStream = connection.getOutputStream()

        val data = NewService().dataLiveData.value
//        thread { println(reader.nextLine()) }
        while (connected) {
//            val input = readLine() ?: ""
//            if ("exit" in input) {
//                connected = false
//                reader.close()
//                connection.close()
//                Log.i(TAG, "running reader")
//            } else {
                writer.write(("hellooo" + '\n').toByteArray(Charset.defaultCharset()))
                Log.i(TAG, "inside writer: $data")
            }

        return Result.success()
    }

//    fun newDataArrived(var data){

//    }

    }

