package com.mohsenoid.certhunter.domain.model

data class AppItem(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val firstInstallTime: Long = 0,
)
