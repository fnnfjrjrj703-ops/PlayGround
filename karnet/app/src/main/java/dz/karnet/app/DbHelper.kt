package dz.karnet.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) :
    SQLiteOpenHelper(context, "karnet.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE customers(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE operations(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                customer_id INTEGER NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                amount REAL NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX idx_ops_customer ON operations(customer_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // الإصدار الأول: لا ترقية بعد
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    fun addCustomer(name: String, phone: String, note: String): Long {
        val cv = ContentValues().apply {
            put("name", name.trim())
            put("phone", phone.trim())
            put("note", note.trim())
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("customers", null, cv)
    }

    fun deleteCustomer(id: Long): Int {
        val db = writableDatabase
        db.delete("operations", "customer_id=?", arrayOf(id.toString()))
        return db.delete("customers", "id=?", arrayOf(id.toString()))
    }

    fun getCustomers(query: String): List<Customer> {
        val list = mutableListOf<Customer>()
        val q = "%" + query.trim() + "%"
        val c = readableDatabase.rawQuery(
            """SELECT c.id, c.name, c.phone, c.note, c.created_at,
                      COALESCE(SUM(o.amount), 0)
               FROM customers c LEFT JOIN operations o ON o.customer_id = c.id
               WHERE c.name LIKE ? OR c.phone LIKE ?
               GROUP BY c.id ORDER BY c.name COLLATE NOCASE""",
            arrayOf(q, q)
        )
        c.use {
            while (it.moveToNext()) {
                list.add(
                    Customer(
                        id = it.getLong(0),
                        name = it.getString(1) ?: "",
                        phone = it.getString(2) ?: "",
                        note = it.getString(3) ?: "",
                        createdAt = it.getLong(4),
                        balance = it.getDouble(5)
                    )
                )
            }
        }
        return list
    }

    fun getCustomer(id: Long): Customer? {
        val c = readableDatabase.rawQuery(
            """SELECT c.id, c.name, c.phone, c.note, c.created_at,
                      COALESCE(SUM(o.amount), 0)
               FROM customers c LEFT JOIN operations o ON o.customer_id = c.id
               WHERE c.id=? GROUP BY c.id""",
            arrayOf(id.toString())
        )
        c.use {
            if (it.moveToFirst()) {
                return Customer(
                    id = it.getLong(0),
                    name = it.getString(1) ?: "",
                    phone = it.getString(2) ?: "",
                    note = it.getString(3) ?: "",
                    createdAt = it.getLong(4),
                    balance = it.getDouble(5)
                )
            }
        }
        return null
    }

    fun addOperation(customerId: Long, amount: Double, note: String): Long {
        require(amount != 0.0) { "amount must be non-zero" }
        val cv = ContentValues().apply {
            put("customer_id", customerId)
            put("amount", amount)
            put("note", note.trim())
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insertOrThrow("operations", null, cv)
    }

    fun deleteOperation(id: Long): Int =
        writableDatabase.delete("operations", "id=?", arrayOf(id.toString()))

    fun getOperations(customerId: Long): List<Operation> {
        val list = mutableListOf<Operation>()
        val c = readableDatabase.rawQuery(
            "SELECT id, customer_id, amount, note, created_at FROM operations " +
                "WHERE customer_id=? ORDER BY created_at DESC, id DESC",
            arrayOf(customerId.toString())
        )
        c.use {
            while (it.moveToNext()) {
                list.add(
                    Operation(
                        id = it.getLong(0),
                        customerId = it.getLong(1),
                        amount = it.getDouble(2),
                        note = it.getString(3) ?: "",
                        createdAt = it.getLong(4)
                    )
                )
            }
        }
        return list
    }

    fun getStats(): GlobalStats {
        var lent = 0.0
        var paid = 0.0
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(CASE WHEN amount>0 THEN amount ELSE 0 END),0)," +
                "COALESCE(SUM(CASE WHEN amount<0 THEN -amount ELSE 0 END),0) FROM operations",
            null
        ).use {
            if (it.moveToFirst()) {
                lent = it.getDouble(0)
                paid = it.getDouble(1)
            }
        }
        var customers = 0
        var debtors = 0
        readableDatabase.rawQuery(
            """SELECT COUNT(*),
                      COALESCE(SUM(CASE WHEN COALESCE(s.b,0)>0.005 THEN 1 ELSE 0 END),0)
               FROM customers c LEFT JOIN
                 (SELECT customer_id AS cid, SUM(amount) AS b FROM operations GROUP BY customer_id) s
               ON s.cid = c.id""",
            null
        ).use {
            if (it.moveToFirst()) {
                customers = it.getInt(0)
                debtors = it.getInt(1)
            }
        }
        return GlobalStats(
            totalDue = lent - paid,
            totalLent = lent,
            totalPaid = paid,
            customerCount = customers,
            debtorCount = debtors
        )
    }
}
