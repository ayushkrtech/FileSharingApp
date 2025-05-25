
package server;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApp {

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(900)) {
            System.out.println("Server is listening on port 900...");

            while (true) {
                try (
                    Socket clientSocket = serverSocket.accept();
                    DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())
                ) {
                    String fileName = dataInputStream.readUTF();
                    long fileSize = dataInputStream.readLong();

                    File file = new File("received_" + fileName);
                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while (fileSize > 0 && (bytesRead = dataInputStream.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                            fileOutputStream.write(buffer, 0, bytesRead);
                            fileSize -= bytesRead;
                        }
                    }

                    System.out.println("Received file: " + file.getName());
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
