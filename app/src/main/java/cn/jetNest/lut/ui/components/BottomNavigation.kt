package cn.jetNest.lut.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    HOME("home", "首页", Icons.Default.Home),
    MANUAL_PROCESS("manual_process", "手动处理", Icons.Default.PhotoLibrary),
    PROCESSED_IMAGES("processed_images", "已处理图片", Icons.Default.List)
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BottomNavItem.values().forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}