import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {
    
    ServerSocket serverSocket;

    public WebServer(int portNumber) throws IOException {
        serverSocket = new ServerSocket(portNumber);
    }

    
    public class ClientHandler implements Runnable {
        private Socket clientSocket;
    
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            // Handle client request
        }
    }
}



