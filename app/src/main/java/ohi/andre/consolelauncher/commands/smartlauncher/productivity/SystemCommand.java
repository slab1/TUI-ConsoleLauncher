package ohi.andre.consolelauncher.commands.smartlauncher.productivity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Smart Launcher System Command
 * Displays comprehensive system information including CPU, memory, battery, storage
 */
public class SystemCommand implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        StringBuilder output = new StringBuilder();
        output.append("\n┌─────────────────────────────────────────────────────────────────────┐\n");
        output.append("│                    SYSTEM INFORMATION                              │\n");
        output.append("├─────────────────────────────────────────────────────────────────────┤\n");
        
        // System info
        output.append("│  Device:     ").append(Build.MODEL).append(Tuils.SPACE.repeat(35 - Build.MODEL.length())).append("│\n");
        output.append("│  Android:    ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")").append(Tuils.SPACE.repeat(20)).append("│\n");
        output.append("│  Build:      ").append(Build.BOARD).append(Tuils.SPACE.repeat(35 - Build.BOARD.length())).append("│\n");
        
        // Memory info
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);
        
        long totalMemory = memInfo.totalMem;
        long availableMemory = memInfo.availMem;
        long usedMemory = totalMemory - availableMemory;
        double memoryUsage = (double) usedMemory / totalMemory * 100;
        
        output.append("│  Memory:     ").append(formatBytes(usedMemory)).append(" / ").append(formatBytes(totalMemory)).append(" (").append(String.format("%.1f", memoryUsage)).append("%)").append(Tuils.SPACE.repeat(15)).append("│\n");
        
        // Storage info
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long totalStorage = stat.getTotalBytes();
        long availableStorage = stat.getAvailableBytes();
        long usedStorage = totalStorage - availableStorage;
        double storageUsage = (double) usedStorage / totalStorage * 100;
        
        output.append("│  Storage:    ").append(formatBytes(usedStorage)).append(" / ").append(formatBytes(totalStorage)).append(" (").append(String.format("%.1f", storageUsage)).append("%)").append(Tuils.SPACE.repeat(15)).append("│\n");
        
        // Battery info
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        int batteryStatus = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        
        String status = getBatteryStatus(batteryStatus);
        output.append("│  Battery:    ").append(batteryLevel).append("% ").append(status).append(Tuils.SPACE.repeat(25)).append("│\n");
        
        // Network info
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        
        String networkType = getNetworkType(capabilities);
        String networkStatus = (network != null && capabilities != null) ? "Connected" : "Disconnected";
        
        output.append("│  Network:    ").append(networkStatus).append(" (").append(networkType).append(")").append(Tuils.SPACE.repeat(15)).append("│\n");
        
        // CPU info
        output.append("│  CPU:        ").append(Runtime.getRuntime().availableProcessors()).append(" cores").append(Tuils.SPACE.repeat(25)).append("│\n");
        
        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        output.append("│  Time:       ").append(timestamp).append(Tuils.SPACE.repeat(35 - timestamp.length())).append("│\n");
        
        output.append("└─────────────────────────────────────────────────────────────────────┘\n");
        
        return output.toString();
    }

    @Override
    public int[] argType() {
        return new int[]{}; // No arguments required
    }

    @Override
    public int priority() {
        return 3; // Higher priority for smart launcher commands
    }

    @Override
    public int helpRes() {
        return 0; // Use default help
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return "System command doesn't accept arguments";
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "System command doesn't require arguments";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String getBatteryStatus(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Charging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Full";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "Not Charging";
            default:
                return "Unknown";
        }
    }

    private String getNetworkType(NetworkCapabilities capabilities) {
        if (capabilities == null) return "None";
        
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return "WiFi";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return "Cellular";
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return "Ethernet";
        } else {
            return "Other";
        }
    }
}
