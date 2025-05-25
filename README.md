# 📁 File Sharing App
A simple Java application that transfers a file from a **Client** to a **Server** over TCP. Built using Java Sockets and Streams, this project is great for learning network programming.

---


---

## 💻 Prerequisites

- Java 8 or higher
- Visual Studio Code (VS Code) or any terminal
- Java Extension Pack (if using VS Code)

---

## 🧪 How to Compile & Run

### ✅ Step 1: Open terminal in the project root

Ensure your terminal is inside `FileTransferProject/`, where the `src` folder is located.

### ✅ Step 2: Compile
    javac -d out src/client/ClientApp.java src/server/ServerApp.java

### ✅ Step 3: Run The Server
    java -cp out server.ServerApp

### ✅ Step 4: Run The Client
    java -cp out client.ClientApp

