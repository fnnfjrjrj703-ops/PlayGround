package dz.karnet.app

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CustomerDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CUSTOMER_ID = "customer_id"
    }

    private lateinit var db: DbHelper
    private lateinit var adapter: OperationAdapter
    private var customerId: Long = -1

    private lateinit var tvName: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvEmptyOps: TextView
    private lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_detail)
        db = DbHelper(this)
        customerId = intent.getLongExtra(EXTRA_CUSTOMER_ID, -1)
        if (customerId < 0) {
            finish()
            return
        }

        tvName = findViewById(R.id.tvDetailName)
        tvInfo = findViewById(R.id.tvDetailInfo)
        tvBalance = findViewById(R.id.tvDetailBalance)
        tvEmptyOps = findViewById(R.id.tvEmptyOps)
        rv = findViewById(R.id.rvOperations)

        adapter = OperationAdapter(emptyList()) { op ->
            AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.ask_delete_operation)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    try {
                        db.deleteOperation(op.id)
                        Toast.makeText(
                            this, R.string.msg_operation_deleted,
                            Toast.LENGTH_SHORT
                        ).show()
                        refresh()
                    } catch (e: Exception) {
                        Toast.makeText(this, e.message ?: "خطأ", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAddDebt).setOnClickListener {
            showTransactionDialog(isDebt = true)
        }
        findViewById<Button>(R.id.btnAddPayment).setOnClickListener {
            showTransactionDialog(isDebt = false)
        }
        findViewById<Button>(R.id.btnDeleteCustomer).setOnClickListener {
            val c = db.getCustomer(customerId) ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_customer)
                .setMessage(getString(R.string.ask_delete_customer, c.name))
                .setPositiveButton(R.string.confirm) { _, _ ->
                    try {
                        db.deleteCustomer(customerId)
                        Toast.makeText(
                            this, R.string.msg_customer_deleted,
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } catch (e: Exception) {
                        Toast.makeText(this, e.message ?: "خطأ", Toast.LENGTH_LONG)
                            .show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        try {
            val c = db.getCustomer(customerId)
            if (c == null) {
                finish()
                return
            }
            tvName.text = c.name
            val info = buildString {
                if (c.phone.isNotBlank()) {
                    append(getString(R.string.phone_label)).append(" ").append(c.phone)
                }
                if (c.note.isNotBlank()) {
                    if (isNotEmpty()) append("  •  ")
                    append(getString(R.string.note_label)).append(" ").append(c.note)
                }
            }
            tvInfo.text = info
            tvInfo.visibility =
                if (info.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            tvBalance.text = Format.money(c.balance)

            val ops = db.getOperations(customerId)
            adapter.update(ops)
            tvEmptyOps.visibility =
                if (ops.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "خطأ", Toast.LENGTH_LONG).show()
        }
    }

    private fun showTransactionDialog(isDebt: Boolean) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_transaction, null)
        val etAmount: EditText = view.findViewById(R.id.etAmount)
        val etDesc: EditText = view.findViewById(R.id.etDesc)
        AlertDialog.Builder(this)
            .setTitle(if (isDebt) R.string.add_debt else R.string.add_payment)
            .setView(view)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { d ->
                d.setOnShowListener {
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val raw = etAmount.text.toString().trim().replace(",", ".")
                        val value = raw.toDoubleOrNull()
                        if (value == null || value <= 0) {
                            etAmount.error = getString(R.string.err_amount_invalid)
                            return@setOnClickListener
                        }
                        if (!isDebt) {
                            val current = db.getCustomer(customerId)?.balance ?: 0.0
                            if (value - current > 0.005) {
                                etAmount.error =
                                    getString(R.string.err_payment_exceeds)
                                return@setOnClickListener
                            }
                        }
                        try {
                            val signed = if (isDebt) value else -value
                            db.addOperation(
                                customerId, signed,
                                etDesc.text.toString()
                            )
                            Toast.makeText(
                                this,
                                if (isDebt) R.string.msg_debt_added
                                else R.string.msg_payment_added,
                                Toast.LENGTH_SHORT
                            ).show()
                            d.dismiss()
                            refresh()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this, e.message ?: "خطأ",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                d.show()
            }
    }
}
