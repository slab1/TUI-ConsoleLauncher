package ohi.andre.consolelauncher.tuils.libsuperuser;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.managers.TerminalManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 18/08/2017.
 */

public class ShellHolder {

    private static final String TAG = "ShellHolder";

    private Context context;

    public ShellHolder(Context context) {
        this.context = context;
    }

    Pattern p = Pattern.compile("^\\n");

    public Shell.Interactive build() {
        Shell.Interactive interactive = null;

        try {
            interactive = new Shell.Builder()
                    .setOnSTDOUTLineListener(line -> {
                        line = p.matcher(line).replaceAll(Tuils.EMPTYSTRING);
                        Tuils.sendOutput(context, line, TerminalManager.CATEGORY_OUTPUT);
                    })
                    .setOnSTDERRLineListener(line -> {
                        line = p.matcher(line).replaceAll(Tuils.EMPTYSTRING);
                        Tuils.sendOutput(context, line, TerminalManager.CATEGORY_OUTPUT);
                    })
                    .open();
        } catch (Exception e) {
            Log.e(TAG, "Failed to build shell", e);
        }

        if (interactive == null) {
            Log.e(TAG, "Shell could not be created");
            interactive = new Shell.Builder()
                    .setOnSTDOUTLineListener(line -> {})
                    .setOnSTDERRLineListener(line -> {})
                    .open();
            return interactive;
        }

        try {
            File homeDir = XMLPrefsManager.get(File.class, Behavior.home_path);
            if (homeDir != null) {
                interactive.addCommand("cd " + homeDir.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to set home directory", e);
        }

        return interactive;
    }
}
