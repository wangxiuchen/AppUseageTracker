package com.appspy.ui.permission

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appspy.ui.theme.*
import com.appspy.util.PermissionUtil

/**
 * 首次进入时的授权引导页。
 * 对应原型图①：三步 checklist + CTA 按钮。
 */
@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(PermissionUtil.hasUsageStatsPermission(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── 图标 ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                BarChartIcon()
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "看清自己每天\n用了哪些应用",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "统计每个应用的打开次数和使用时长，\n全部数据只存在本机，不上传。",
                fontSize = 13.sp,
                color = InkSub,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(28.dp))

            // ── 三步 checklist ──────────────────────────────────────
            hasPermission = PermissionUtil.hasUsageStatsPermission(context)

            SetupRow(done = hasPermission, title = "使用情况访问权限", subtitle = "读取应用使用数据的前提")
            Spacer(Modifier.height(10.dp))
            SetupRow(done = false, title = "允许自启动", subtitle = "否则后台统计会被系统关闭")
            Spacer(Modifier.height(10.dp))
            SetupRow(done = false, title = "省电策略设为「无限制」", subtitle = "小米 / 华为等系统需手动放行")

            Spacer(Modifier.height(32.dp))

            // ── CTA 按钮 ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (hasPermission) GreenOk else Primary)
                    .clickable(enabled = !hasPermission) {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (hasPermission) "权限已开启 ✓" else "开启使用情况访问权限",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            if (!hasPermission) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "开启后回到本页，App 会自动进入",
                    fontSize = 12.sp,
                    color = InkSub,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SetupRow(done: Boolean, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (done) GreenOk else Color.White)
                .border(if (done) 0.dp else 1.8.dp, Divider, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(subtitle, fontSize = 11.sp, color = InkSub, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun BarChartIcon() {
    Canvas(modifier = Modifier.size(36.dp)) {
        val w = size.width
        val h = size.height
        val bw = w * 0.22f
        val gap = w * 0.12f
        val x0 = (w - 3 * bw - 2 * gap) / 2
        drawRect(Primary, Offset(x0, h * 0.50f), Size(bw, h * 0.50f))
        drawRect(Primary, Offset(x0 + bw + gap, h * 0.28f), Size(bw, h * 0.72f))
        drawRect(Primary, Offset(x0 + 2 * (bw + gap), h * 0.06f), Size(bw, h * 0.94f))
    }
}
