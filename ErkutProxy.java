import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ErkutProxy {
    static int proxyPort = 8888;
    static String defaultTargetHost = "localhost";
    static int defaultTargetPort = 80;  // HTTP Port
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
            if (requestLine == null || requestLine.isEmpty() || requestLine.startsWith("CONNECT")) return;
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

            if (host != null && host.equals("localhost") && path != null) {
                try {
                    int requestedFileSize = Integer.parseInt(path.split("/")[1]);
                    if (requestedFileSize > maxRequestSize) {
                        out.println("HTTP/1.1 414 Request-URI Too Long");
                        return;
                    }
                } catch (NumberFormatException e) {
                    // If not a number
                    out.println("HTTP/1.1 400 Bad Request");
                    return;
                }
            }

            // Cache kontrolü
            if (method.equals("GET")) {
                System.out.println("Request URI for cache control: " + requestURI);
                CacheEntry cacheEntry = cache.get(requestURI);
                if (cacheEntry != null) {
                    System.out.println("Cache hit for: " + requestURI);
                    out.println(cacheEntry.response);
                    return;
                }
            }

            // Hedef sunucuya istek ilet
            try (
                Socket targetSocket = new Socket(host, port);
                PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))
            ) {
                // Forward the GET request
                targetOut.println(method + " " + path + " " + requestVersion);
                System.out.println("Forwarded request: " + method + " " + path + " " + requestVersion);
                targetOut.println("Host: " + host);
                targetOut.println();  // Blank line to end headers

                // İstek başlıklarını ilet
                String headerLine;
                while (!(headerLine = in.readLine()).isEmpty()) {
                    targetOut.println(headerLine);
                }

                // Read the response
                StringBuilder responseBuilder = new StringBuilder();
                String responseLine;
                while ((responseLine = targetIn.readLine()) != null) {
                    responseBuilder.append(responseLine).append("\n");
                }

                String response = responseBuilder.toString();

                // Add to cache 
                if (cache.size() >= cacheSize) {
                    String oldestKey = cache.keySet().iterator().next();
                    cache.remove(oldestKey);
                    System.out.println("Cache full, removed: " + oldestKey);
                }
                System.out.println("Request URI for cache adding: " + requestURI);
                cache.put(requestURI, new CacheEntry(response));

                // Yanıtı istemciye ilet
                out.println(response);

            } catch (IOException e) {
                // Return 404 Not Found when server is not available
                System.out.println("Error connecting to target server: " + e.getMessage());
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                out.println("<html><body><h1>404 Not Found</h1>");
                out.println("<p>The requested server is not available.</p></body></html>");
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
