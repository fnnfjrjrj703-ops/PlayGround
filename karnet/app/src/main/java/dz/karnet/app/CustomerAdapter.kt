package dz.karnet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CustomerAdapter(
    private var items: List<Customer>,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tvName)
        val meta: TextView = v.findViewById(R.id.tvMeta)
        val balance: TextView = v.findViewById(R.id.tvBalance)
    }

    fun update(data: List<Customer>) {
        items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: Holder, position: Int) {
        val c = items[position]
        h.name.text = c.name
        val meta = buildString {
            if (c.phone.isNotBlank()) append(c.phone)
            if (c.balance <= 0.005) {
                if (isNotEmpty()) append(" • ")
                append(h.itemView.context.getString(R.string.all_settled))
            }
        }
        h.meta.text = meta
        h.meta.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE
        h.balance.text = Format.money(c.balance)
        val color = if (c.balance > 0.005) R.color.red_debt else R.color.green_primary
        h.balance.setTextColor(ContextCompat.getColor(h.itemView.context, color))
        h.itemView.setOnClickListener { onClick(c) }
    }
}
