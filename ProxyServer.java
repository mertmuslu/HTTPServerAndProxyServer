import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ProxyServer {

    static int proxyPort = 8888;
    static String defaultTargetHost = "localhost";
    static int defaultTargetPort = 443;
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            //Read the request line and split it into parts
            String requestLine = in.readLine();
            System.out.println(requestLine);
            String requestType = requestLine.split(" ")[0];
            String requestURI = requestLine.split(" ")[1];
            String requestVersion = requestLine.split(" ")[2];

            
            
            //Parse the request URI to get the host, port, and path
            String host = "";
            int port = -1;
            String path = "";
            try {
                URI uri = new URI(requestURI);
                host = uri.getHost();
                port = uri.getPort();
                path = uri.getPath();
                
                //If the port is not specified, use the default target port
                if (port == -1) {
                    port = defaultTargetPort;
                }   
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            //Check if the requested file size is greater than the max request size
            // try {
            //     int requestedFileSize = Integer.parseInt(path.split("/")[1]);
            //     if (requestedFileSize > maxRequestSize) {
            //         out.println("HTTP/1.1 414 Request-URI Too Long");
            //         return;
            //     }
            // } catch (NumberFormatException e) {
            //     // If not a number
            //     System.out.println("Not a number");
            // }
            // catch (Exception e) {
            //     System.out.println("Exception:" + e.getMessage());
            // }
            

            //Forward the request to the target server
            try (Socket targetSocket = new Socket(host, port);
                 PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                 BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))) {
                
                // Normal request handling code here
                targetOut.println(requestLine);
                String headerLine;
                while (!(headerLine = in.readLine()).isEmpty()) {
                    targetOut.println(headerLine);
                    System.out.println(headerLine);
                }
                targetOut.println();

                // Read the response from the target server and send it back to the client
                String responseLine;
                while ((responseLine = targetIn.readLine()) != null) {
                    out.println(responseLine);
                    System.out.println(responseLine);
                }
                
            } catch (IOException e) {
                // Server not running/reachable
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/plain");
                out.println();
                out.println("Server not found or not running");
                return;
            }
            

            System.out.println("Host: " + host);
            System.out.println("Port: " + port);
            System.out.println("Path: " + path);


            
            // System.out.println("Request: " + requestLine);
            // String[] requestParts = requestLine.split(" ");
            // String method = requestParts[0]; // Holds GET
            // String uri = requestParts[1]; // it holds "/URI"
            // // requestParts[2] holds "HTTP/1.1"

            // // Check if the method is GET and if the requested URI exceeds 9999
            // if ("GET".equals(method)) {
            //     try {
            //         int uriNum = Integer.parseInt(uri.substring(1));
            //         if (uriNum > 9999) {
            //             out.println("HTTP/1.1 414 Request-URI Too Long");
            //             out.println("Content-Type: text/plain");
            //             out.println("Content-Length: 23");
            //             out.println();
            //             out.println("Request-URI too long");
            //             return;
            //         }
            //     } catch (NumberFormatException e) {
            //         // If not a number
            //         if (!"GET".equals(uri)) {
            //             return;
            //         }
            //     }
            // }

            // // Forward the request to the target server
            // try (Socket targetSocket = new Socket(targetHost, targetPort);
            //      PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
            //      BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))) {

            //     // Send the request to the target server
            //     targetOut.println(requestLine);
            //     String headerLine;
            //     while (!(headerLine = in.readLine()).isEmpty()) {
            //         targetOut.println(headerLine);
            //     }
            //     targetOut.println();
                
            //     // Read the response from the target server and send it back to the client
            //     String responseLine;
            //     while ((responseLine = targetIn.readLine()) != null) {
            //         out.println(responseLine);
            //         System.out.println(responseLine);
            //     }
            // } catch (BindException e) {
            //     System.err.println("BindException: " + e.getMessage());
            // }

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
}
