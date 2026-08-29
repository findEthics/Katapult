package com.gezimos.katapult.model

data class AppModel(
    val packageName: String,
    val label: String,
    val activityName: String,
    // User serial number of the profile this app belongs to.
    // 0 = main/current user; non-zero = a managed (work) profile or other user.
    val userSerial: Long = 0L
)
