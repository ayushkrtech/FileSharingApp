# 📁 File Sharing App

A robust Java application for secure file transfer between clients and servers over TCP. This enhanced version includes integrity verification, progress tracking, concurrent client support, and comprehensive error handling.

## ✨ Features

### 🔒 Security & Integrity
- **SHA-256 Checksum Verification**: Ensures file integrity during transfer
- **File Size Validation**: Prevents excessively large file transfers (100MB default limit)
- **Path Traversal Protection**: Safe filename generation prevents directory traversal attacks
- **Input Sanitization**: Robust validation of all user inputs

### 🚀 Performance & Reliability
- **Concurrent Client Support**: Server handles up to 10 simultaneous connections
- **Buffered I/O**: Optimized with 8KB buffers for better performance
- **Connection Timeouts**: Prevents hanging connections (60s server, 30s client)
- **Progress Tracking**: Real-time progress bar with transfer speeds
- **Graceful Error Handling**: Comprehensive error messages and recovery

### 💻 User Experience
- **Interactive CLI**: User-friendly command-line interface
- **File Validation**: Pre-transfer validation (existence, readability, size)
- **Formatted Output**: Human-readable file sizes and timestamps
- **Unique Filenames**: Automatic timestamp-based naming to prevent conflicts
- **Continuous Operation**: Client can send multiple files without restarting

### 🏗️ Architecture
- **Multi-threaded Server**: Thread pool for handling concurrent clients
- **Graceful Shutdown**: Clean server shutdown with Ctrl+C
- **Organized File Storage**: Dedicated uploads directory
- **Connection Management**: Automatic client limit enforcement

## 🛠️ Technical Improvements

| Feature | Enhanced |
|---------|----------|
| Port | 9000 (standard non-privileged port) |
| Buffer Size | 8KB (optimized) |
| Error Handling | Comprehensive with specific error types |
| File Validation | Size, readability, existence checks |
| Progress Tracking | Real-time progress bar |
| Concurrent Clients | Up to 10 with thread pool |
| Data Integrity | SHA-256 checksum verification |
| File Naming | Timestamp-based unique naming |
| Timeouts | 30s client, 60s server |

## 💻 Prerequisites

- **Java 8 or higher**
- **Maven** (optional, for dependency management)
- **Terminal/Command Prompt**
- **Network access** between client and server

## 📦 Project Structure

```
FileTransferProject/
├── src/
│   ├── client/
│   │   └── ClientApp.java          # Enhanced client with progress tracking
│   └── server/
│       └── ServerApp.java          # Multi-threaded server with validation
├── uploads/                        # Auto-created directory for received files
├── out/                           # Compiled classes directory
└── README.md                      # This file
```

## 🚀 Quick Start

### Step 1: Navigate to Project Directory
```bash
cd FileTransferProject/
```

### Step 2: Compile the Application
```bash
# Create output directory
mkdir -p out

# Compile both client and server
javac -d out src/client/ClientApp.java src/server/ServerApp.java
```

### Step 3: Start the Server
```bash
java -cp out server.ServerApp
```
You should see:
```
=== File Transfer Server ===
Port: 9000
Upload directory: /path/to/FileTransferProject/uploads
Max file size: 100MB
Max concurrent clients: 10
Server started at: 2024-01-15 10:30:45
Waiting for clients...
```

### Step 4: Run the Client
```bash
# In a new terminal
java -cp out client.ClientApp
```

### Step 5: Transfer Files
```
=== File Transfer Client ===
Server: localhost:9000
Max file size: 100MB

Enter file path to send (or 'quit' to exit): /path/to/your/file.txt
Connected to server...
Sending file: file.txt (1.2 MB)
Progress: [████████████████████████████████████████████████] 100% (1.2 MB/1.2 MB)
✓ File sent successfully!

Enter file path to send (or 'quit' to exit): quit
Goodbye!
```

## 🔧 Configuration

### Server Configuration
Edit the constants in `ServerApp.java`:
```java
private static final int SERVER_PORT = 9000;           // Server port
private static final int MAX_CLIENTS = 10;             // Max concurrent clients
private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB limit
private static final String UPLOAD_DIR = "uploads";    // Upload directory
```

### Client Configuration
Edit the constants in `ClientApp.java`:
```java
private static final String SERVER_HOST = "localhost"; // Server hostname
private static final int SERVER_PORT = 9000;           // Server port
private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // File size limit
```

## 🧪 Testing

### Test with Different File Types
```bash
# Text files
java -cp out client.ClientApp
# Enter: test.txt

# Binary files
java -cp out client.ClientApp
# Enter: image.jpg

# Large files (up to 100MB)
java -cp out client.ClientApp
# Enter: largefile.zip
```

### Test Concurrent Connections
Run multiple client instances simultaneously:
```bash
# Terminal 1
java -cp out client.ClientApp

# Terminal 2
java -cp out client.ClientApp

# Terminal 3
java -cp out client.ClientApp
```

## 🛡️ Security Features

### File Validation
- Checks file existence and readability
- Validates file size limits
- Prevents empty file transfers
- Sanitizes filenames to prevent directory traversal

### Network Security
- Connection timeouts prevent resource exhaustion
- Client limits prevent DoS attacks
- Buffer size limits prevent memory exhaustion
- Graceful error handling prevents information leakage

### Data Integrity
- SHA-256 checksums verify file integrity
- Complete transfer verification
- Automatic cleanup of failed transfers

## 🐛 Troubleshooting

### Common Issues

**Port Already in Use**
```
Error: Address already in use
```
- Change `SERVER_PORT` to a different value (e.g., 9001)
- Check if another server is running: `netstat -ln | grep 9000`

**Permission Denied**
```
Error: Permission denied
```
- Ensure the file is readable: `ls -la filename`
- Check upload directory permissions: `ls -la uploads/`

**File Too Large**
```
✗ File too large: 150.0 MB (max: 100.0 MB)
```
- Increase `MAX_FILE_SIZE` in both client and server
- Or use a smaller file

**Connection Timeout**
```
✗ Transfer timed out. Please try again.
```
- Check network connectivity
- Increase timeout values in the code
- Ensure server is running and accessible

### Debugging

Enable verbose logging by adding debug prints:
```java
System.out.println("Debug: " + message);
```

Monitor network connections:
```bash
# Linux/Mac
netstat -an | grep 9000

# Windows
netstat -an | findstr 9000
```

## 🎯 Future Enhancements

- **SSL/TLS Encryption**: Secure data transmission
- **User Authentication**: Login-based access control
- **File Compression**: Automatic compression for faster transfers
- **Resume Capability**: Resume interrupted transfers
- **Web Interface**: Browser-based file upload
- **Database Integration**: Transfer history and user management
- **Configuration Files**: External configuration management
- **Logging Framework**: Structured logging with log levels

## 📝 License

This project is open source and available under the MIT License.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make improvements
4. Add tests
5. Submit a pull request

## 📞 Support

For questions or issues:
- Create an issue in the repository
- Check the troubleshooting section
- Review the code comments for implementation details
