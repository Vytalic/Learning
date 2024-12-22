package com.bignerdranch.android.criminalintent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bignerdranch.android.criminalintent.ui.theme.CriminalIntentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_crime_detail)

        // Add the fragment if it’s not already added
//        if (savedInstanceState == null) {
//            val fragment = CrimeDetailFragment() // Create an instance of your fragment
//            supportFragmentManager.beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .commit()

    }
}

