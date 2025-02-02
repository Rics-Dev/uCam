package com.ricsdev.ucam.navigation

import kotlinx.serialization.Serializable


sealed class Screens {
    @Serializable
    object SetupScreen


    @Serializable
    object HomeScreen

}