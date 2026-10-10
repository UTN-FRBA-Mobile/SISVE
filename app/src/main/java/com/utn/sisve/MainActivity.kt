package com.utn.sisve

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.utn.sisve.data.local.AmbulancePreferences
import com.utn.sisve.ui.navigation.AppNavGraph
import com.utn.sisve.ui.navigation.AppRoute
import com.utn.sisve.ui.theme.SisveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ambulancePreferences: AmbulancePreferences

    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SisveTheme {
                val startDestination = if (ambulancePreferences.isConfigured()) {
                    AppRoute.Login.route
                } else {
                    AppRoute.Setup.route
                }

                AppNavGraph(startDestination = startDestination)
            }
        }
    }
}