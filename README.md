# HomeCloud Console Client & Secure Server
A secure client-server application written in Java, allowing users to interact with their own cloud-like file storage system through a console interface over TLS.

## ✨ Features
### ✅ User Authentication & Account Management
- Create new accounts

- Authenticate existing users

- Change passwords

- Delete accounts

### 🗃️ File & Folder Operations
- Upload / Download files

- Delete files

- Create / Delete folders

- Move files/folders

- List directory contents as a tree structure

### 🔐 Secure Communication
- Encrypted client-server communication over TLS

### 🧾 Logging & Monitoring
- Request and message logging

- Basic database-backed persistence of users and storage metadata

### 📦 Project Structure
- `Main.java`: Console client application entry point

- `ConsoleClientService.java`: Interactive client handling user input and sending requests

- `Server.java`: TLS-enabled server handling client connections and all request processing

## 🚀 Getting Started
### Prerequisites
- Java 17+

- Java Keystore (keytool) for generating TLS certificates

### Server Setup
- Generate TLS credentials:

```
# bash
keytool -genkeypair -alias serverkey -keyalg RSA -keysize 2048 -validity 365 -keystore server.keystore -storepass yourPassword
keytool -export -alias serverkey -keystore server.keystore -file server.crt -storepass yourPassword
keytool -import -alias servercert -file server.crt -keystore client.truststore -storepass changeit
# Save your storage directory path in storage_dir.txt.
# You will need to trust the certificate on the client with this setup
```

