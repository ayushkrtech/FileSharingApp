package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Scanner;

public class ClientApp {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 8192;
    private static final int SOCKET_TIMEOUT = 30000; // 30 seconds
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB limit

    public static void main(String[] args) {
        ClientApp client = new ClientApp();
        client.run();
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== File Transfer Client ===");
        System.out.println("Server: " + SERVER_HOST + ":" + SERVER_PORT);
        System.out.println("Max file size: " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        System.out.println();
        
        while (true) {
            System.out.print("Enter file path to send (or 'quit' to exit): ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }
            
            if (input.isEmpty()) {
                System.out.println("Please enter a valid file path.");
                continue;
            }
            
            sendFile(input);
            System.out.println();
        }
        
        scanner.close();
    }
    
    private void sendFile(String filePath) {
        Path path = Paths.get(filePath);
        
        // Validate file
        if (!validateFile(path)) {
            return;
        }
        
        File file = path.toFile();
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            socket.setSoTimeout(SOCKET_TIMEOUT);
            
            System.out.println("Connected to server...");
            System.out.println("Sending file: " + file.getName() + " (" + formatFileSize(file.length()) + ")");
            
            try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                 DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                 FileInputStream fis = new FileInputStream(file)) {
                
                // Send file metadata
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                
                // Calculate and send file checksum
                String checksum = calculateChecksum(file);
                dos.writeUTF(checksum);
                dos.flush();
                
                // Send file data with progress tracking
                sendFileData(fis, dos, file.length());
                
                // Wait for server confirmation
                String response = dis.readUTF();
                if ("SUCCESS".equals(response)) {
                    System.out.println("✓ File sent successfully!");
                } else if ("CHECKSUM_MISMATCH".equals(response)) {
                    System.out.println("✗ File transfer failed: Checksum mismatch");
                } else {
                    System.out.println("✗ File transfer failed: " + response);
                }
                
            }
        } catch (SocketTimeoutException e) {
            System.out.println("✗ Transfer timed out. Please try again.");
        } catch (IOException e) {
            System.out.println("✗ Network error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
        }
    }
    
    private boolean validateFile(Path path) {
        if (!Files.exists(path)) {
            System.out.println("✗ File does not exist: " + path);
            return false;
        }
        
        if (!Files.isRegularFile(path)) {
            System.out.println("✗ Path is not a regular file: " + path);
            return false;
        }
        
        if (!Files.isReadable(path)) {
            System.out.println("✗ File is not readable: " + path);
            return false;
        }
        
        try {
            long fileSize = Files.size(path);
            if (fileSize > MAX_FILE_SIZE) {
                System.out.println("✗ File too large: " + formatFileSize(fileSize) + 
                                 " (max: " + formatFileSize(MAX_FILE_SIZE) + ")");
                return false;
            }
            
            if (fileSize == 0) {
                System.out.println("✗ File is empty: " + path);
                return false;
            }
        } catch (IOException e) {
            System.out.println("✗ Cannot read file size: " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    private void sendFileData(FileInputStream fis, DataOutputStream dos, long fileSize) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalSent = 0;
        int bytesRead;
        long lastProgressUpdate = System.currentTimeMillis();
        
        while ((bytesRead = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, bytesRead);
            totalSent += bytesRead;
            
            // Update progress every 500ms
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProgressUpdate > 500) {
                showProgress(totalSent, fileSize);
                lastProgressUpdate = currentTime;
            }
        }
        
        dos.flush();
        showProgress(totalSent, fileSize); // Final progress update
        System.out.println(); // New line after progress
    }
    
    private void showProgress(long sent, long total) {
        int percentage = (int) ((sent * 100) / total);
        int progressBars = percentage / 2; // 50 chars max
        
        System.out.print("\rProgress: [");
        for (int i = 0; i < 50; i++) {
            if (i < progressBars) {
                System.out.print("█");
            } else {
                System.out.print("░");
            }
        }
        System.out.print("] " + percentage + "% (" + formatFileSize(sent) + "/" + formatFileSize(total) + ")");
    }
    
    private String calculateChecksum(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file);
             DigestInputStream dis = new DigestInputStream(fis, md)) {
            
            byte[] buffer = new byte[BUFFER_SIZE];
            while (dis.read(buffer) != -1) {
                // Reading automatically updates the digest
            }
        }
        
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

class DigestInputStream extends FilterInputStream {
    private MessageDigest digest;
    
    public DigestInputStream(InputStream stream, MessageDigest digest) {
        super(stream);
        this.digest = digest;
    }
    
    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            digest.update((byte) b);
        }
        return b;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesRead = super.read(b, off, len);
        if (bytesRead != -1) {
            digest.update(b, off, bytesRead);
        }
        return bytesRead;
    }
}
