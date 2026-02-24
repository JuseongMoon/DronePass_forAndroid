package com.ScienceFiction.DronePassAndroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ScienceFiction.DronePassAndroid.ui.theme.DronePassAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DronePassAndroidTheme {
                MapScreen()
            }
        }
    }
}