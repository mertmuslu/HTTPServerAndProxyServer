import java.io.*;
import java.net.*;
import java.util.Scanner;

public class HTTPServer {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the port number: ");
        int portNumber = scanner.nextInt();
        scanner.close();
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.print("Server is listening on port: " + portNumber);

        while (true) {
            // If there is no client, the accept() method will block the program until a client connects
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
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

            // Extract the requested URI from the request line
            String[] requestParts = requestLine.split(" ");
            String uri = requestParts[1];

            // Print client connection details for non-favicon requests
            System.out.println("New client connected");
            System.out.println("Client IP:" + clientSocket.getInetAddress().getHostAddress());

            // Determine the number of bytes to return based on the URI
            int numBytes;
            int controlBytes;
            
            try {
                controlBytes = Integer.parseInt(uri.substring(1));
                numBytes = ((controlBytes <= 20000) && (controlBytes >= 100) ? controlBytes : 0);
                if(numBytes == 0) {
                    throw new NumberFormatException();
                }

            } catch (NumberFormatException e) {
                out.println("HTTP/1.1 400 Bad Request");
                return;
            }

            // Calculate the header size
            String header = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\n\r\n";
            int headerSize = header.length();

            // Generate the HTML content
            String title = "<HTML><HEAD><TITLE>I am " + numBytes + " bytes long</TITLE></HEAD><BODY>";
            String footer = "</BODY></HTML>";
            int contentSize = numBytes - headerSize - title.length() - footer.length();

            // Generate a response with the specified number of bytes
            StringBuilder responseBody = new StringBuilder();
            for (int i = 0; i < contentSize; i++) {
                responseBody.append("a");
            }

            // Send HTTP response
            out.print(header);
            out.println(title + responseBody.toString() + footer);
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } 
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}