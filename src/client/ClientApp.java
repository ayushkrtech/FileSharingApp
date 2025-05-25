
package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientApp {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter file path to send: ");
        String filePath = scanner.nextLine();

        try (
            Socket socket = new Socket("localhost", 900);
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            FileInputStream fileInputStream = new FileInputStream(filePath)
        ) {
            File file = new File(filePath);
            System.out.println("Sending file: " + file.getName());

            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
                dataOutputStream.flush();
            }

            System.out.println("File sent successfully.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
