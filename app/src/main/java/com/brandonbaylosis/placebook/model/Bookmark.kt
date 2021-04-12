package com.brandonbaylosis.placebook.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// 1 Designates this as a data base entry class to Room
@Entity
// 2 Define primary constructor by using arguments for all properties with the default values defined
data class Bookmark(
// 3 Defines id property, autoGenerate automatically generates incrementing numbers for this field
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    // 4 Defined with default values
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = ""
)