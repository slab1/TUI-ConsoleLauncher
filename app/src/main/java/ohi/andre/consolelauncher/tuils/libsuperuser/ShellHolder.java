package ohi.andre.consolelauncher.tuils.libsuperuser;

import android.content.Context;

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

    private Context context;

    public ShellHolder(Context context) {
        this.context = context;
    }

    Pattern p = Pattern.compile("^\\n");

    public Shell.Interactive build() {
        Shell.Interactive interactive;
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
            Tuils.sendOutput(context, "Warning: shell init failed (" + e.getMessage() + ")", TerminalManager.CATEGORY_OUTPUT);
            return null;
        }
        try {
            interactive.addCommand("cd " + XMLPrefsManager.get(File.class, Behavior.home_path));
        } catch (Exception e) {
            Tuils.sendOutput(context, "Warning: could not set home dir (" + e.getMessage() + ")", TerminalManager.CATEGORY_OUTPUT);
        }
        return interactive;
    }
}
