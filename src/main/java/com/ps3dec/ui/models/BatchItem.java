package com.ps3dec.ui.models;

import com.ps3dec.util.I18n;
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

    /**
     * Retorna o texto de status localizado.
     */
    public String getStatusText() {
        if (status == null) return "";
        switch (status) {
            case "PENDING":        return I18n.get("item.status.pending");
            case "ONLINE_SEARCH":  return I18n.get("item.status.online_search");
            case "READY_ONLINE":   return I18n.get("item.status.ready_online");
            case "NOT_FOUND":      return I18n.get("item.status.not_found_online");
            case "SEARCH_ERROR":   return I18n.get("item.status.search_error");
            case "DECRYPTING":     return I18n.get("item.status.decrypting");
            case "DONE":           return I18n.get("item.status.done");
            case "ERROR":          return I18n.get("item.status.error");
            case "READY":          return I18n.get("item.status.pending"); // Reuse pending icon/color
            default:               return status;
        }
    }
}
