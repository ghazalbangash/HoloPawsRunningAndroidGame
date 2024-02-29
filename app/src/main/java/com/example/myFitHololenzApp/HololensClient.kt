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
    val address = "10.190.33.114"
    val port = 139
    private val TAG = "thread"
//    private val connection: Socket = Socket(address, port)
//    private var connected: Boolean = true

//    init {
//        println("Connected to server at $address on port $port")
//    }

//    private val reader: Scanner = Scanner(connection.getInputStream())
//    private val writer: OutputStream = connection.getOutputStream()
    override fun doWork(): Result {
        Log.i(TAG, "running onCreate")
        val connection: Socket = Socket(address, port)
        var connected: Boolean = true
        val reader: Scanner = Scanner(connection.getInputStream())
        val writer: OutputStream = connection.getOutputStream()
        thread { println(reader.nextLine()) }
        while (connected) {
            val input = readLine() ?: ""
            if ("exit" in input) {
                connected = false
                reader.close()
                connection.close()
            } else {
                writer.write(("message" + '\n').toByteArray(Charset.defaultCharset()))
            }
    }
        return Result.success()
    }
//    fun connectinToServer() {
//        println("Connected entera")
//        thread { read() }
//        while (connected) {
//            val input = readLine() ?: ""
//            if ("exit" in input) {
//                connected = false
//                reader.close()
//                connection.close()
//            } else {
//                write(input)
//            }
//        }

    }

//    private fun write(message: String) {
//        writer.write((message + '\n').toByteArray(Charset.defaultCharset()))
//    }
//
//    private fun read() {
//        while (connected)
//            println(reader.nextLine())
//    }
