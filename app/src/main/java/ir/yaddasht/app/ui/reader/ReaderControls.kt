package ir.yaddasht.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.yaddasht.app.ui.theme.LalezarFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    themeIndex: Int,
    fontSize: Int,
    columns: Int,
    isPdf: Boolean,
    onTheme: (Int) -> Unit,
    onFontSize: (Int) -> Unit,
    onColumns: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF202020)) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Text("تنظیمات مطالعه 📖", color = Color.White, fontFamily = LalezarFont, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))

            Text("تم صفحه", color = Color(0xFFB8B8B8), fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                val themeColors = listOf(Color(0xFFFFF8E1), Color(0xFFF4ECD8), Color(0xFF1A1A1A))
                val themeNames = listOf("روشن", "سپیا", "شب")
                themeColors.forEachIndexed { i, c ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    if (themeIndex == i) 3.dp else 1.dp,
                                    if (themeIndex == i) Color(0xFFFFB74D) else Color.Gray,
                                    CircleShape
                                )
                                .clickable { onTheme(i) }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(themeNames[i], color = Color(0xFFB8B8B8), fontSize = 11.sp)
                    }
                }
            }

            if (!isPdf) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("اندازه قلم", color = Color(0xFFB8B8B8), fontSize = 14.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onFontSize((fontSize - 2).coerceAtLeast(12)) }) { Text("−", color = Color.White, fontSize = 22.sp) }
                    Text("$fontSize", color = Color.White, fontSize = 16.sp)
                    IconButton(onClick = { onFontSize((fontSize + 2).coerceAtMost(28)) }) { Text("+", color = Color.White, fontSize = 22.sp) }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Text("حالت کتاب (دو صفحه‌ای) 📖", color = Color(0xFFB8B8B8), fontSize = 14.sp, modifier = Modifier.weight(1f))
                // ✅ اصلاح: استفاده از پارامترهای صحیح در Material3 جدید
                Switch(
                    checked = columns == 2,
                    onCheckedChange = { onColumns(if (it) 2 else 1) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFFFB74D),
                        checkedTrackColor = Color(0xFFFFB74D).copy(alpha = 0.4f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}
