package com.gezimos.katapult.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gezimos.katapult.MainViewModel
import com.gezimos.katapult.R
import com.gezimos.katapult.Screen

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(LocalSurface.current).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(R.string.settings), color = LocalInk.current)
        Text("Home", color = LocalInk.current, modifier = Modifier.padding(top = 24.dp).clickable { viewModel.navigateTo(Screen.HOME) })
    }
}

@Composable
fun SettingsToggleRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp).clickable { onCheckedChange(!checked) }) {
        Text(title, color = LocalInk.current)
        Text(description, color = LocalInk.current)
    }
}
