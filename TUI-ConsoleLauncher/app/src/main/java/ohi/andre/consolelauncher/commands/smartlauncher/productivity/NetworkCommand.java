package ohi.andre.consolelauncher.commands.smartlauncher.productivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * Network Command - Network diagnostic tools
 * Supports ping, DNS lookup, port scan, and connectivity checks
 */
public class NetworkCommand implements CommandAbstraction {

    private static final String TAG = "NetworkCommand";
    private static final int DEFAULT_TIMEOUT = 3000;

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        ArrayList<String> argsList = new ArrayList<>();
        for (Object arg : pack.args) {
            if (arg != null) {
                argsList.add(arg.toString());
            }
        }

        if (argsList.isEmpty() || "--help".equals(argsList.get(0)) || "-h".equals(argsList.get(0))) {
            return getUsage();
        }

        String command = argsList.get(0).toLowerCase();
        ArrayList<String> parameters = new ArrayList<>(argsList.subList(1, argsList.size()));

        switch (command) {
            case "ping":
                return pingHost(parameters);
            case "dns":
            case "resolve":
                return dnsLookup(parameters);
            case "port":
                return checkPort(parameters);
            case "scan":
                return portScan(parameters);
            case "localip":
            case "ip":
                return showLocalIp();
            case "publicip":
            case "external":
                return showPublicIp();
            case "whois":
                return whoisLookup(parameters);
            case "curl":
            case "http":
            case "get":
                return httpRequest(parameters);
            case "speed":
            case "test":
                return speedTest(context);
            case "status":
            case "check":
                return networkStatus(context);
            default:
                return "Unknown network command: " + command + "\n" + getUsage();
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return "Missing argument at position " + indexNotFound + "\n" + getUsage();
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "Not enough arguments (" + nArgs + " required)\n" + getUsage();
    }

    private String getUsage() {
        return "\n╔══════════════════════════════════════════════════════╗\n" +
               "║                  NETWORK COMMANDS                     ║\n" +
               "╠══════════════════════════════════════════════════════╣\n" +
               "║  network ping <host>         - Ping a host           ║\n" +
               "║  network dns <domain>        - Resolve DNS           ║\n" +
               "║  network port <host> <port>  - Check port            ║\n" +
               "║  network scan <host> <ports> - Scan port range       ║\n" +
               "║  network localip             - Show local IP         ║\n" +
               "║  network publicip            - Show public IP        ║\n" +
               "║  network whois <domain>      - WHOIS lookup          ║\n" +
               "║  network curl <url>          - HTTP request          ║\n" +
               "║  network speed               - Speed test            ║\n" +
               "║  network status              - Connection status     ║\n" +
               "╚══════════════════════════════════════════════════════╝\n";
    }

    /**
     * Pings a host
     */
    private String pingHost(ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: network ping <host> [count]";
        }

        String host = args.get(0);
        int count = 4;
        if (args.size() > 1) {
            try {
                count = Integer.parseInt(args.get(1));
            } catch (NumberFormatException e) {
                // Use default count
            }
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("PING ").append(host).append(" (").append(count).append(" pings)\n");
            builder.append("─".repeat(50)).append("\n");

            Process process = Runtime.getRuntime().exec(new String[]{"ping", "-c", String.valueOf(count), host});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            builder.append("\n");
            builder.append("Exit code: ").append(exitCode).append("\n");

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Ping failed", e);
            return "Ping failed: " + e.getMessage();
        }
    }

    /**
     * DNS lookup
     */
    private String dnsLookup(ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: network dns <domain>";
        }

        String domain = args.get(0);

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("DNS Lookup: ").append(domain).append("\n");
            builder.append("─".repeat(50)).append("\n");

            InetAddress[] addresses = InetAddress.getAllByName(domain);
            for (InetAddress addr : addresses) {
                builder.append("  ").append(addr.getHostAddress()).append(" (").append(addr.getHostName()).append(")\n");
            }

            builder.append("\n");
            builder.append("Total: ").append(addresses.length).append(" address(es)\n");

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "DNS lookup failed", e);
            return "DNS lookup failed: " + e.getMessage();
        }
    }

    /**
     * Checks a specific port
     */
    private String checkPort(ArrayList<String> args) {
        if (args.size() < 2) {
            return "Usage: network port <host> <port> [timeout]";
        }

        String host = args.get(0);
        int port;
        try {
            port = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            return "Invalid port number";
        }

        int timeout = DEFAULT_TIMEOUT;
        if (args.size() > 2) {
            try {
                timeout = Integer.parseInt(args.get(2));
            } catch (NumberFormatException e) {
                // Use default timeout
            }
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("Port Check: ").append(host).append(":").append(port).append("\n");
            builder.append("─".repeat(50)).append("\n");

            Socket socket = new Socket();
            long startTime = System.currentTimeMillis();

            socket.connect(new InetSocketAddress(host, port), timeout);

            long elapsed = System.currentTimeMillis() - startTime;
            builder.append("✓ Port ").append(port).append(" is OPEN\n");
            builder.append("  Response time: ").append(elapsed).append("ms\n");

            socket.close();

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Port check failed", e);
            return "✗ Port " + port + " is CLOSED\n  " + e.getMessage();
        }
    }

    /**
     * Port scanner
     */
    private String portScan(ArrayList<String> args) {
        if (args.size() < 2) {
            return "Usage: network scan <host> <ports>\nExample: network scan localhost 80,443,8080";
        }

        String host = args.get(0);
        String[] portStrings = args.get(1).split(",");
        ArrayList<Integer> portList = new ArrayList<>();
        
        for (String portStr : portStrings) {
            try {
                portList.add(Integer.parseInt(portStr.trim()));
            } catch (NumberFormatException e) {
                // Skip invalid ports
            }
        }

        if (portList.isEmpty()) {
            return "No valid ports specified";
        }

        int timeout = 1000;
        if (args.size() > 2) {
            try {
                timeout = Integer.parseInt(args.get(2));
            } catch (NumberFormatException e) {
                // Use default timeout
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("Port Scan: ").append(host).append("\n");
        builder.append("─".repeat(50)).append("\n");

        ArrayList<Integer> openPorts = new ArrayList<>();
        ArrayList<Integer> closedPorts = new ArrayList<>();

        for (Integer port : portList) {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeout);
                openPorts.add(port);
                socket.close();
            } catch (Exception e) {
                closedPorts.add(port);
            }
        }

        if (!openPorts.isEmpty()) {
            builder.append("OPEN: ").append(listToString(openPorts)).append("\n");
        }
        if (!closedPorts.isEmpty()) {
            builder.append("CLOSED: ").append(listToString(closedPorts)).append("\n");
        }

        builder.append("\n");
        builder.append("Scanned: ").append(portList.size()).append(" ports\n");

        return builder.toString();
    }

    private String listToString(ArrayList<Integer> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /**
     * Shows local IP addresses
     */
    private String showLocalIp() {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("Local IP Addresses\n");
            builder.append("─".repeat(50)).append("\n");

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress()) {
                        String ip = address.getHostAddress();
                        if (ip != null && !ip.contains(":")) {
                            builder.append("  ").append(networkInterface.getDisplayName()).append(": ").append(ip).append("\n");
                        }
                    }
                }
            }

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP", e);
            return "Error getting local IP: " + e.getMessage();
        }
    }

    /**
     * Shows public IP
     */
    private String showPublicIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String ip = reader.readLine();
            reader.close();

            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("Public IP Address\n");
            builder.append("─".repeat(50)).append("\n");
            builder.append("  ").append(ip).append("\n");
            builder.append("\n");
            builder.append("Lookup time: ").append(System.currentTimeMillis()).append("\n");

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get public IP", e);
            return "Failed to get public IP: " + e.getMessage();
        }
    }

    /**
     * WHOIS lookup
     */
    private String whoisLookup(ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: network whois <domain>";
        }

        String domain = args.get(0);

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("WHOIS: ").append(domain).append("\n");
            builder.append("─".repeat(50)).append("\n");

            // Simple HTTP-based WHOIS lookup
            URL url = new URL("https://whoisjson.com/api/whois?name=" + domain);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            reader.close();

            String responseText = response.toString();
            if (responseText.length() > 1000) {
                responseText = responseText.substring(0, 1000);
            }
            builder.append(responseText);

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "WHOIS lookup failed", e);
            return "WHOIS lookup failed: " + e.getMessage() + "\nTry using a dedicated WHOIS app.";
        }
    }

    /**
     * HTTP request
     */
    private String httpRequest(ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: network curl <url>";
        }

        String urlString = args.get(0);
        if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            return "Please use full URL (http:// or https://)";
        }

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("HTTP GET: ").append(urlString).append("\n");
            builder.append("─".repeat(50)).append("\n");

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            builder.append("Status: ").append(responseCode).append(" ").append(getStatusText(responseCode)).append("\n");

            String contentType = connection.getContentType();
            if (contentType == null) contentType = "Unknown";
            builder.append("Content-Type: ").append(contentType).append("\n");

            int contentLength = connection.getContentLength();
            if (contentLength > 0) {
                builder.append("Content-Length: ").append(contentLength).append(" bytes\n");
            }

            builder.append("\n");
            builder.append("Response (first 2KB):\n");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            ArrayList<String> lines = new ArrayList<>();
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 20) {
                lines.add(line.length() > 80 ? line.substring(0, 80) : line);
                lineCount++;
            }
            reader.close();

            for (String responseLine : lines) {
                builder.append(responseLine).append("\n");
            }

            connection.disconnect();

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "HTTP request failed", e);
            return "HTTP request failed: " + e.getMessage();
        }
    }

    private String getStatusText(int code) {
        switch (code) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "";
        }
    }

    /**
     * Simple speed test
     */
    private String speedTest(Context context) {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("Network Speed Test\n");
            builder.append("─".repeat(50)).append("\n");

            // Test download speed with a small file
            String testUrl = "https://speed.hetzner.de/1MB.bin";
            long startTime = System.currentTimeMillis();

            URL url = new URL(testUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            java.io.InputStream inputStream = connection.getInputStream();
            long totalBytes = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
            }

            inputStream.close();
            connection.disconnect();

            long elapsed = System.currentTimeMillis() - startTime;
            long speedKbps = (totalBytes * 8 / elapsed);

            builder.append("Downloaded: ").append(totalBytes / 1024).append(" KB\n");
            builder.append("Time: ").append(elapsed).append("ms\n");
            builder.append("Speed: ").append(speedKbps).append(" kbps\n");
            builder.append("\n");
            
            int barLength = Math.min((int) (speedKbps / 100), 30);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < barLength; i++) {
                bar.append("█");
            }
            builder.append(String.format("  [%-30s] %d kbps", bar.toString(), speedKbps)).append("\n");

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Speed test failed", e);
            return "Speed test failed: " + e.getMessage();
        }
    }

    /**
     * Network connection status
     */
    private String networkStatus(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

        boolean connected = capabilities != null;
        String connectionType;
        if (connected) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                connectionType = "WiFi";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                connectionType = "Cellular";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                connectionType = "Ethernet";
            } else {
                connectionType = "Other";
            }
        } else {
            connectionType = "Disconnected";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("Network Status\n");
        builder.append("─".repeat(50)).append("\n");
        builder.append("  Status: ").append(connected ? "CONNECTED" : "DISCONNECTED").append("\n");
        if (connected) {
            builder.append("  Type: ").append(connectionType).append("\n");
            builder.append("  Cellular: ").append(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).append("\n");
            builder.append("  WiFi: ").append(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).append("\n");
            builder.append("  Ethernet: ").append(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).append("\n");
        }
        builder.append("\n");
        builder.append("Use 'network localip' for local addresses\n");

        return builder.toString();
    }
}