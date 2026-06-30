package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AssetsTab
import com.example.ui.screens.DashboardTab
import com.example.ui.screens.DepartmentsTab
import com.example.ui.screens.HistoryTab
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.AddDeviceScreen
import com.example.data.local.LocalImageStorageService
import kotlinx.coroutines.launch
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AssetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AssetViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageService = remember { LocalImageStorageService(context) }
    var currentScreen by remember { mutableStateOf("main") }

    // Intercept Back Press to show confirmation dialog
    BackHandler(enabled = true) {
        if (currentScreen == "add_device") {
            currentScreen = "main"
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "تأكيد الخروج",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في الخروج وإغلاق التطبيق؟",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("خروج", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Collect States from the ViewModel reactively
    val departments by viewModel.departments.collectAsState(initial = emptyList())
    val assetsWithDetails by viewModel.assetsWithDetails.collectAsState(initial = emptyList())
    val filteredAssets by viewModel.filteredAssets.collectAsState(initial = emptyList())
    val transfersWithDetails by viewModel.transfersWithDetails.collectAsState(initial = emptyList())

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedDeptFilter by viewModel.selectedDepartmentFilter.collectAsState()

    if (currentScreen == "add_device") {
        AddDeviceScreen(
            departments = departments,
            onNavigateBack = { currentScreen = "main" },
            onSaveClick = { assetId, deviceName, deptId, model, serial, quantity, status, notes, galleryUri, cameraBitmap ->
                coroutineScope.launch {
                    var localImagePath: String? = null
                    if (cameraBitmap != null) {
                        localImagePath = storageService.saveBitmapLocally(cameraBitmap, assetId)
                    } else if (galleryUri != null) {
                        localImagePath = storageService.saveImageLocally(galleryUri, assetId)
                    }
                    
                    // إضافة أصل جديد مع الصورة والبيانات
                    viewModel.addAsset(
                        name = deviceName,
                        serialNumber = serial.ifBlank { "SN-${System.currentTimeMillis() % 10000}" },
                        type = "BIOMEDICAL",
                        category = "أجهزة طبية",
                        description = notes,
                        cost = 0.0,
                        currentDepartmentId = deptId,
                        condition = "GOOD",
                        model = model,
                        quantity = quantity,
                        imageUri = localImagePath,
                        assetCode = assetId,
                        status = status
                    )
                    
                    currentScreen = "main"
                    selectedTab = 0 // العودة للوحة التحكم
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Geometric AM circular badge on left (corresponds to RTL layout)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                  Text(
                                      text = "AM",
                                      color = MaterialTheme.colorScheme.onPrimaryContainer,
                                      fontWeight = FontWeight.Bold,
                                      style = MaterialTheme.typography.bodyMedium
                                  )
                            }
                            Text(
                                text = "إدارة الأصول والعهد",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    actions = {
                        Box(modifier = Modifier.padding(end = 12.dp)) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "${assetsWithDetails.sumOf { it.asset.quantity }} أصل",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("لوحة التحكم", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = "Assets") },
                        label = { Text("سجل الأصول", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.MapsHomeWork, contentDescription = "Departments") },
                        label = { Text("الأقسام", style = MaterialTheme.typography.labelSmall) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("سجل الترانزيت", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        assets = assetsWithDetails,
                        onAddDeviceClick = { currentScreen = "add_device" },
                        onDeviceClick = { item ->
                            viewModel.updateSearchQuery(item.asset.assetCode)
                            viewModel.updateDepartmentFilter(item.asset.currentDepartmentId)
                            selectedTab = 1
                        }
                    )
                    1 -> AssetsTab(
                    assets = filteredAssets,
                    departments = departments,
                    transfers = transfersWithDetails,
                    searchQuery = searchQuery,
                    selectedType = selectedTypeFilter,
                    selectedDeptId = selectedDeptFilter,
                    onSearchChanged = { viewModel.updateSearchQuery(it) },
                    onTypeChanged = { viewModel.updateTypeFilter(it) },
                    onDeptIdChanged = { viewModel.updateDepartmentFilter(it) },
                    onAddAsset = { name, serial, type, cat, desc, cost, deptId, cond, model, qty, img, code, accessories, manufacturer ->
                        viewModel.addAsset(name, serial, type, cat, desc, cost, deptId, cond, model, qty, img, code, accessories, manufacturer)
                    },
                    onUpdateAsset = { viewModel.updateAsset(it) },
                    onDeleteAsset = { viewModel.deleteAsset(it) },
                    onTransferAsset = { assetId, fromDept, toDept, auth, note ->
                        viewModel.transferAsset(assetId, fromDept, toDept, auth, note)
                    },
                    onImportCsv = { csvText, onComplete ->
                        viewModel.importAssetsFromCsv(csvText, onComplete)
                    },
                    onExportCsv = {
                        viewModel.exportAssetsToCsv()
                    },
                    onGetTemplate = {
                        viewModel.getCsvTemplate()
                    }
                )
                2 -> DepartmentsTab(
                    departments = departments,
                    assets = assetsWithDetails,
                    onAddDepartment = { name, code, desc ->
                        viewModel.addDepartment(name, code, desc)
                    },
                    onDeleteDepartment = {
                        viewModel.deleteDepartment(it)
                    }
                )
                3 -> HistoryTab(
                    transfers = transfersWithDetails
                )
            }
        }
    }
    }
}
