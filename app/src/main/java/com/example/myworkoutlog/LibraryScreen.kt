package com.example.myworkoutlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Library", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.ManageExercises.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Manage Exercises", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.ManageTemplates.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Manage Templates", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.Programs.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Manage Program Blueprints", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.PersonalRecords.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Personal Records", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.VolumeAnalysis.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Volume Analysis", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.Analytics.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Advanced Analytics", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.Settings.route) },
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Text("Settings", modifier = Modifier.padding(16.dp), fontSize = 18.sp)
        }
    }
}