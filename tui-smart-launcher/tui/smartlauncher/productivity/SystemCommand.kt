package tui.smartlauncher.productivity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.util.Log
import tui.smartlauncher.core.CommandHandler
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * System Command - Monitor and display system information
 * Shows CPU, memory, battery, storage, and process information
 */
class SystemCommand : CommandHandler {

    companion object {
        private const val TAG = "SystemCommand"
    }

    private val activityManager: ActivityManager by lazy { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    private val batteryManager: BatteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }
    private val connectivityManager: ConnectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    override fun getName(): String = "system"

    override fun getAliases(): List<String> = listOf("sys", "stats", "monitor", "mem", "cpu", "battery")

    override fun getDescription(): String = "Display system information and resource usage"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                  SYSTEM COMMANDS                      ║
        ╠══════════════════════════════════════════════════════╣
        ║  system              - Full system overview          ║
        ║  system --cpu        - CPU usage and info            ║
        ║  system --memory     - Memory usage                  ║
        ║  system --battery    - Battery status                ║
        ║  system --storage    - Storage information           ║
        ║  system --process    - Running processes             ║
        ║  system --network    - Network status                ║
        ║  system --all        - Complete system report        ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        this.context = context

        if (args.isEmpty() || args[0] == "--all" || args[0] == "-a") {
            return showFullOverview()
        }

        return when (args[0].lowercase()) {
            "--cpu", "-c" -> showCpuInfo()
            "--memory", "--mem", "-m" -> showMemoryInfo()
            "--battery", "-b" -> showBatteryInfo()
            "--storage", "-s" -> showStorageInfo()
            "--process", "--proc", "-p" -> showProcessInfo()
            "--network", "-n" -> showNetworkInfo()
            "--help", "-h" -> getUsage()
            else -> "Unknown option: ${args[0]}\n${getUsage()}"
        }
    }

    private var context: Context? = null

    /**
     * Shows complete system overview
     */
    private fun showFullOverview(): String {
        return buildString {
            appendLine()
            appendLine("══════════════════════════════════════════════════════════════")
            appendLine("                    SYSTEM OVERVIEW                           ")
            appendLine("══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine(showCpuInfo())
            appendLine(showMemoryInfo())
            appendLine(showBatteryInfo())
            appendLine(showStorageInfo())
            appendLine(showNetworkInfo())
        }
    }

    /**
     * Shows CPU information and usage
     */
    private fun showCpuInfo(): String {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            var cpuUsage = 0f

            // Try to read from /proc/stat
            try {
                val reader = BufferedReader(FileReader("/proc/stat"))
                val line = reader.readLine()
                reader.close()

                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val user = parts[1].toLongOrNull() ?: 0
                    val nice = parts[2].toLongOrNull() ?: 0
                    val system = parts[3].toLongOrNull() ?: 0
                    val idle = parts[4].toLongOrNull() ?: 0
                    val iowait = parts.getOrNull(5)?.toLongOrNull() ?: 0
                    val irq = parts.getOrNull(6)?.toLongOrNull() ?: 0
                    val softirq = parts.getOrNull(7)?.toLongOrNull() ?: 0

                    val total = user + nice + system + idle + iowait + irq + softirq
                    val active = total - idle - iowait
                    cpuUsage = if (total > 0) (active * 100f / total) else 0f
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read CPU stats: ${e.message}")
            }

            buildString {
                appendLine("┌─────────────────────────────────────────────────────────────┐")
                appendLine("│                        CPU INFO                             │")
                appendLine("├─────────────────────────────────────────────────────────────┤")
                appendLine(String.format("│  Cores: %-52d │", cores))
                appendLine(String.format("│  Usage: [%-50s] %.1f%% │", "█".repeat((cpuUsage / 2).toInt()), cpuUsage))
                appendLine(String.format("│  Architecture: %-46s │", System.getProperty("os.arch") ?: "Unknown"))
                appendLine(String.format("│  Android Runtime: %-44s │", System.getProperty("java.vm.name") ?: "Unknown"))
                appendLine("└─────────────────────────────────────────────────────────────┘")
            }
        } catch (e: Exception) {
            "Error getting CPU info: ${e.message}"
        }
    }

    /**
     * Shows memory usage
     */
    private fun showMemoryInfo(): String {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val totalMemory = memInfo.totalMem
            val availableMemory = memInfo.availMem
            val usedMemory = totalMemory - availableMemory
            val threshold = memInfo.threshold
            val isLowMemory = memInfo.lowMemory

            buildString {
                appendLine("┌─────────────────────────────────────────────────────────────┐")
                appendLine("│                      MEMORY INFO                            │")
                appendLine("├─────────────────────────────────────────────────────────────┤")
                appendLine(String.format("│  Total:     %-48s │", formatMemory(totalMemory)))
                appendLine(String.format("│  Used:      %-48s │", formatMemory(usedMemory)))
                appendLine(String.format("│  Available: %-48s │", formatMemory(availableMemory)))
                appendLine(String.format("│  Usage:     [%-50s] │", "█".repeat(((usedMemory * 50) / totalMemory).toInt())))
                appendLine(String.format("│  Threshold: %-48s │", formatMemory(threshold)))
                appendLine(String.format("│  Low Memory: %-47s │", if (isLowMemory) "YES ⚠️" else "No"))
                appendLine("└─────────────────────────────────────────────────────────────┘")
            }
        } catch (e: Exception) {
            "Error getting memory info: ${e.message}"
        }
    }

    /**
     * Shows battery status
     */
    private fun showBatteryInfo(): String {
        return try {
            val batteryIntent = context?.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val percentage = if (scale > 0) (level * 100 / scale) else level

            val status = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                else -> "Unknown"
            }

            val health = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Unknown"
            }

            val plugged = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Not Plugged"
            }

            val batteryTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempCelsius = batteryTemp / 10.0

            buildString {
                appendLine("┌─────────────────────────────────────────────────────────────┐")
                appendLine("│                      BATTERY INFO                           │")
                appendLine("├─────────────────────────────────────────────────────────────┤")
                appendLine(String.format("│  Level:    [%-50s] %d%% │", "█".repeat(percentage / 2), percentage))
                appendLine(String.format("│  Status:   %-48s │", status))
                appendLine(String.format("│  Health:   %-48s │", health))
                appendLine(String.format("│  Power:    %-48s │", plugged))
                appendLine(String.format("│  Temp:     %-48.1f°C │", tempCelsius))
                appendLine("└─────────────────────────────────────────────────────────────┘")
            }
        } catch (e: Exception) {
            "Error getting battery info: ${e.message}"
        }
    }

    /**
     * Shows storage information
     */
    private fun showStorageInfo(): String {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val totalBytes = stat.totalBytes
            val availableBytes = stat.availableBytes
            val usedBytes = totalBytes - availableBytes

            buildString {
                appendLine("┌─────────────────────────────────────────────────────────────┐")
                appendLine("│                      STORAGE INFO                           │")
                appendLine("├─────────────────────────────────────────────────────────────┤")
                appendLine(String.format("│  Total:    %-48s │", formatMemory(totalBytes)))
                appendLine(String.format("│  Used:     %-48s │", formatMemory(usedBytes)))
                appendLine(String.format("│  Free:     %-48s │", formatMemory(availableBytes)))
                appendLine(String.format("│  Usage:    [%-50s] │", "█".repeat(((usedBytes * 50) / totalBytes).toInt())))
                appendLine("└─────────────────────────────────────────────────────────────┘")
            }
        } catch (e: Exception) {
            "Error getting storage info: ${e.message}"
        }
    }

    /**
     * Shows network status
     */
    private fun showNetworkInfo(): String {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            val connected = capabilities != null
            val connectionType = when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                connected -> "Other"
                else -> "Disconnected"
            }

            val ipAddress = getLocalIpAddress()

            buildString {
                appendLine("┌─────────────────────────────────────────────────────────────┐")
                appendLine("│                      NETWORK INFO                           │")
                appendLine("├─────────────────────────────────────────────────────────────┤")
                appendLine(String.format("│  Status:   %-48s │", if (connected) "Connected" else "Disconnected"))
                appendLine(String.format("│  Type:     %-48s │", connectionType))
                appendLine(String.format("│  IP:       %-48s │", ipAddress))
                appendLine("└─────────────────────────────────────────────────────────────┘")
            }
        } catch (e: Exception) {
            "Error getting network info: ${e.message}"
        }
    }

    /**
     * Shows running processes
     */
    private fun showProcessInfo(): String {
        return try {
            val processes = activityManager.runningAppProcesses ?: emptyList()

            buildString {
                appendLine("┌─────────────────────────────────────────────────────────────┐")
                appendLine("│                     PROCESSES (${processes.size})                          │")
                appendLine("├─────────────────────────────────────────────────────────────┤")
                appendLine(String.format("│  %-40s %8s │", "PROCESS", "MEMORY"))
                appendLine("├─────────────────────────────────────────────────────────────┤")

                processes.sortedByDescending { it.memoryInfo?.dalvikPss?.plus(it.memoryInfo?.nativePss ?: 0) ?: 0 }
                    .take(15)
                    .forEach { process ->
                        val memory = process.memoryInfo?.let {
                            formatMemory((it.nativePss + it.dalvikPss) * 1024L)
                        } ?: "N/A"
                        appendLine(String.format("│  %-40s %8s │", process.processName.take(40), memory))
                    }

                if (processes.size > 15) {
                    appendLine(String.format("│  ... and %-44d │", processes.size - 15))
                }
                appendLine("└─────────────────────────────────────────────────────────────┘")
            }
        } catch (e: Exception) {
            "Error getting process info: ${e.message}"
        }
    }

    private fun formatMemory(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress) {
                        val ip = address.hostAddress
                        if (ip != null && !ip.contains(":")) {
                            return ip
                        }
                    }
                }
            }
            "Not available"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}

private fun IntentFilter(pattern: String): android.content.IntentFilter {
    return android.content.IntentFilter(pattern)
}
