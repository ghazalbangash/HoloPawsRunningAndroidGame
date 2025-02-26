package com.example.myFitHololenzApp

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException

class DataLogger(private val context: Context, private val fileName: String) {

    private val file: File

    init {
        // Create a directory in external storage
        val externalDir = File(context.getExternalFilesDir(null), "MyFitAppLogs")
        if (!externalDir.exists()) {
            val dirCreated = externalDir.mkdirs()
            if (!dirCreated) {
                throw IOException("Failed to create directory: ${externalDir.absolutePath}")
            }
        }

        // Create the file in this directory
        file = File(externalDir, fileName)

        // Create the file if it doesn't exist
        if (!file.exists()) {
            val fileCreated = file.createNewFile()
            if (!fileCreated) {
                throw IOException("Failed to create file: ${file.absolutePath}")
            }
        }
    }

    fun logData(data: String) {
        try {
            val writer = FileWriter(file, true)
            writer.append(data)
            writer.append("\n")
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun clearLog() {
        try {
            val writer = FileWriter(file, false)
            writer.write("") // Clear the file
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readLog(): String {
        return file.readText()
    }
}