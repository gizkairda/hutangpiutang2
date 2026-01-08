package com.example.hutangpiutang2

import java.util.Date

data class Debt(
    var id: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var personName: String = "",
    var description: String = "", // Tambahkan properti ini
    var type: String = "hutang",
    var status: String = "aktif",
    var dueDate: Date? = null, // Tambahkan properti ini
    var timestamp: Long = System.currentTimeMillis(),
    var userId: String = ""
)