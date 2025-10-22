package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SideControl(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    onPositionReady: ((Offset) -> Unit)? = null, // Optional callback
    iconTint: Color = Color.White,
    labelColor: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .then(
                if (onPositionReady != null) {
                    Modifier.onGloballyPositioned { layoutCoordinates ->
                        val position = layoutCoordinates.localToWindow(Offset.Zero)
                        val height = layoutCoordinates.size.height.toFloat()
                        onPositionReady(position + Offset(0f, height))
                    }
                } else Modifier
            )
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint =iconTint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}