Simple Socket Chat (Java + Swing)

What this is
This is a tiny chat app made with plain Java sockets and a Swing user interface. It includes two small programs:

- `Server.java` — runs the server side and waits for one client.
- `Client.java` — connects to the server and sends/receives messages.

It shows:

- Quick way to test socket programming and a Swing chat UI.
- Shows messages in neat rounded bubbles with timestamps and simple scrollback.

Requirements

- Java JDK 8 or newer installed and `java`/`javac` on your PATH.
- Windows PowerShell or any terminal to run commands.

How to run locally (simple)

1. Open two terminal windows (PowerShell). One will be the server, the other the client.
2. In the project folder, compile both files:

```
javac Server.java Client.java
```

3. Start the server (it will ask for a port and a display name):

```
java Server.java
```

4. In the other terminal, start the client (it will ask for host, port and a display name):

```
java Client.java
```

5. Type a message in the input box and press Enter or click Send.

Notes about the UI

- Your messages appear on one side; incoming messages appear on the other.
- Each message shows a small time label (HH:mm).
- Long messages wrap inside a maximum bubble width so they stay readable.
- Auto-scroll only happens when you are already at the bottom; you can scroll back to read history.

Limitations

- The server accepts a single client connection (no group chat yet).
- Messages are not saved to disk; closing the program clears the chat.
