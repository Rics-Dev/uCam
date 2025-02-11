package com.ricsdev.uconnect.navigation

import kotlinx.serialization.Serializable


sealed class Screens {
    @Serializable
    object SetupScreen


    @Serializable
    object HomeScreen

}