package tui.smartlauncher

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Theme colors for the terminal display.
 * Controls the color scheme of all terminal text elements.
 */
data class ThemeColors(
    val inputColor: Int = Color.parseColor("#00FF00"),
    val outputColor: Int = Color.WHITE,
    val errorColor: Int = Color.parseColor("#FF5252"),
    val infoColor: Int = Color.parseColor("#2196F3"),
    val backgroundColor: Int = Color.BLACK
)

/**
 * RecyclerView Adapter for Terminal Display
 * Handles command input and output rendering with terminal styling
 */
class TerminalAdapter : RecyclerView.Adapter<TerminalAdapter.TerminalViewHolder>() {

    private val items = mutableListOf<TerminalItem>()

    /**
     * Current theme colors applied to the terminal.
     * When changed, the entire list is refreshed to reflect new colors.
     */
    var theme: ThemeColors = ThemeColors()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

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
        holder.bind(items[position], theme)
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

    fun addItem(item: TerminalItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun getItems(): List<TerminalItem> = items.toList()

    /**
     * Convenience method to update the terminal theme and refresh the display.
     */
    fun setTheme(colors: ThemeColors) {
        theme = colors
    }

    fun clearHistory() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }

    companion object {
        private val gson: Gson by lazy {
            GsonBuilder()
                .registerTypeAdapter(TerminalItem::class.java, TerminalItemTypeAdapter())
                .create()
        }

        fun itemsToJson(items: List<TerminalItem>): String = gson.toJson(items)

        fun itemsFromJson(json: String): List<TerminalItem>? {
            return try {
                val type = object : TypeToken<List<TerminalItem>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                null
            }
        }

        private class TerminalItemTypeAdapter : JsonSerializer<TerminalItem>, JsonDeserializer<TerminalItem> {
            override fun serialize(src: TerminalItem, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                val json = JsonObject()
                when (src) {
                    is TerminalItem.Input -> {
                        json.addProperty("type", "input")
                        json.addProperty("text", src.text)
                    }
                    is TerminalItem.Output -> {
                        json.addProperty("type", "output")
                        json.addProperty("text", src.text)
                    }
                    is TerminalItem.Error -> {
                        json.addProperty("type", "error")
                        json.addProperty("text", src.text)
                    }
                    is TerminalItem.Info -> {
                        json.addProperty("type", "info")
                        json.addProperty("text", src.text)
                    }
                }
                return json
            }

            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TerminalItem {
                val obj = json.asJsonObject
                val itemType = obj.get("type").asString
                val text = obj.get("text").asString
                return when (itemType) {
                    "input" -> TerminalItem.Input(text)
                    "output" -> TerminalItem.Output(text)
                    "error" -> TerminalItem.Error(text)
                    "info" -> TerminalItem.Info(text)
                    else -> throw JsonParseException("Unknown TerminalItem type: $itemType")
                }
            }
        }
    }

    class TerminalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.terminal_text)

        fun bind(item: TerminalItem, theme: ThemeColors) {
            when (item) {
                is TerminalItem.Input -> {
                    textView.text = "> ${item.text}"
                    textView.setTextColor(theme.inputColor)
                }
                is TerminalItem.Output -> {
                    textView.text = item.text
                    textView.setTextColor(theme.outputColor)
                }
                is TerminalItem.Error -> {
                    textView.text = "ERROR: ${item.text}"
                    textView.setTextColor(theme.errorColor)
                }
                is TerminalItem.Info -> {
                    textView.text = item.text
                    textView.setTextColor(theme.infoColor)
                }
            }
        }
    }
}
