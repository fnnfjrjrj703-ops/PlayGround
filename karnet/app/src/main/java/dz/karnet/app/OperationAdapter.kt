package dz.karnet.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class OperationAdapter(
    private var items: List<Operation>,
    private val onDelete: (Operation) -> Unit
) : RecyclerView.Adapter<OperationAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val badge: TextView = v.findViewById(R.id.tvBadge)
        val title: TextView = v.findViewById(R.id.tvOpTitle)
        val date: TextView = v.findViewById(R.id.tvOpDate)
        val amount: TextView = v.findViewById(R.id.tvOpAmount)
        val del: ImageButton = v.findViewById(R.id.btnDeleteOp)
    }

    fun update(data: List<Operation>) {
        items = data
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_operation, parent, false)
        return Holder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: Holder, position: Int) {
        val op = items[position]
        val ctx = h.itemView.context
        val isDebt = op.amount > 0
        h.badge.text = if (isDebt) "−" else "+"
        h.badge.background = ContextCompat.getDrawable(
            ctx,
            if (isDebt) R.drawable.bg_badge_debt else R.drawable.bg_badge_pay
        )
        h.title.text = if (op.note.isNotBlank()) {
            op.note
        } else {
            ctx.getString(if (isDebt) R.string.debt_label else R.string.payment_label)
        }
        h.date.text = Format.date(op.createdAt)
        h.amount.text = Format.signedMoney(op.amount)
        h.amount.setTextColor(
            ContextCompat.getColor(
                ctx, if (isDebt) R.color.red_debt else R.color.green_primary
            )
        )
        h.del.setOnClickListener { onDelete(op) }
    }
}
