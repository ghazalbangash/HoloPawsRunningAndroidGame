Android Fitness App

1- Overview

This is an Android fitness application that integrates with the Google Fit API to track real-time fitness metrics such as step count, heart rate, and cadence. The app provides users with a seamless experience for monitoring their activity levels while ensuring accurate data collection.

2- Features

-Real-time Step Tracking: Retrieves step count data from Google Fit.

-Heart Rate Monitoring: Fetches heart rate readings from supported devices.

-Cadence Tracking: Measures steps per minute to help users analyze their running or walking pace.

-Live Data Display: Shows fitness metrics in an easy-to-read user interface.

-Data Synchronization: Ensures smooth integration with Google Fit.

3- Tech Stack

-Language: Kotlin

-Google Fit API for fitness data retrieval

-Android Jetpack Components (LiveData, ViewModel)

-Retrofit (optional) for API integration

-Socket Communication (if applicable) for real-time data updates

4-Installation

Prerequisites

-Android device with Google Fit installed

-Google Fit account connected

-Android Studio (latest version)

5-Steps to Install

-Clone this repository

-Open the project in Android Studio.

-Set up a Google Fit API project in Google Cloud Console and obtain an OAuth 2.0 client ID.

-Add the client ID to your Google Fit API configuration in AndroidManifest.xml.

-Run the app on a physical device or emulator with Google Fit support.

6-Usage

-Sign in with your Google account to enable fitness tracking.

-Grant necessary Google Fit permissions.

-Start an activity and monitor your real-time step count, cadence, and heart rate.

-Data updates dynamically while you move.

-Optionally, send fitness data to a connected HoloLens application.

7-Permissions

The app requires the following permissions:

android.permission.ACTIVITY_RECOGNITION

android.permission.BODY_SENSORS

android.permission.INTERNET (if sending data over a network)

8-Known Issues

Delayed sync with Google Fit: Some data might take a few seconds to update.

Device Compatibility: Ensure your device supports Google Fit sensors.

Background Tracking: Some devices may restrict background activity due to battery optimizations.

9-Future Improvements

Workout Session Tracking

Calorie Estimation Algorithm

User Profile & Progress Dashboard

Wearable Integration (Smartwatches, Fitness Bands)
