package dz.karnet.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var db: DbHelper
    private lateinit var adapter: CustomerAdapter
    private lateinit var tvTotalDue: TextView
    private lateinit var tvDebtors: TextView
    private lateinit var tvCustomers: TextView
    private lateinit var tvStats: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var rv: RecyclerView
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = DbHelper(this)

        tvTotalDue = findViewById(R.id.tvTotalDue)
        tvDebtors = findViewById(R.id.tvDebtors)
        tvCustomers = findViewById(R.id.tvCustomers)
        tvStats = findViewById(R.id.tvStats)
        tvEmpty = findViewById(R.id.tvEmpty)
        rv = findViewById(R.id.rvCustomers)
        etSearch = findViewById(R.id.etSearch)

        adapter = CustomerAdapter(emptyList()) { c ->
            startActivity(
                Intent(this, CustomerDetailActivity::class.java)
                    .putExtra(CustomerDetailActivity.EXTRA_CUSTOMER_ID, c.id)
            )
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                refresh()
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddCustomerDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        try {
            val stats = db.getStats()
            tvTotalDue.text = Format.money(stats.totalDue)
            tvDebtors.text = stats.debtorCount.toString()
            tvCustomers.text = stats.customerCount.toString()
            tvStats.text = getString(R.string.total_lent) + ": " + Format.money(stats.totalLent) +
                " " + getString(R.string.currency_dzd) + "  •  " +
                getString(R.string.total_paid) + ": " + Format.money(stats.totalPaid) +
                " " + getString(R.string.currency_dzd)

            val q = etSearch.text?.toString() ?: ""
            val data = db.getCustomers(q)
            adapter.update(data)
            val empty = data.isEmpty()
            tvEmpty.visibility =
                if (empty) android.view.View.VISIBLE else android.view.View.GONE
            rv.visibility =
                if (empty) android.view.View.GONE else android.view.View.VISIBLE
            if (empty && q.isNotBlank()) {
                tvEmpty.text = getString(R.string.no_search_results)
            } else {
                tvEmpty.text = getString(R.string.no_customers)
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "خطأ", Toast.LENGTH_LONG).show()
        }
    }

    private fun showAddCustomerDialog() {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_customer, null)
        val etName: EditText = view.findViewById(R.id.etName)
        val etPhone: EditText = view.findViewById(R.id.etPhone)
        val etNote: EditText = view.findViewById(R.id.etNote)
        AlertDialog.Builder(this)
            .setTitle(R.string.add_customer)
            .setView(view)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { d ->
                d.setOnShowListener {
                    d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = etName.text.toString().trim()
                        if (name.isEmpty()) {
                            etName.error = getString(R.string.err_name_required)
                            return@setOnClickListener
                        }
                        try {
                            db.addCustomer(
                                name,
                                etPhone.text.toString(),
                                etNote.text.toString()
                            )
                            Toast.makeText(
                                this, R.string.msg_customer_added,
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
