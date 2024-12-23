import java.io.*;
import java.net.*;
import java.util.Scanner;

public class HTTPServer {
    private static int portNumber;

    public static void main(String[] args) throws IOException {
        //try-with-resource statements to close scanner and serverSocket objects (they give warning about not being closed, not necessary to close them)
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the port number: ");
            portNumber = scanner.nextInt();
            savePortNumber(portNumber);
            //other try-with-resource statement to close serverSocket object
            try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
                System.out.println("Server is listening on port: " + portNumber);

                while (true) {
                    // If there is no client, the accept() method will block the program until a client connects
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } finally {
                // delete portNumber operations will be added here
            }
        }
    }

    public static int getPortNumber() {
        return portNumber;
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
        /*This try-with-resources statement gets the input and output streams from the client socket
        *Then it closes the bufferedReader and printWriter objects to make it reusable for other clients.
        *
        *InputStreamReader reads byte stream from incoming data and turns them to the character streams.
        *
        *PrintWriter gets the output stream of the client socket. This stream is used to send data to the client from server.
        *
        */
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
            

            // Print client connection details for non-favicon requests
            //System.out.println("New client connected");
            //System.out.println("Client IP:" + clientSocket.getInetAddress().getHostAddress());

            // Determine the number of bytes to return based on the URI
            int numBytes;
            int uriNum;
            
            try {
                // discards "/" and turns the remaining string to integer
                uriNum = Integer.parseInt(uri.substring(1)); 
                numBytes = ((uriNum <= 20000) && (uriNum >= 100) ? uriNum : 0);
                if(numBytes == 0) {
                    throw new NumberFormatException();
                }

            } 
            // If the URI is not between 100 and 20000, return a 400 Bad Request response
            catch (NumberFormatException e) {
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
            } 
            catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}