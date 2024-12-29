import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class ProxyServerWithCache {
    static int proxyPort = 8888;
    static String defaultTargetHost = "localhost";
    static int defaultTargetPort = 80;
    static int maxRequestSize = 9999;

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
            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                out.println("HTTP/1.1 400 Bad Request");
                return;
            }



            String method = parts[0];
            String requestURI = parts[1];
            String httpVersion = parts[2];
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
                    System.out.println("Not a number");
                    return;
                }
            }
            System.out.println("Request: " + requestLine);
            System.out.println("Host: " + host + " Port: " + port + " Version: " + httpVersion);
            // Forward request to target server
            try (
                Socket targetSocket = new Socket(host, port);
                PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))
            ) {
                // Forward request
                targetOut.println(method + " " + path + " " + httpVersion);
                System.out.println("Forwarded request: " + method + " " + path + " " + httpVersion);
                targetOut.println("Host: " + host);
                targetOut.println();
                
                String line;
                while ((line = targetIn.readLine()) != null) {
                    out.println(line);
                }

            } catch (IOException e) {
                out.println("HTTP/1.1 502 Bad Gateway");
                out.println("Content-Type: text/plain");
                out.println();
                out.println("Error connecting to target server");
            }

        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("222222222222222222222");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("333333333333333333333");
            }
        }
    }
}
