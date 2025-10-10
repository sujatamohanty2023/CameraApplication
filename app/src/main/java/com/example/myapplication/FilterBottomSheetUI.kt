package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FilterBottomSheetUI(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Trending") }
    val tabs = listOf("Trending", "New", "Face", "Green Screen")

    val filters = listOf(
        "Original", "Bright", "Retro", "Cool", "Warm", "Cinematic", "Soft", "Night",
        "Pop", "Vivid", "Dream", "Moody","Original", "Bright", "Retro", "Cool", "Warm", "Cinematic", "Soft", "Night",
        "Pop", "Vivid", "Dream", "Moody","Original", "Bright", "Retro", "Cool", "Warm", "Cinematic", "Soft", "Night",
        "Pop", "Vivid", "Dream", "Moody"
    )

    var selectedFilter by remember { mutableStateOf("Original") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(bottom = 8.dp)
    ) {
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            containerColor = Color.Black,
            contentColor = Color.White,
            edgePadding = 8.dp,
            indicator = {}
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab == selectedTab,
                    onClick = { selectedTab = tab },
                    text = {
                        Text(
                            text = tab,
                            color = if (tab == selectedTab) Color.White else Color.Gray,
                            fontWeight = if (tab == selectedTab) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Grid — Instagram/TikTok style
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Black),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filters) { filter ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable {
                        selectedFilter = filter
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (filter == selectedFilter) Color.White.copy(alpha = 0.2f)
                                else Color.DarkGray
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder for thumbnail (you can replace with Image)
                        Icon(
                            painter = painterResource(id = R.drawable.filter),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )

                        // Download icon overlay (if not downloaded)
                        if (filter != "Original") {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_download),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                                    .offset(x = (-4).dp, y = (-4).dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = filter,
                        color = if (filter == selectedFilter) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

