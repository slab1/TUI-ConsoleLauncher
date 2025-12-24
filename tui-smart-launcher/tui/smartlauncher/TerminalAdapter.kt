package tui.smartlauncher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView Adapter for Terminal Display
 * Handles command input and output rendering with terminal styling
 */
class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

    private val items = mutableListOf<TerminalItem>()

    sealed class TerminalItem {
        data class Input(val text: String) : TerminalItem()
        data class Output(val text: String) : TerminalItem()
        data class Error(val text: String) : TerminalItem()
        data class Info(val text: String) : TerminalItem()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TerminalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_terminal_line, parent, false)
        return TerminalViewHolder(view)
    }

    override fun onBindViewHolder(holder: TerminalViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addInput(text: String) {
        items.add(TerminalItem.Input(text))
        notifyItemInserted(items.size - 1)
    }

    fun addOutput(text: String) {
        items.add(TerminalItem.Output(text))
        notifyItemInserted(items.size - 1)
    }

    fun addError(text: String) {
        items.add(TerminalItem.Error(text))
        notifyItemInserted(items.size - 1)
    }

    fun addInfo(text: String) {
        items.add(TerminalItem.Info(text))
        notifyItemInserted(items.size - 1)
    }

    fun clearHistory() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    class TerminalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.terminal_text)

        fun bind(item: TerminalItem) {
            when (item) {
                is TerminalItem.Input -> {
                    textView.text = "> ${item.text}"
                    textView.setTextColor(Color.parseColor("#00FF00")) // Green for input
                }
                is TerminalItem.Output -> {
                    textView.text = item.text
                    textView.setTextColor(Color.WHITE)
                }
                is TerminalItem.Error -> {
                    textView.text = "ERROR: ${item.text}"
                    textView.setTextColor(Color.parseColor("#FF5252")) // Red for errors
                }
                is TerminalItem.Info -> {
                    textView.text = item.text
                    textView.setTextColor(Color.parseColor("#2196F3")) // Blue for info
                }
            }
        }
    }
}
