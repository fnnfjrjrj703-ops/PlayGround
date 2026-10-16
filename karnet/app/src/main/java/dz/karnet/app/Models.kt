package dz.karnet.app

data class Customer(
    val id: Long,
    val name: String,
    val phone: String,
    val note: String,
    val createdAt: Long,
    val balance: Double
)

data class Operation(
    val id: Long,
    val customerId: Long,
    /** موجب = دين، سالب = دفعة */
    val amount: Double,
    val note: String,
    val createdAt: Long
)

data class GlobalStats(
    val totalDue: Double,
    val totalLent: Double,
    val totalPaid: Double,
    val customerCount: Int,
    val debtorCount: Int
)
