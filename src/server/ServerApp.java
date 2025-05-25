package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerApp {
    private static final int SERVER_PORT = 9000;
    private static final int BUFFER_SIZE = 8192;
    private static final int CLIENT_TIMEOUT = 60000; // 60 seconds
    private static final int MAX_CLIENTS = 10;
    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB limit

    private final ExecutorService clientThreadPool;
    private final AtomicInteger activeClients;
    private volatile boolean serverRunning;

    public ServerApp() {
        this.clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.activeClients = new AtomicInteger(0);
        this.serverRunning = true;
        
        // Create upload directory if it doesn't exist
        createUploadDirectory();
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static void main(String[] args) {
        new ServerApp().start();
    }

    public void start() {
        System.out.println("=== File Transfer Server ===");
        System.out.println("Port: " + SERVER_PORT);
        System.out.println("Upload directory: " + new File(UPLOAD_DIR).getAbsolutePath());
        System.out.println("Max file size: " + (MAX_FILE_SIZE / (1024 * 1024)) + "MB");
        System.out.println("Max concurrent clients: " + MAX_CLIENTS);
        System.out.println("Server started at: " + getCurrentTimestamp());
        System.out.println("Waiting for clients...\n");

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            serverSocket.setSoTimeout(1000); // Check for shutdown every second

            while (serverRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    
                    if (activeClients.get() >= MAX_CLIENTS) {
                        rejectClient(clientSocket, "Server at maximum capacity");
                        continue;
                    }
                    
                    activeClients.incrementAndGet();
                    clientThreadPool.submit(new ClientHandler(clientSocket));
                    
                } catch (SocketTimeoutException e) {
                    // Normal timeout to check if server should continue running
                    continue;
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void createUploadDirectory() {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to create upload directory: " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private void rejectClient(Socket clientSocket, String reason) {
        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
            dos.writeUTF("REJECTED: " + reason);
            dos.flush();
        } catch (IOException e) {
            // Ignore - client will see connection closed
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void shutdown() {
        System.out.println("\nShutting down server...");
        serverRunning = false;
        clientThreadPool.shutdown();
        System.out.println("Server stopped.");
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final String clientId;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.clientId = clientSocket.getRemoteSocketAddress().toString();
        }

        @Override
        public void run() {
            System.out.println("[" + getCurrentTimestamp() + "] Client connected: " + clientId);
            
            try {
                clientSocket.setSoTimeout(CLIENT_TIMEOUT);
                handleClient();
            } catch (Exception e) {
                System.err.println("[" + getCurrentTimestamp() + "] Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void handleClient() throws IOException {
            try (DataInputStream dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                 DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()))) {

                // Receive file metadata
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                String expectedChecksum = dis.readUTF();

                System.out.println("[" + getCurrentTimestamp() + "] " + clientId + " sending: " + fileName + 
                                 " (" + formatFileSize(fileSize) + ")");

                // Validate file size
                if (fileSize > MAX_FILE_SIZE) {
                    dos.writeUTF("FILE_TOO_LARGE");
                    dos.flush();
                    System.out.println("[" + getCurrentTimestamp() + "] Rejected " + fileName + ": file too large");
                    return;
                }

                // Generate unique filename to prevent conflicts
                String safeFileName = generateSafeFileName(fileName);
                File outputFile = new File(UPLOAD_DIR, safeFileName);

                // Receive file data
                String result = receiveFile(dis, outputFile, fileSize, expectedChecksum);
                
                // Send response to client
                dos.writeUTF(result);
                dos.flush();

                if ("SUCCESS".equals(result)) {
                    System.out.println("[" + getCurrentTimestamp() + "] ✓ Successfully received: " + safeFileName);
                } else {
                    System.out.println("[" + getCurrentTimestamp() + "] ✗ Failed to receive: " + fileName + " - " + result);
                    // Clean up failed file
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                }
            }
        }

        private String receiveFile(DataInputStream dis, File outputFile, long expectedFileSize, String expectedChecksum) {
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[BUFFER_SIZE];
                long totalReceived = 0;
                int bytesRead;

                while (totalReceived < expectedFileSize && 
                       (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, expectedFileSize - totalReceived))) != -1) {
                    
                    fos.write(buffer, 0, bytesRead);
                    md.update(buffer, 0, bytesRead);
                    totalReceived += bytesRead;
                }

                if (totalReceived != expectedFileSize) {
                    return "INCOMPLETE_TRANSFER";
                }

                // Verify checksum
                String actualChecksum = bytesToHex(md.digest());
                if (!actualChecksum.equals(expectedChecksum)) {
                    return "CHECKSUM_MISMATCH";
                }

                return "SUCCESS";

            } catch (Exception e) {
                return "WRITE_ERROR: " + e.getMessage();
            }
        }

        private String generateSafeFileName(String originalFileName) {
            // Remove dangerous characters and add timestamp to prevent conflicts
            String safeName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            
            int dotIndex = safeName.lastIndexOf('.');
            if (dotIndex > 0) {
                return safeName.substring(0, dotIndex) + "_" + timestamp + safeName.substring(dotIndex);
            } else {
                return safeName + "_" + timestamp;
            }
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        private void cleanup() {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            
            activeClients.decrementAndGet();
            System.out.println("[" + getCurrentTimestamp() + "] Client disconnected: " + clientId + 
                             " (Active clients: " + activeClients.get() + ")");
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
