package com.mdq.ruangguru

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ================= WARNA PASTEL & TEMA =================

val BgCream = Color(0xFFFBF8F2)
val SidebarSage = Color(0xFFE4EDE4)
val SidebarItemActive = Color(0xFFFFFFFF)
val TextDarkBrown = Color(0xFF382E2B)
val TextMuted = Color(0xFF7E7672)

val BannerCoral = Color(0xFFF9957F)
val BannerBgPeach = Color(0xFFFEECE5)
val CardBgWhite = Color(0xFFFFFFFF)

val StatTealBg = Color(0xFFE1F3F0)
val StatTealIcon = Color(0xFF5CBBA7)
val StatOrangeBg = Color(0xFFFCE9E2)
val StatOrangeIcon = Color(0xFFF38A6B)
val StatYellowBg = Color(0xFFFEF6E4)
val StatYellowIcon = Color(0xFFF2BE54)
val StatBlueBg = Color(0xFFE5F1FC)
val StatBlueIcon = Color(0xFF71AEEA)

val ColorHadir = Color(0xFF5AB69F)
val ColorSakit = Color(0xFFF2BE54)
val ColorIzin = Color(0xFF71AEEA)
val ColorAlfa = Color(0xFFE77C6B)

val MDQTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = TextDarkBrown),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDarkBrown),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextDarkBrown),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp, color = TextDarkBrown),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = TextMuted)
)

@Composable
fun MDQTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BannerCoral,
            onPrimary = CardBgWhite,
            secondary = StatTealIcon,
            background = BgCream,
            surface = CardBgWhite,
            onBackground = TextDarkBrown,
            onSurface = TextDarkBrown
        ),
        typography = MDQTypography,
        content = content
    )
}

// ================= KOMPONEN UI & GRAFIK =================

data class NavItem(val title: String, val icon: ImageVector, val route: String)

@Composable
fun AppSidebar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onQuickJournalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("Dashboard", Icons.Default.Home, "dashboard"),
        NavItem("Data Kelas", Icons.Default.MeetingRoom, "kelas"),
        NavItem("Data Siswa", Icons.Default.Person, "siswa"),
        NavItem("Absensi Siswa", Icons.Default.CheckCircleOutline, "absensi"),
        NavItem("Nilai & Leger", Icons.Default.Grade, "nilai"),
        NavItem("Jurnal Mengajar", Icons.Default.MenuBook, "jurnal"),
        NavItem("Pengaturan", Icons.Default.Settings, "pengaturan")
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(250.dp)
            .background(SidebarSage)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp, top = 6.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFD3E4DA)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = StatTealIcon, modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Hi, Admin Guru! 👋", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Administrator", fontSize = 11.sp, color = TextMuted)
                }
            }

            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) SidebarItemActive else Color.Transparent)
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(item.icon, contentDescription = item.title, tint = if (isSelected) StatTealIcon else TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp, color = if (isSelected) TextDarkBrown else TextMuted)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BannerBgPeach),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👑 Jurnal Hari Ini", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDarkBrown)
                Text("Catat KBM kelas dengan rapi!", fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(vertical = 4.dp))
                Button(
                    onClick = onQuickJournalClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BannerCoral),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("+ Isi Jurnal Cepat", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBgWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDarkBrown)
            Text(subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
fun WeeklyBarChart(weeklyCounts: List<Float>, modifier: Modifier = Modifier) {
    val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val barColors = listOf(BannerCoral, StatYellowIcon, StatTealIcon, BannerCoral, StatBlueIcon, Color(0xFFC4B8EB), Color(0xFFE89BB6))
    val maxVal = (weeklyCounts.maxOrNull() ?: 10f).coerceAtLeast(5f)

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBgWhite), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fill
