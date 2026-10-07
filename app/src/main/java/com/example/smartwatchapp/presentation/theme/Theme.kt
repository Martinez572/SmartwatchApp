package com.example.smartwatchapp.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun SmartwatchAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        content = content
    )
}