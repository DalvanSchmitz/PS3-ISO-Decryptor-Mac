package com.ps3dec.ui.models;

import java.io.File;

public class BatchItem {
    private File isoFile;
    private File keyFile;
    private String status;

    public BatchItem(File isoFile, File keyFile, String status) {
        this.isoFile = isoFile;
        this.keyFile = keyFile;
        this.status = status;
    }

    public File getIsoFile() { return isoFile; }
    public File getKeyFile() { return keyFile; }
    public void setKeyFile(File keyFile) { this.keyFile = keyFile; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
