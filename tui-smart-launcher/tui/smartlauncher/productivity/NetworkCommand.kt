package tui.smartlauncher.productivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import tui.smartlauncher.core.CommandHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Network Command - Network diagnostic tools
 * Supports ping, DNS lookup, port scan, and connectivity checks
 */
class NetworkCommand : CommandHandler {

    companion object {
        private const val TAG = "NetworkCommand"
        private const val DEFAULT_TIMEOUT = 3000
    }

    override fun getName(): String = "network"

    override fun getAliases(): List<String> = listOf("net", "ping", "dns", "port", "ip")

    override fun getDescription(): String = "Network diagnostic and connectivity tools"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                  NETWORK COMMANDS                     ║
        ╠══════════════════════════════════════════════════════╣
        ║  network ping <host>         - Ping a host           ║
        ║  network dns <domain>        - Resolve DNS           ║
        ║  network port <host> <port>  - Check port            ║
        ║  network scan <host> <ports> - Scan port range       ║
        ║  network localip             - Show local IP         ║
        ║  network publicip            - Show public IP        ║
        ║  network whois <domain>      - WHOIS lookup          ║
        ║  network curl <url>          - HTTP request          ║
        ║  network speed               - Speed test            ║
        ║  network status              - Connection status     ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty() || args[0] == "--help" || args[0] == "-h") {
            return getUsage()
        }

        val command = args[0].lowercase()
        val parameters = args.drop(1)

        return when (command) {
            "ping" -> pingHost(parameters)
            "dns", "resolve" -> dnsLookup(parameters)
            "port" -> checkPort(parameters)
            "scan" -> portScan(parameters)
            "localip", "ip" -> showLocalIp()
            "publicip", "external" -> showPublicIp()
            "whois" -> whoisLookup(parameters)
            "curl", "http", "get" -> httpRequest(parameters)
            "speed", "test" -> speedTest(context)
            "status", "check" -> networkStatus(context)
            else -> "Unknown network command: $command\n${getUsage()}"
        }
    }

    /**
     * Pings a host
     */
    private fun pingHost(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: network ping <host> [count]"
        }

        val host = args[0]
        val count = args.getOrNull(1)?.toIntOrNull() ?: 4

        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("PING $host ($count pings)")
            builder.appendLine("─".repeat(50))

            val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", count.toString(), host))
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.appendLine(line)
            }

            val exitCode = process.waitFor()
            builder.appendLine()
            builder.appendLine("Exit code: $exitCode")

            builder.toString()
        } catch (e: Exception) {
            "Ping failed: ${e.message}"
        }
    }

    /**
     * DNS lookup
     */
    private fun dnsLookup(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: network dns <domain>"
        }

        val domain = args[0]

        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("DNS Lookup: $domain")
            builder.appendLine("─".repeat(50))

            val addresses = InetAddress.getAllByName(domain)
            addresses.forEach { addr ->
                builder.appendLine("  ${addr.hostAddress} (${addr.hostName})")
            }

            builder.appendLine()
            builder.appendLine("Total: ${addresses.size} address(es)")

            builder.toString()
        } catch (e: Exception) {
            "DNS lookup failed: ${e.message}"
        }
    }

    /**
     * Checks a specific port
     */
    private fun checkPort(args: List<String>): String {
        if (args.size < 2) {
            return "Usage: network port <host> <port> [timeout]"
        }

        val host = args[0]
        val port = args[1].toIntOrNull() ?: return "Invalid port number"
        val timeout = args.getOrNull(2)?.toIntOrNull() ?: DEFAULT_TIMEOUT

        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("Port Check: $host:$port")
            builder.appendLine("─".repeat(50))

            val socket = Socket()
            val startTime = System.currentTimeMillis()

            socket.connect(InetSocketAddress(host, port), timeout)

            val elapsed = System.currentTimeMillis() - startTime
            builder.appendLine("✓ Port $port is OPEN")
            builder.appendLine("  Response time: ${elapsed}ms")

            socket.close()

            builder.toString()
        } catch (e: Exception) {
            "✗ Port $port is CLOSED\n  ${e.message}"
        }
    }

    /**
     * Port scanner
     */
    private fun portScan(args: List<String>): String {
        if (args.size < 2) {
            return "Usage: network scan <host> <ports>\nExample: network scan localhost 80,443,8080"
        }

        val host = args[0]
        val portStrings = args[1].split(",").mapNotNull { it.trim().toIntOrNull() }
        val timeout = args.getOrNull(2)?.toIntOrNull() ?: 1000

        if (portStrings.isEmpty()) {
            return "No valid ports specified"
        }

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Port Scan: $host")
        builder.appendLine("─".repeat(50))

        val openPorts = mutableListOf<Int>()
        val closedPorts = mutableListOf<Int>()

        portStrings.forEach { port ->
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeout)
                openPorts.add(port)
                socket.close()
            } catch (e: Exception) {
                closedPorts.add(port)
            }
        }

        if (openPorts.isNotEmpty()) {
            builder.appendLine("OPEN: ${openPorts.joinToString(", ")}")
        }
        if (closedPorts.isNotEmpty()) {
            builder.appendLine("CLOSED: ${closedPorts.joinToString(", ")}")
        }

        builder.appendLine()
        builder.appendLine("Scanned: ${portStrings.size} ports")

        return builder.toString()
    }

    /**
     * Shows local IP addresses
     */
    private fun showLocalIp(): String {
        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("Local IP Addresses")
            builder.appendLine("─".repeat(50))

            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress) {
                        val ip = address.hostAddress
                        if (ip != null && !ip.contains(":")) {
                            builder.appendLine("  ${networkInterface.displayName}: $ip")
                        }
                    }
                }
            }

            builder.toString()
        } catch (e: Exception) {
            "Error getting local IP: ${e.message}"
        }
    }

    /**
     * Shows public IP
     */
    private fun showPublicIp(): String {
        return try {
            val url = URL("https://api.ipify.org")
            val connection = url.openConnection() as URLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val ip = reader.readLine()
            reader.close()

            buildString {
                appendLine()
                appendLine("Public IP Address")
                appendLine("─".repeat(50))
                appendLine("  $ip")
                appendLine()
                appendLine("Lookup time: ${System.currentTimeMillis()}")
            }
        } catch (e: Exception) {
            "Failed to get public IP: ${e.message}"
        }
    }

    /**
     * WHOIS lookup
     */
    private fun whoisLookup(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: network whois <domain>"
        }

        val domain = args[0]

        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("WHOIS: $domain")
            builder.appendLine("─".repeat(50))

            // Simple HTTP-based WHOIS lookup
            val url = URL("https://whoisjson.com/api/whois?name=$domain")
            val connection = url.openConnection() as URLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 10000

            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            val response = reader.readText()
            reader.close()

            builder.appendLine(response.take(1000))

            builder.toString()
        } catch (e: Exception) {
            "WHOIS lookup failed: ${e.message}\nTry using a dedicated WHOIS app."
        }
    }

    /**
     * HTTP request
     */
    private fun httpRequest(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: network curl <url>"
        }

        val urlString = args[0]
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            return "Please use full URL (http:// or https://)"
        }

        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("HTTP GET: $urlString")
            builder.appendLine("─".repeat(50))

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            builder.appendLine("Status: $responseCode ${getStatusText(responseCode)}")

            val contentType = connection.contentType ?: "Unknown"
            builder.appendLine("Content-Type: $contentType")

            val contentLength = connection.contentLength
            if (contentLength > 0) {
                builder.appendLine("Content-Length: $contentLength bytes")
            }

            builder.appendLine()
            builder.appendLine("Response (first 2KB):")

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val lines = reader.readLines().take(20)
            reader.close()

            lines.forEach { builder.appendLine(it.take(80)) }

            if (connection is HttpURLConnection) {
                connection.disconnect()
            }

            builder.toString()
        } catch (e: Exception) {
            "HTTP request failed: ${e.message}"
        }
    }

    private fun getStatusText(code: Int): String {
        return when (code) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            else -> ""
        }
    }

    /**
     * Simple speed test
     */
    private fun speedTest(context: Context): String {
        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("Network Speed Test")
            builder.appendLine("─".repeat(50))

            // Test download speed with a small file
            val testUrl = "https://speed.hetzner.de/1MB.bin"
            val startTime = System.currentTimeMillis()

            val url = URL(testUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            val inputStream = connection.inputStream
            var totalBytes = 0
            val buffer = ByteArray(8192)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytes += bytesRead
            }

            inputStream.close()
            connection.disconnect()

            val elapsed = System.currentTimeMillis() - startTime
            val speedKbps = (totalBytes * 8.0 / elapsed).toLong()

            builder.appendLine("Downloaded: ${totalBytes / 1024} KB")
            builder.appendLine("Time: ${elapsed}ms")
            builder.appendLine("Speed: $speedKbps kbps")
            builder.appendLine()
            builder.appendLine(String.format("  [%-30s] %d kbps", "█".repeat((speedKbps / 100).coerceAtMost(30).toInt()), speedKbps))

            builder.toString()
        } catch (e: Exception) {
            "Speed test failed: ${e.message}"
        }
    }

    /**
     * Network connection status
     */
    private fun networkStatus(context: Context): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Network Status")
        builder.appendLine("─".repeat(50))
        builder.appendLine("  Status: ${if (connected) "CONNECTED" else "DISCONNECTED"}")
        if (connected) {
            builder.appendLine("  Type: $connectionType")
            builder.appendLine("  Cellular: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true}")
            builder.appendLine("  WiFi: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true}")
            builder.appendLine("  Ethernet: ${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true}")
        }
        builder.appendLine()
        builder.appendLine("Use 'network localip' for local addresses")

        return builder.toString()
    }
}

private typealias HttpURLConnection = java.net.HttpURLConnection
