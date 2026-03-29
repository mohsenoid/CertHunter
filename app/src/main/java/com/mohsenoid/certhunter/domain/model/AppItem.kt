package com.mohsenoid.certhunter.domain.model

import android.graphics.drawable.Drawable

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)
