package com.ps3dec.service;

import com.ps3dec.model.DkeyResult;
import java.util.Map;
import java.util.HashMap;

import javax.net.ssl.*;
import javax.swing.SwingWorker;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searches the PS3 online DKEY database at aldostools.org and extracts
 * the decryption key for a given Game/Title ID.
 *
 * <p>The site embeds all data in a static HTML table (~800 KB).
 * We fetch the full page, then search row-by-row via regex for the
 * matching Title ID and extract the DKEY hex from column index 5.</p>
 *
 * <p>SSL validation is relaxed because the site uses a self-signed certificate.</p>
 */
public class DkeySearchService {

    private static final String DKEY_URL = "https://ps3.aldostools.org/dkey.html";
    private static final int    TIMEOUT_MS = 20_000;
    private static final long   CACHE_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes
    private static final Pattern HEX32 = Pattern.compile("^[0-9A-Fa-f]{32}$");

    private static Map<String, DkeyResult> dkeyCache = null;
    private static long lastFetchTime = 0;

    // ── Public callback interface ─────────────────────────────────────────────

    public interface SearchCallback {
        /** Called when a matching DKEY was found. */
        void onResult(DkeyResult result);
        /** Called when the title ID exists in the DB but has no valid DKEY. */
        void onNotFound(String titleId);
        /** Called on network or parse error. */
        void onError(String errorMsg);
    }

    // ── Async search ──────────────────────────────────────────────────────────

    /**
     * Performs the search in a background thread.
     * Callbacks are always invoked on the Event Dispatch Thread.
     *
     * @param titleId  PS3 Title ID, e.g. "BLES01234". Case-insensitive.
     * @param callback Result callbacks.
     */
    public void searchAsync(String titleId, SearchCallback callback) {
        new SwingWorker<DkeyResult, Void>() {
            @Override
            protected DkeyResult doInBackground() throws Exception {
                return fetchAndParseSync(titleId.toUpperCase().trim());
            }
            @Override
            protected void done() {
                try {
                    DkeyResult result = get();
                    if (result != null) callback.onResult(result);
                    else                callback.onNotFound(titleId);
                } catch (Exception e) {
                    String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    callback.onError("Falha na busca: " + cause);
                }
            }
        }.execute();
    }

    public DkeyResult fetchAndParseSync(String titleId) throws Exception {
        String tid = titleId.toUpperCase().trim();
        
        long now = System.currentTimeMillis();
        if (dkeyCache != null && (now - lastFetchTime) < CACHE_EXPIRY_MS) {
            return dkeyCache.get(tid);
        }

        trustAllCerts();

        URL url = new URL(DKEY_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        
        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) throw new Exception("HTTP " + status);

        StringBuilder html = new StringBuilder(1000_000);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append('\n');
            }
        }

        // Parse entire HTML into memory map
        Map<String, DkeyResult> newCache = parseFullHtml(html.toString());
        dkeyCache = newCache;
        lastFetchTime = System.currentTimeMillis();

        return dkeyCache.get(tid);
    }

    private Map<String, DkeyResult> parseFullHtml(String html) {
        Map<String, DkeyResult> map = new HashMap<>();
        Pattern rowPat = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher rowMatcher = rowPat.matcher(html);

        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            List<String> cells = extractCells(row);
            if (cells.size() < 6) continue;

            // Typically: [Game Title, Region, Game ID, MD5, DKEY, ...]
            // Let's find exactly which cell is the TitleID (e.g. BLES01234) and which is the DKEY (32 hex)
            String foundTitleId = null;
            String foundDkey = null;
            String foundGameName = "";

            for (String cell : cells) {
                String s = cell.trim();
                if (s.isEmpty()) continue;
                
                if (s.length() == 9 && (s.startsWith("B") || s.startsWith("N") || s.startsWith("M"))) {
                    foundTitleId = s.toUpperCase();
                } else if (HEX32.matcher(s).matches()) {
                    foundDkey = s;
                } else if (foundGameName.isEmpty()) {
                    foundGameName = s;
                }
            }

            if (foundTitleId != null && foundDkey != null) {
                map.put(foundTitleId, new DkeyResult(foundTitleId, foundGameName, foundDkey));
            }
        }
        return map;
    }

    /** Extracts inner text from each {@code <td>} in a row, stripping HTML tags. */
    private List<String> extractCells(String rowHtml) {
        List<String> cells = new ArrayList<>();
        // The aldostools site omits closing </td> tags. We split by the opening <td...>.
        String[] parts = rowHtml.split("(?i)<td[^>]*>");
        for (int i = 1; i < parts.length; i++) {
            String text = parts[i]
                    .replaceAll("<[^>]+>", "")   // strip remaining tags
                    .replaceAll("&amp;", "&")
                    .replaceAll("&nbsp;", " ")
                    .trim();
            cells.add(text);
        }
        return cells;
    }

    // ── SSL helper ────────────────────────────────────────────────────────────

    /**
     * Installs a permissive SSL context that accepts the self-signed certificate
     * used by ps3.aldostools.org. Only affects this JVM process.
     */
    private static void trustAllCerts() {
        try {
            TrustManager[] tm = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tm, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
        } catch (Exception ignored) {}
    }
}
