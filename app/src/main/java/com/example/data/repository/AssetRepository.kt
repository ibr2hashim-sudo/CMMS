package com.example.data.repository

import com.example.data.local.AssetDao
import com.example.data.local.DepartmentDao
import com.example.data.local.TransferDao
import com.example.data.model.Asset
import com.example.data.model.AssetWithDetails
import com.example.data.model.Department
import com.example.data.model.TransferRecord
import com.example.data.model.TransferWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AssetRepository(
    private val assetDao: AssetDao,
    private val departmentDao: DepartmentDao,
    private val transferDao: TransferDao
) {
    val allAssets: Flow<List<Asset>> = assetDao.getAllAssets()
    val allDepartments: Flow<List<Department>> = departmentDao.getAllDepartments()
    val allTransfers: Flow<List<TransferRecord>> = transferDao.getAllTransfers()

    val assetsWithDetails: Flow<List<AssetWithDetails>> = combine(
        assetDao.getAllAssets(),
        departmentDao.getAllDepartments()
    ) { assets, departments ->
        val deptMap = departments.associateBy { it.id }
        assets.map { asset ->
            val dept = deptMap[asset.currentDepartmentId]
            AssetWithDetails(
                asset = asset,
                departmentName = dept?.name ?: "غير محدد",
                departmentCode = dept?.code ?: "N/A"
            )
        }
    }

    val transfersWithDetails: Flow<List<TransferWithDetails>> = combine(
        transferDao.getAllTransfers(),
        assetDao.getAllAssets(),
        departmentDao.getAllDepartments()
    ) { transfers, assets, departments ->
        val assetMap = assets.associateBy { it.id }
        val deptMap = departments.associateBy { it.id }
        transfers.map { record ->
            val asset = assetMap[record.assetId]
            val fromDept = deptMap[record.fromDepartmentId]
            val toDept = deptMap[record.toDepartmentId]
            TransferWithDetails(
                record = record,
                assetName = asset?.name ?: "أصل محذوف أو مجهول",
                assetSerialNumber = asset?.serialNumber ?: "N/A",
                fromDeptName = fromDept?.name ?: "غير محدد",
                fromDeptCode = fromDept?.code ?: "N/A",
                toDeptName = toDept?.name ?: "غير محدد",
                toDeptCode = toDept?.code ?: "N/A"
            )
        }
    }

    suspend fun insertAsset(asset: Asset): Long {
        return assetDao.insertAsset(asset)
    }

    suspend fun updateAsset(asset: Asset) {
        assetDao.updateAsset(asset)
    }

    suspend fun deleteAsset(asset: Asset) {
        assetDao.deleteAsset(asset)
    }

    suspend fun insertDepartment(department: Department): Long {
        return departmentDao.insertDepartment(department)
    }

    suspend fun deleteDepartment(department: Department) {
        departmentDao.deleteDepartment(department)
    }

    suspend fun transferAsset(
        assetId: Int,
        fromDeptId: Int,
        toDeptId: Int,
        authorizedBy: String,
        notes: String
    ): Boolean {
        val asset = assetDao.getAssetById(assetId) ?: return false
        val updatedAsset = asset.copy(currentDepartmentId = toDeptId)
        assetDao.updateAsset(updatedAsset)

        val record = TransferRecord(
            assetId = assetId,
            fromDepartmentId = fromDeptId,
            toDepartmentId = toDeptId,
            transferDate = System.currentTimeMillis(),
            authorizedBy = authorizedBy,
            notes = notes
        )
        transferDao.insertTransfer(record)
        return true
    }
}
