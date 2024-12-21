import java.io.*;
import java.net.*;

public class ProxyServer {
    public static void main(String[] args) throws IOException {
        int proxyPort = 8888; // Port for the proxy server
        int targetPort = readPortNumber(); // Read the port number from the file
        ServerSocket serverSocket = new ServerSocket(proxyPort);
        System.out.println("Target server is listening on port: " + targetPort);
        System.out.println("Proxy server is listening on port: " + proxyPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket, targetPort)).start();
        }
    }

    private static int readPortNumber() throws IOException {
        File file = new File("portNumber.txt");
        if (!file.exists()) {
            return -1;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return Integer.parseInt(reader.readLine());
        }
    }

    private static void handleClient(Socket clientSocket, int targetPort) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            if (targetPort == -1) {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/plain");
                out.println("Content-Length: 13");
                out.println();
                out.println("Not Found");
                return;
            }

            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0]; // Holds GET
            String uri = requestParts[1]; // it holds "/URI"
            // requestParts[2] holds "HTTP/1.1"

            // Check if the method is GET and if the requested URI exceeds 9999
            if ("GET".equals(method)) {
                try {
                    int uriNum = Integer.parseInt(uri.substring(1));
                    if (uriNum > 9999) {
                        out.println("HTTP/1.1 414 Request-URI Too Long");
                        out.println("Content-Type: text/plain");
                        out.println("Content-Length: 23");
                        out.println();
                        out.println("Request-URI too long");
                        return;
                    }
                } catch (NumberFormatException e) {
                    // If not a number
                    if (!"GET".equals(uri)) {
                        return;
                    }
                }
            }

            // Forward the request to the target server
            try (Socket targetSocket = new Socket("localhost", targetPort);
                 PrintWriter targetOut = new PrintWriter(targetSocket.getOutputStream(), true);
                 BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()))) {

                // Send the request to the target server
                targetOut.println(requestLine);
                String headerLine;
                while (!(headerLine = in.readLine()).isEmpty()) {
                    targetOut.println(headerLine);
                }
                targetOut.println();
                
                // Read the response from the target server and send it back to the client
                String responseLine;
                while ((responseLine = targetIn.readLine()) != null) {
                    out.println(responseLine);
                    System.out.println(responseLine);
                }
            } catch (BindException e) {
                System.err.println("BindException: " + e.getMessage());
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
}
