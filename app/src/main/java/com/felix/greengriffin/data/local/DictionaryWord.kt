package com.felix.greengriffin.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dictionary",
    indices = [
        Index(value = ["word", "language"], unique = true),
        Index(value = ["language"]),
        Index(value = ["length"]),
        Index(value = ["first_letter"])
    ]
)
data class DictionaryWord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "language")
    val language: String,

    @ColumnInfo(name = "length")
    val length: Int,

    @ColumnInfo(name = "first_letter")
    val firstLetter: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long? = null
)
