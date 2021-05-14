package edu.scut.generator.database

import android.os.SystemClock
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Generator(
    @PrimaryKey(autoGenerate = true) var _id: Int? = null,
    @ColumnInfo(name = "uuid") var uuid: String = "",
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "create_time") var createTime: Long = -1,
    @ColumnInfo(name = "last_connect_time") var lastConnectTime: Long = -1
)
