package com.appspy.ui.trend

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appspy.data.db.entity.AppMeta
import com.appspy.ui.theme.*

/**
 * 底部弹出的 App 选择器。
 * 支持搜索 + 多选。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorSheet(
    apps: List<AppMeta>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        var query by remember { mutableStateOf("") }

        val filtered = remember(query, apps) {
            if (query.isBlank()) apps
            else apps.filter { it.appLabel.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择应用", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(
                    "完成",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(Modifier.height(12.dp))

            // 搜索框
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Chip)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (query.isEmpty()) {
                            Text("搜索应用名称…", fontSize = 14.sp, color = InkSub)
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("没有找到相关应用", color = InkSub, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        val isSelected = app.packageName in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimarySoft else Color.Transparent)
                                .clickable { onToggle(app.packageName) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.appLabel,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Primary else Ink,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
