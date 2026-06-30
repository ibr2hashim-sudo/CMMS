package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Asset
import com.example.data.model.AssetWithDetails
import com.example.data.model.Department
import com.example.data.model.TransferRecord
import com.example.data.model.TransferWithDetails
import com.example.data.repository.AssetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AssetRepository(
        database.assetDao(),
        database.departmentDao(),
        database.transferDao(),
        database.companyDao()
    )

    // Raw streams from Repository
    val departments: StateFlow<List<Department>> = repository.allDepartments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assetsWithDetails: StateFlow<List<AssetWithDetails>> = repository.assetsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transfersWithDetails: StateFlow<List<TransferWithDetails>> = repository.transfersWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering options
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "FIXED", "MOVABLE"
    val selectedTypeFilter = _selectedTypeFilter.asStateFlow()

    private val _selectedDepartmentFilter = MutableStateFlow<Int?>(null)
    val selectedDepartmentFilter = _selectedDepartmentFilter.asStateFlow()

    // Combined filtered assets flow
    val filteredAssets: StateFlow<List<AssetWithDetails>> = combine(
        assetsWithDetails,
        _searchQuery,
        _selectedTypeFilter,
        _selectedDepartmentFilter
    ) { assets, query, type, deptId ->
        assets.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.asset.name.contains(query, ignoreCase = true) ||
                item.asset.serialNumber.contains(query, ignoreCase = true)

            val matchesType = type == "ALL" || item.asset.type == type
            val matchesDept = deptId == null || item.asset.currentDepartmentId == deptId

            matchesQuery && matchesType && matchesDept
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed default departments if database is empty
        viewModelScope.launch {
            try {
                val depts = repository.allDepartments.first()
                if (depts.isEmpty()) {
                    val itId = repository.insertDepartment(Department(name = "تقنية المعلومات", code = "IT", description = "إدارة أجهزة الحاسوب والشبكات والدعم التقني"))
                    val hrId = repository.insertDepartment(Department(name = "الموارد البشرية", code = "HR", description = "شؤون الموظفين والتوظيف والتدريب"))
                    val finId = repository.insertDepartment(Department(name = "الإدارة المالية", code = "FIN", description = "المحاسبة والميزانيات والمعاملات والدفع"))
                    val whId = repository.insertDepartment(Department(name = "المستودع الرئيسي", code = "WH", description = "المركز الرئيسي لتخزين وتوزيع الأصول"))
                    val opsId = repository.insertDepartment(Department(name = "العمليات اللوجستية", code = "LOG", description = "إدارة أسطول النقل والمعدات الميدانية"))

                    // Seed some initial assets to demonstrate the application immediately
                    repository.insertAsset(
                        Asset(
                            id = "AST-SRV-01",
                            name = "خادم رئيسي Dell PowerEdge",
                            serialNumber = "SRV-MX928-11",
                            type = "FIXED",
                            description = "خادم قاعدة البيانات الرئيسي مجهز بذاكرة 128 جيجابايت وسعة تخزين سحابية محلية.",
                            purchaseDate = System.currentTimeMillis() - 31536000000L, // 1 year ago
                            currentDepartmentId = itId.toInt(),
                            status = "ACTIVE",
                            condition = "EXCELLENT",
                            model = "PowerEdge R750",
                            quantity = 2,
                            imageUri = null,
                            accessories = "",
                            manufacturer = ""
                        )
                    )

                    repository.insertAsset(
                        Asset(
                            id = "AST-LAP-02",
                            name = "حاسوب محمول MacBook Pro 16",
                            serialNumber = "MAC-PRO-0098",
                            type = "MOVABLE",
                            description = "جهاز محمول مخصص لمطوري النظم ومصممي الواجهات بالشركة.",
                            purchaseDate = System.currentTimeMillis() - 15768000000L, // 6 months ago
                            currentDepartmentId = itId.toInt(),
                            status = "ACTIVE",
                            condition = "NEW",
                            model = "MacBook Pro M3 Max",
                            quantity = 5,
                            imageUri = null,
                            accessories = "",
                            manufacturer = ""
                        )
                    )

                    repository.insertAsset(
                        Asset(
                            id = "AST-FUR-03",
                            name = "طاولة اجتماعات خشبية فاخرة",
                            serialNumber = "FURN-TB-104",
                            type = "FIXED",
                            description = "طاولة اجتماعات دائرية تتسع لـ 12 شخصاً بقاعة الاجتماعات الكبرى.",
                            purchaseDate = System.currentTimeMillis() - 94608000000L, // 3 years ago
                            currentDepartmentId = hrId.toInt(),
                            status = "ACTIVE",
                            condition = "GOOD",
                            model = "Premium Oak Table",
                            quantity = 1,
                            imageUri = null,
                            accessories = "",
                            manufacturer = ""
                        )
                    )

                    repository.insertAsset(
                        Asset(
                            id = "AST-DIS-04",
                            name = "شاشة عرض ذكية 75 بوصة Sony",
                            serialNumber = "TV-SONY-75A",
                            type = "MOVABLE",
                            description = "شاشة قاعة العروض والتدريب، مجهزة للاتصال اللاسلكي السريع.",
                            purchaseDate = System.currentTimeMillis() - 7884000000L, // 3 months ago
                            currentDepartmentId = whId.toInt(),
                            status = "ACTIVE",
                            condition = "EXCELLENT",
                            model = "Bravia XR 75",
                            quantity = 3,
                            imageUri = null,
                            accessories = "",
                            manufacturer = ""
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Filter adjustments
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun updateDepartmentFilter(deptId: Int?) {
        _selectedDepartmentFilter.value = deptId
    }

    // Actions
    fun addAsset(
        id: String,
        name: String,
        serialNumber: String,
        type: String,
        description: String,
        currentDepartmentId: Int,
        condition: String,
        model: String = "",
        quantity: Int = 1,
        imageUri: String? = null,
        accessories: String = "",
        manufacturer: String = "",
        status: String = "ACTIVE"
    ) {
        viewModelScope.launch {
            val asset = Asset(
                id = id,
                name = name,
                serialNumber = serialNumber,
                type = type,
                description = description,
                purchaseDate = System.currentTimeMillis(),
                currentDepartmentId = currentDepartmentId,
                status = status,
                condition = condition,
                model = model,
                quantity = quantity,
                imageUri = imageUri,
                accessories = accessories,
                manufacturer = manufacturer
            )
            repository.insertAsset(asset)
        }
    }

    // Excel-compatible CSV Importer
    fun importAssetsFromCsv(csvText: String, onComplete: (Int, String) -> Unit) {
        viewModelScope.launch {
            try {
                val lines = csvText.lines()
                if (lines.isEmpty() || lines.first().isBlank()) {
                    onComplete(0, "الملف فارغ أو غير صالح")
                    return@launch
                }
                
                // Get header and columns
                val header = lines.first().split(",").map { it.trim().removeSurrounding("\"") }
                
                var importedCount = 0
                val departmentsList = repository.allDepartments.first()
                val defaultDept = departmentsList.firstOrNull()
                val defaultDeptId = defaultDept?.id ?: 1
                
                // Map to store department name (trimmed lowercase) to its ID for avoiding duplicates
                val departmentsMap = departmentsList.associate { 
                    it.name.trim().lowercase() to it.id 
                }.toMutableMap()
                
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.isBlank()) continue
                    
                    // CSV column values
                    val tokens = line.split(",").map { it.trim().removeSurrounding("\"") }
                    if (tokens.isEmpty()) continue
                    
                    // Map indices dynamically based on header
                    fun getValueForHeader(vararg names: String): String {
                        val index = header.indexOfFirst { headerName -> 
                            names.any { it.equals(headerName, ignoreCase = true) }
                        }
                        return if (index != -1 && index < tokens.size) tokens[index] else ""
                    }
                    
                    val nameValue = getValueForHeader("الاسم", "name", "Device Name")
                    if (nameValue.isBlank()) continue
                    
                    val serial = getValueForHeader("الرقم التسلسلي", "serialNumber", "Serial Number")
                    val type = getValueForHeader("النوع", "type", "Type")
                    val description = getValueForHeader("الوصف", "description", "Notes")
                    val condition = getValueForHeader("الجودة", "condition", "Condition")
                    val model = getValueForHeader("الموديل", "model", "Model")
                    val quantity = getValueForHeader("الكمية", "quantity", "Quantity").toIntOrNull() ?: 1
                    val assetCode = getValueForHeader("كود تعريفي", "assetCode", "Asset ID").ifBlank { getValueForHeader("Asset ID", "id") }
                    val idValue = assetCode.ifBlank { System.currentTimeMillis().toString() + "-" + importedCount }
                    val accessories = getValueForHeader("الملحقات", "accessories", "Accessories")
                    val manufacturerIdOrName = getValueForHeader("الشركة المصنعة", "manufacturer", "Manufacturer")
                    val companyName = getValueForHeader("الشركة", "company", "Company")
                    
                    // Use company name if available, otherwise fallback to manufacturer
                    val manufacturer = if (companyName.isNotBlank()) companyName else manufacturerIdOrName
                    
                    // Check if department exists by name, otherwise use default
                    val deptName = getValueForHeader("القسم", "department", "Department").trim()
                    var deptId = defaultDeptId
                    if (deptName.isNotBlank()) {
                        val deptKey = deptName.lowercase()
                        if (departmentsMap.containsKey(deptKey)) {
                            deptId = departmentsMap[deptKey]!!
                        } else {
                            // Create department dynamically
                            val newDeptId = repository.insertDepartment(
                                Department(name = deptName, code = deptName.take(3).uppercase(), description = "تم إنشاؤه تلقائياً")
                            )
                            deptId = newDeptId.toInt()
                            departmentsMap[deptKey] = deptId
                        }
                    }
                    
                    val asset = Asset(
                        id = idValue,
                        name = nameValue,
                        serialNumber = serial,
                        type = type,
                        description = description,
                        purchaseDate = System.currentTimeMillis(),
                        currentDepartmentId = deptId,
                        status = "ACTIVE",
                        condition = condition,
                        model = model,
                        quantity = quantity,
                        imageUri = null,
                        accessories = accessories,
                        manufacturer = manufacturer
                    )
                    repository.insertAsset(asset)
                    importedCount++
                }
                onComplete(importedCount, "تم استيراد $importedCount من الأصول بنجاح!")
            } catch (e: Exception) {
                onComplete(0, "خطأ في قراءة ملف CSV: ${e.localizedMessage}")
            }
        }
    }

    // Excel-compatible CSV Exporter
    fun exportAssetsToCsv(): String {
        val builder = StringBuilder()
        // CSV Header
        builder.append("الاسم,كود تعريفي,الشركة المصنعة,الموديل,الرقم التسلسلي,النوع,التصنيف,الملحقات,الوصف,التكلفة,الكمية,الجودة,القسم\n")
        
        val assets = filteredAssets.value
        val depts = departments.value.associateBy { it.id }
        for (item in assets) {
            val name = item.asset.name.replace(",", " ")
            val code = item.asset.id.replace(",", " ")
            val mfg = item.asset.manufacturer.replace(",", " ")
            val model = item.asset.model.replace(",", " ")
            val serial = item.asset.serialNumber.replace(",", " ")
            val type = item.asset.type
            val acc = item.asset.accessories.replace(",", " ")
            val description = item.asset.description.replace(",", " ")
            val quantity = item.asset.quantity
            val condition = item.asset.condition
            val deptName = depts[item.asset.currentDepartmentId]?.name ?: "غير محدد"
            builder.append("\"$name\",\"$code\",\"$mfg\",\"$model\",\"$serial\",\"$type\",\"$acc\",\"$description\",$quantity,\"$condition\",\"$deptName\"\n")
        }
        return builder.toString()
    }

    // CSV Empty Template for Excel
    fun getCsvTemplate(): String {
        val builder = StringBuilder()
        builder.append("الاسم,كود تعريفي,الشركة المصنعة,الموديل,الرقم التسلسلي,النوع,التصنيف,الملحقات,الوصف,التكلفة,الكمية,الجودة,القسم\n")
        builder.append("\"طاولة مكتبية ذكية\",\"AST-1001\",\"IKEA\",\"IKEA-Desk-X\",\"SN-99238\",\"MOVABLE\",\"أثاث مكتب\",\"\",\"طاولة مكتبية قابلة لتعديل الارتفاع\",1250,5,\"NEW\",\"الموارد البشرية\"\n")
        builder.append("\"شاشة حاسوب 27 بوصة\",\"AST-1002\",\"LG\",\"LG-27UL\",\"SN-88231\",\"MOVABLE\",\"أجهزة إلكترونية\",\"كابل طاقة - كابل HDMI\",\"شاشة عرض بدقة 4K فائقة الوضوح\",1500,10,\"EXCELLENT\",\"تقنية المعلومات\"\n")
        return builder.toString()
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.updateAsset(asset)
        }
    }

    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    fun addDepartment(name: String, code: String, description: String) {
        viewModelScope.launch {
            repository.insertDepartment(
                Department(
                    name = name,
                    code = code,
                    description = description
                )
            )
        }
    }

    fun deleteDepartment(department: Department) {
        viewModelScope.launch {
            repository.deleteDepartment(department)
        }
    }

    fun transferAsset(
        assetId: String,
        fromDeptId: Int,
        toDeptId: Int,
        authorizedBy: String,
        notes: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val success = repository.transferAsset(
                assetId = assetId,
                fromDeptId = fromDeptId,
                toDeptId = toDeptId,
                authorizedBy = authorizedBy,
                notes = notes
            )
            onComplete(success)
        }
    }
}
