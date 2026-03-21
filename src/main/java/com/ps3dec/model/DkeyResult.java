package com.ps3dec.model;

/**
 * Represents a DKEY search result from the online database.
 */
public class DkeyResult {

    private final String titleId;
    private final String gameName;
    private final String dkeyHex;

    public DkeyResult(String titleId, String gameName, String dkeyHex) {
        this.titleId  = titleId;
        this.gameName = gameName;
        this.dkeyHex  = dkeyHex;
    }

    /** e.g. "BLES01234" */
    public String getTitleId()  { return titleId; }

    /** Full game title */
    public String getGameName() { return gameName; }

    /**
     * 32-character hex string (16 bytes) representing the PS3 disc decryption key.
     * Can be passed directly to ps3decrs via --dk.
     */
    public String getDkeyHex() { return dkeyHex; }

    @Override
    public String toString() {
        return titleId + " - " + gameName + " [" + dkeyHex + "]";
    }
}
