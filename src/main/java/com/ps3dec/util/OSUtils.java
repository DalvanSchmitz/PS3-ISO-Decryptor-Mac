package com.ps3dec.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class OSUtils {

    public static String getPs3decPath() {
        String binName = System.getProperty("os.name").toLowerCase().contains("win") ? "ps3dec.exe" : "ps3decrs";

        try {
            String classPath = OSUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(classPath);
            File jarDir = jarFile.getParentFile();

            java.util.List<File> searchDirs = new java.util.ArrayList<>();
            if (jarDir != null) {
                searchDirs.add(jarDir);
                searchDirs.add(new File(jarDir, "bin"));
                searchDirs.add(new File(jarDir, "../Resources"));
                if (jarDir.getParentFile() != null) searchDirs.add(jarDir.getParentFile());
            }
            searchDirs.add(new File(System.getProperty("user.dir")));
            searchDirs.add(new File(System.getProperty("user.home")));
            searchDirs.add(new File(System.getProperty("user.home"), "Downloads"));

            for (File dir : searchDirs) {
                if (dir == null || !dir.exists()) continue;
                File bin = new File(dir, binName);
                if (bin.exists()) {
                    // Self-heal: try to make it executable if it's not
                    if (!bin.canExecute()) {
                        try { bin.setExecutable(true); } catch (Exception ignored) {}
                    }
                    if (bin.canExecute()) return bin.getAbsolutePath();
                }
            }
        } catch (Exception ignored) {}

        // 2. Try 'which' command
        try {
            Process which = new ProcessBuilder(System.getProperty("os.name").toLowerCase().contains("win") ? "where" : "which", binName).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(which.getInputStream()))) {
                String path = r.readLine();
                if (path != null && !path.isEmpty()) {
                    File f = new File(path);
                    if (f.exists()) return f.getAbsolutePath();
                }
            }
        } catch (Exception ignored) {}

        // 3. Last resort: just return local name and let it fail with a clear msg
        String local = new File(System.getProperty("user.dir"), binName).getAbsolutePath();
        if (!new File(local).exists()) {
            System.err.println("CRITICAL: Binary " + binName + " not found in search paths!");
        }
        return local;
    }
}
