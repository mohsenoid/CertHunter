package com.mohsenoid.certhunter.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object AppList : NavKey

@Serializable
data class AppDetail(val packageName: String) : NavKey

@Serializable
data object AppAbout : NavKey
