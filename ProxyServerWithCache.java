import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ProxyServerWithCache {
    static int proxyPort = 8888;
    static String defaultTargetHost = "localhost";
    static int defaultTargetPort = 443;
    static int maxRequestSize = 9999;
    static int cacheSize = 5; // Cache size
    static Map<String, CacheEntry> cache = new LinkedHashMap<>(cacheSize, 0.75f, true); // LRU Cache

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(proxyPort);
        System.out.println("Proxy server is listening on port: " + proxyPort);
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.execute(() -> handleClient(clientSocket));
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("Request: " + requestLine);
            String[] parts = requestLine.split(" ");
            String method = parts[0];
            String requestURI = parts[1];
            String requestVersion = parts[2];

            // Parse URI
            String host = "";
            int port = -1;
            String path = "";
            try {
                URI uri = new URI(requestURI);
                host = uri.getHost();
                port = uri.getPort();
                path = uri.getPath();
                if (port == -1) port = defaultTargetPort;
            } catch (URISyntaxException e) {
                e.printStackTrace();
                out.println("HTTP/1.1 400 Bad Request");
                return;
            }

            // Check cache
            if (method.equals("GET")) {
                CacheEntry cacheEntry = cache.get(requestURI);
                if (cacheEntry != null) {
                    System.out.println("Cache hit for: " + requestURI);
                    out.println(cacheEntry.response);
                    return;
                }
            }

            // Forward request to target server
            try (
                Socket targetSocket = new Socket(host, port);
                PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))
            ) {
                // Forward request
                targetOut.println(requestLine);
                String headerLine;
                while (!(headerLine = in.readLine()).isEmpty()) {
                    targetOut.println(headerLine);
                }
                targetOut.println();

                // Read response from target server
                StringBuilder responseBuilder = new StringBuilder();
                String responseLine;
                boolean isEvenFile = path.length() % 2 == 0;
                boolean conditionalGet = false;

                while ((responseLine = targetIn.readLine()) != null) {
                    responseBuilder.append(responseLine).append("\n");

                    // Conditional GET logic
                    if (responseLine.startsWith("Last-Modified:") && !isEvenFile) {
                        targetOut.println("If-Modified-Since: " + responseLine.split(": ", 2)[1]);
                        conditionalGet = true;
                    }
                }

                String response = responseBuilder.toString();

                // Cache the response if no conditional GET or modified content
                if (!conditionalGet || isEvenFile) {
                    if (cache.size() >= cacheSize) {
                        String oldestKey = cache.keySet().iterator().next();
                        cache.remove(oldestKey);
                        System.out.println("Cache full, removed: " + oldestKey);
                    }
                    cache.put(requestURI, new CacheEntry(response));
                }

                // Send response back to client
                out.println(response);

            } catch (IOException e) {
                out.println("HTTP/1.1 502 Bad Gateway");
                out.println("Content-Type: text/plain");
                out.println();
                out.println("Error connecting to target server");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class CacheEntry {
        String response;

        CacheEntry(String response) {
            this.response = response;
        }
    }
}
