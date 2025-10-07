import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun TimerGridPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    selectedTimer: Int,
    onTimerSelected: (Int) -> Unit,
    anchorPosition: Offset
) {
    if (expanded) {
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = anchorPosition.x.toInt(),
                y = anchorPosition.y.toInt()
            ),
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .background(Color(0xDA1C1C1E), RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                val timerOptions = listOf(
                    15 to "15s",
                    30 to "30s",
                    60 to "60s",
                    120 to "2m"
                )

                // Manual 2x2 grid (chunk list in pairs)
                for (row in timerOptions.chunked(2)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { (seconds, label) ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .weight(1f)
                                    .background(
                                        if (selectedTimer == seconds) Color.White else Color.DarkGray,
                                        RoundedCornerShape(40.dp)
                                    )
                                    .clickable {
                                        onTimerSelected(seconds)
                                        onDismissRequest()
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = label,
                                        color =  if (selectedTimer == seconds) Color.Red else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (selectedTimer == seconds) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        if (row.size < 2) {
                            Spacer(modifier = Modifier.weight(1f)) // fill space if row has 1 item
                        }
                    }
                }
            }
        }
    }
}
