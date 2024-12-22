import java.io.*;
import java.net.*;
import java.util.Scanner;

public class HTTPandProxyServer {
    private static int portNumber;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the mode (server/proxy): ");
        String mode = scanner.nextLine();

        if ("server".equalsIgnoreCase(mode)) {
            startHTTPServer(scanner);
        } else if ("proxy".equalsIgnoreCase(mode)) {
            startProxyServer();
        } else {
            System.out.println("Invalid mode. Please enter 'server' or 'proxy'.");
        }
    }

    private static void startHTTPServer(Scanner scanner) throws IOException {
        System.out.print("Enter the port number: ");
        portNumber = scanner.nextInt();
        savePortNumber(portNumber);

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server is listening on port: " + portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } finally {
            deletePortNumber();
        }
    }

    private static void startProxyServer() throws IOException {
        int proxyPort = 8888; // Port for the proxy server
        int targetPort = readPortNumber(); // Read the port number from the file
        ServerSocket serverSocket = new ServerSocket(proxyPort);
        System.out.println("Target server is listening on port: " + targetPort);
        System.out.println("Proxy server is listening on port: " + proxyPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleProxyClient(clientSocket, targetPort)).start();
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

    private static void savePortNumber(int portNumber) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter("portNumber.txt"))) {
            out.println(portNumber);
        }
    }

    private static void deletePortNumber() {
        File file = new File("portNumber.txt");
        if (file.exists()) {
            file.delete();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = in.readLine();
            System.out.println("Request: " + requestLine);

            // Extract the method and requested URI from the request line
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0]; // Holds GET
            String uri = requestParts[1]; // it holds "/URI"
            // requestParts[2] holds "HTTP/1.1"

            // Check if the method is GET
            if (!"GET".equals(method)) {
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println();
                out.println();
                System.out.println("Response: HTTP/1.1 501 Not Implemented");
                return;
            }

            // Determine the number of bytes to return based on the URI
            int numBytes;
            int uriNum;

            try {
                // discards "/" and turns the remaining string to integer
                uriNum = Integer.parseInt(uri.substring(1));
                numBytes = ((uriNum <= 20000) && (uriNum >= 100) ? uriNum : 0);
                if (numBytes == 0) {
                    throw new NumberFormatException();
                }

            } catch (NumberFormatException e) {
                out.println("HTTP/1.1 400 Bad Request");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println();
                out.println();
                System.out.println("Response: HTTP/1.1 400 Bad Request");
                return;
            }

            // Generate the HTML content
            String title = "<HTML><HEAD><TITLE>I am " + numBytes + " bytes long</TITLE></HEAD><BODY>";
            String footer = "</BODY></HTML>";
            int digitNum = (int) Math.log10(numBytes);
            int staticPartsLength = title.length() + footer.length() + digitNum;
            int headerSize = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n".length();
            int contentSize = numBytes - staticPartsLength - headerSize;

            // Generate a response with the specified number of bytes
            StringBuilder responseBody = new StringBuilder();
            for (int i = 0; i < contentSize; i++) {
                responseBody.append("a");
            }

            // Send HTTP response
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println(title + responseBody.toString() + footer);
            System.out.println("Response: HTTP/1.1 200 OK");

        } catch (IOException e) {
            System.err.println("Error closing client socket: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static void handleProxyClient(Socket clientSocket, int targetPort) {
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
