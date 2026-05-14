package com.mohsenoid.certhunter.ui.detail

sealed class AppDetailEvent {
    data class Share(val text: String) : AppDetailEvent()
}
