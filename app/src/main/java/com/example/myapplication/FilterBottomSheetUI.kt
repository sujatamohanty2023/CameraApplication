package com.example.myapplication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

data class FilterItem(
    val name: String,
    val path: String? // null means no filter
)
@Composable
fun FilterBottomSheetUI(
    selectedFilter: FilterItem?,
    onFilterSelected: (FilterItem?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Trending") }
    val tabs = listOf("Trending", "New", "Face", "Green Screen")

    val filters = remember {
        listOf(
            FilterItem("None", null),
            FilterItem("Viking", "file:///android_asset/viking_helmet.deepar"),
            FilterItem("Makeup", "file:///android_asset/MakeupLook.deepar"),
            FilterItem("Split View", "file:///android_asset/Split_View_Look.deepar"),
            FilterItem("Emotions", "file:///android_asset/Emotions_Exaggerator.deepar"),
            FilterItem("Meter", "file:///android_asset/Emotion_Meter.deepar"),
            FilterItem("Stallone", "file:///android_asset/Stallone.deepar"),
            FilterItem("Flowers", "file:///android_asset/flower_face.deepar"),
            FilterItem("Galaxy", "file:///android_asset/galaxy_background.deepar"),
            FilterItem("Humanoid", "file:///android_asset/Humanoid.deepar"),
            FilterItem("Neon Devil", "file:///android_asset/Neon_Devil_Horns.deepar"),
            FilterItem("Ping Pong", "file:///android_asset/Ping_Pong.deepar"),
            FilterItem("Hearts", "file:///android_asset/Pixel_Hearts.deepar"),
            FilterItem("Snail", "file:///android_asset/Snail.deepar"),
            FilterItem("Hope", "file:///android_asset/Hope.deepar"),
            FilterItem("Vendetta", "file:///android_asset/Vendetta_Mask.deepar"),
            FilterItem("Fire", "file:///android_asset/Fire_Effect.deepar"),
            FilterItem("Burning", "file:///android_asset/burning_effect.deepar"),
            FilterItem("Elephant", "file:///android_asset/Elephant_Trunk.deepar")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(bottom = 8.dp)
    ) {
        // Tabs
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            containerColor =Color.Transparent,
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

        // Grid of filters
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Transparent),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            items(filters) { filter ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable {
                        onFilterSelected(filter)
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
                        Icon(
                            painter = painterResource(id = R.drawable.filter),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )

                        if (filter.name != "None") {
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
                        text = filter.name,
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


