package dz.karnet.app

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val arDz = Locale("ar", "DZ")
    private val amountFmt: NumberFormat = NumberFormat.getNumberInstance(arDz).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", arDz)

    fun money(v: Double): String = amountFmt.format(v)

    fun date(ts: Long): String = dateFmt.format(Date(ts))

    fun signedMoney(amount: Double): String =
        if (amount >= 0) "+${money(amount)}" else "−${money(-amount)}"
}
