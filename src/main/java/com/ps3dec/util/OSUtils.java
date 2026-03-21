package com.ps3dec.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class OSUtils {

    public static String getPs3decPath() {
        try {
            String classPath = OSUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File classDir = new File(classPath).getParentFile();

            File[] searchDirs = {
                    classDir,                                                    
                    classDir != null ? new File(classDir, "../Resources") : null, 
                    classDir != null ? classDir.getParentFile() : null,           
                    new File(System.getProperty("user.dir")),                     
            };

            for (File dir : searchDirs) {
                if (dir == null) continue;
                File bin = new File(dir, "ps3decrs");
                if (bin.exists() && bin.canExecute())
                    return bin.getAbsolutePath();
            }
        } catch (Exception ignored) {}

        try {
            Process which = new ProcessBuilder("which", "ps3decrs").start();
            BufferedReader r = new BufferedReader(new InputStreamReader(which.getInputStream()));
            String path = r.readLine();
            which.waitFor();
            if (path != null && !path.isEmpty() && new File(path).exists()) {
                return path;
            }
        } catch (Exception ignored) {}

        return new File(System.getProperty("user.dir"), "ps3decrs").getAbsolutePath();
    }
}
