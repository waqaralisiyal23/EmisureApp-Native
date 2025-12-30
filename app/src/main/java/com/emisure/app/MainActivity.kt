package com.emisure.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emisure.app.ui.theme.*

class MainActivity : ComponentActivity() {
    
    private lateinit var devicePolicyService: DevicePolicyService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        devicePolicyService = DevicePolicyService(this)
        
        // Auto-apply security settings if Device Owner
        if (devicePolicyService.isDeviceOwner()) {
            devicePolicyService.disableFactoryReset()
            devicePolicyService.disableSafeModeBoot()
            devicePolicyService.lockNotificationPermission()
            devicePolicyService.enforceNotificationsEnabled()
        }
        
        enableEdgeToEdge()
        setContent {
            EmisureTheme {
                var refreshTrigger by remember { mutableIntStateOf(0) }
                
                EmisureDashboard(
                    devicePolicyService = devicePolicyService,
                    refreshTrigger = refreshTrigger,
                    onRefresh = { refreshTrigger++ }
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (::devicePolicyService.isInitialized && devicePolicyService.isDeviceOwner()) {
            devicePolicyService.enforceNotificationsEnabled()
        }
    }
}

// Modern color palette
private val PrimaryBlue = Color(0xFF4F46E5)
private val PrimaryPurple = Color(0xFF7C3AED)
private val SuccessGreen = Color(0xFF10B981)
private val WarningAmber = Color(0xFFF59E0B)
private val ErrorRed = Color(0xFFEF4444)
private val CardLight = Color(0xFFFFFFFF)
private val CardDark = Color(0xFF374151)  // Much lighter card for dark mode
private val SurfaceLight = Color(0xFFF8FAFC)
private val SurfaceDark = Color(0xFF1F2937)  // Darker background for contrast
private val TextPrimaryDark = Color(0xFFFFFFFF)
private val TextSecondaryDark = Color(0xFFD1D5DB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmisureDashboard(
    devicePolicyService: DevicePolicyService,
    refreshTrigger: Int,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    
    // State
    var isDeviceOwner by remember { mutableStateOf(false) }
    var isProtected by remember { mutableStateOf(false) }
    var fcmToken by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var deviceModel by remember { mutableStateOf("") }
    var manufacturer by remember { mutableStateOf("") }
    var androidId by remember { mutableStateOf("") }
    var imei1 by remember { mutableStateOf("") }
    var imei2 by remember { mutableStateOf("") }
    
    // Load status
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        isDeviceOwner = devicePolicyService.isDeviceOwner()
        isProtected = devicePolicyService.isFactoryResetDisabled()
        
        val status = devicePolicyService.getDeviceStatus()
        deviceModel = status["deviceModel"] as? String ?: ""
        manufacturer = status["manufacturer"] as? String ?: ""
        
        // Get device identifiers
        val identifiers = devicePolicyService.getDeviceIdentifiers()
        androidId = identifiers["androidId"] ?: ""
        imei1 = identifiers["imei1"] ?: ""
        imei2 = identifiers["imei2"] ?: ""
        
        fcmToken = EmisureFCMService.getStoredToken(context)
        if (fcmToken == null) {
            EmisureFCMService.refreshToken(context) { token ->
                fcmToken = token
            }
        }
        
        isLoading = false
    }
    
    val backgroundColor = if (isDarkTheme) SurfaceDark else SurfaceLight
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryBlue, PrimaryPurple)
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Emisure",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDeviceOwner) "Protection Active" else "Setup Required",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isDeviceOwner) SuccessGreen else WarningAmber)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isDeviceOwner) "Active" else "Inactive",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Protection Status Card
                    ProtectionStatusCard(
                        isProtected = isDeviceOwner && isProtected,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // Device Info Card
                    DeviceCard(
                        manufacturer = manufacturer,
                        model = deviceModel,
                        androidId = androidId,
                        imei1 = imei1,
                        imei2 = imei2,
                        isDarkTheme = isDarkTheme
                    )
                    
                    // Device Token Card (FCM Token - for registration)
                    DeviceIdCard(
                        deviceId = fcmToken,
                        isDarkTheme = isDarkTheme
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Footer
                    Text(
                        text = "Your device is protected by Emisure",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProtectionStatusCard(
    isProtected: Boolean,
    isDarkTheme: Boolean
) {
    val cardColor = if (isDarkTheme) CardDark else CardLight
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (isProtected) SuccessGreen.copy(alpha = 0.15f)
                        else WarningAmber.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isProtected) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isProtected) SuccessGreen else WarningAmber,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isProtected) "Device Protected" else "Protection Inactive",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isDarkTheme) TextPrimaryDark else Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isProtected) 
                        "All security features are enabled" 
                    else 
                        "Device owner setup required",
                    fontSize = 14.sp,
                    color = if (isDarkTheme) TextSecondaryDark else Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    manufacturer: String,
    model: String,
    androidId: String,
    imei1: String,
    imei2: String,
    isDarkTheme: Boolean
) {
    val cardColor = if (isDarkTheme) CardDark else CardLight
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Device Information",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (isDarkTheme) TextPrimaryDark else Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            InfoRow(
                label = "Manufacturer",
                value = manufacturer.replaceFirstChar { it.uppercase() },
                isDarkTheme = isDarkTheme
            )
            InfoRow(
                label = "Model",
                value = model,
                isDarkTheme = isDarkTheme
            )
            InfoRow(
                label = "Android ID",
                value = androidId.ifEmpty { "Loading..." },
                isDarkTheme = isDarkTheme,
                showCopyButton = androidId.isNotEmpty()
            )
            
            val isImei1Valid = imei1.isNotEmpty() && imei1 != "Not available"
            InfoRow(
                label = "IMEI 1",
                value = imei1.ifEmpty { "Loading..." },
                isDarkTheme = isDarkTheme,
                showCopyButton = isImei1Valid
            )
            
            val isImei2Valid = imei2.isNotEmpty() && imei2 != "Not available"
            InfoRow(
                label = "IMEI 2",
                value = imei2.ifEmpty { "Loading..." },
                isDarkTheme = isDarkTheme,
                showCopyButton = isImei2Valid
            )
        }
    }
}

@Composable
fun DeviceIdCard(
    deviceId: String?,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val cardColor = if (isDarkTheme) CardDark else CardLight
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Device Token",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = if (isDarkTheme) TextPrimaryDark else Color.Black
                    )
                }
                
                if (deviceId != null) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Device Token", deviceId)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Device Token copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (deviceId != null) {
                Text(
                    text = deviceId.take(40) + "...",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f),
                    lineHeight = 18.sp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generating device ID...",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isDarkTheme: Boolean,
    showCopyButton: Boolean = false
) {
    val context = LocalContext.current
    val valueColor = if (isDarkTheme) TextPrimaryDark else Color.Black
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = if (isDarkTheme) TextSecondaryDark else Color.Black.copy(alpha = 0.5f)
        )
        
        if (showCopyButton) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDarkTheme) Color.White.copy(alpha = 0.08f) 
                        else Color.Black.copy(alpha = 0.05f)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .then(
                        Modifier.clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(label, value)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = valueColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = if (isDarkTheme) TextSecondaryDark else Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}