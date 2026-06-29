package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val serialNumber: String,
    val type: String, // "FIXED" or "MOVABLE"
    val category: String,
    val description: String,
    val cost: Double,
    val purchaseDate: Long,
    val currentDepartmentId: Int,
    val status: String, // "ACTIVE", "MAINTENANCE", "SCRAPPED"
    val condition: String, // "NEW", "EXCELLENT", "GOOD", "POOR"
    val model: String = "",
    val quantity: Int = 1,
    val imageUri: String? = null,
    val assetCode: String = "",
    val accessories: String = "",
    val manufacturer: String = ""
)
