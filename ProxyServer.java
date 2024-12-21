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
        try (BufferedReader reader = new BufferedReader(new FileReader("portNumber.txt"))) {
            return Integer.parseInt(reader.readLine());
        }
    }

    private static void handleClient(Socket clientSocket, int targetPort) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

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
