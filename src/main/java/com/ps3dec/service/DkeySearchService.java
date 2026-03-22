package com.ps3dec.service;

import com.ps3dec.model.DkeyResult;

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

    private static String lastHtmlContent = null;
    private static long   lastFetchTime = 0;

    // DKEY hex: exactly 32 hex characters (16-byte PS3 disc key)
    private static final Pattern HEX32 = Pattern.compile("^[0-9A-Fa-f]{32}$");

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

    // ── HTTP fetch ────────────────────────────────────────────────────────────

    public DkeyResult fetchAndParseSync(String titleId) throws Exception {
        trustAllCerts();

        URL url = new URL(DKEY_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        // Mimic a browser so the server doesn't reject us
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        conn.setRequestProperty("Accept", "text/html,*/*");

        long now = System.currentTimeMillis();
        if (lastHtmlContent != null && (now - lastFetchTime) < CACHE_EXPIRY_MS) {
            return parseHtml(lastHtmlContent, titleId);
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new Exception("HTTP " + status);
        }

        StringBuilder html = new StringBuilder(900_000);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append('\n');
            }
        }

        lastHtmlContent = html.toString();
        lastFetchTime = System.currentTimeMillis();

        return parseHtml(lastHtmlContent, titleId);
    }

    // ── HTML parsing ──────────────────────────────────────────────────────────

    /**
     * Scans the HTML for a {@code <tr>} row containing {@code titleId}
     * and returns the DKEY from column index 5, or null if not found.
     */
    private DkeyResult parseHtml(String html, String titleId) {
        // Each table row: <tr ...> ... </tr>
        Pattern rowPat = Pattern.compile("<tr[^>]*>(.*?)</tr>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher rowMatcher = rowPat.matcher(html);

        while (rowMatcher.find()) {
            String row = rowMatcher.group(1);
            if (!row.toUpperCase().contains(titleId)) continue;

            List<String> cells = extractCells(row);

            // Find the cell that is exactly the title ID we want
            boolean titleFound = false;
            for (String cell : cells) {
                if (cell.equalsIgnoreCase(titleId)) { titleFound = true; break; }
            }
            if (!titleFound) continue;

            // The DKEY hex must be exactly 32 hex chars. The table has MD5 (col 4) and DKEY (col 5).
            // We search from the end to ensure we grab the DKEY, not the MD5 hash.
            for (int i = cells.size() - 1; i >= 0; i--) {
                String stripped = cells.get(i).trim();
                if (HEX32.matcher(stripped).matches()) {
                    // Extract game name: first non-empty cell that is NOT the titleId and NOT hex
                    String gameName = "";
                    for (String c : cells) {
                        String s = c.trim();
                        if (!s.isEmpty() && !s.equalsIgnoreCase(titleId)
                                && !HEX32.matcher(s).matches()) {
                            gameName = s; break;
                        }
                    }
                    return new DkeyResult(titleId, gameName, stripped);
                }
            }
        }
        return null; // not found
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
