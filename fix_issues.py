#!/usr/bin/env python3
import re

# Read ClientFX.java
with open('g:\\EclipseProject\\TienLenTCP_v3_javafx\\TienLenTCP_v3\\src\\main\\java\\tienlen\\client\\ClientFX.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Fix 1: Line 107 - start() method - 480x380 to 600x500
content = content.replace(
    'Scene scene = new Scene(root, 480, 380);\n        primaryStage.setScene(scene);\n        primaryStage.setTitle("Tiến Lên Client");',
    'Scene scene = new Scene(root, 600, 500);\n        primaryStage.setScene(scene);\n        primaryStage.setTitle("Tiến Lên Client");'
)

# Fix 2: Line 253 - showRegisterScreen() - 500x450 to 600x500
content = content.replace(
    'Scene scene = new Scene(root, 500, 450);\n        primaryStage.setScene(scene);',
    'Scene scene = new Scene(root, 600, 500);\n        primaryStage.setScene(scene);'
)

# Fix 3: Line 315 - showLoginScreen() - 480x380 to 600x500
content = content.replace(
    'Scene scene = new Scene(root, 480, 380);\n        primaryStage.setScene(scene);\n        log("▶ Quay lại màn hình đăng nhập");',
    'Scene scene = new Scene(root, 600, 500);\n        primaryStage.setScene(scene);\n        log("▶ Quay lại màn hình đăng nhập");'
)

# Fix 4: Line 371 - showGameTable() - 1200x800 to 1600x1000
content = content.replace(
    'Scene gameScene = new Scene(gameTable.createRootPane(), 1200, 800);',
    'Scene gameScene = new Scene(gameTable.createRootPane(), 1600, 1000);'
)

# Write back
with open('g:\\EclipseProject\\TienLenTCP_v3_javafx\\TienLenTCP_v3\\src\\main\\java\\tienlen\\client\\ClientFX.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("ClientFX.java updated successfully!")

# Now update GameSession.java
with open('g:\\EclipseProject\\TienLenTCP_v3_javafx\\TienLenTCP_v3\\src\\main\\java\\tienlen\\server\\GameSession.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Add broadcastPlayerList() call after WIN message
# Find the endGame method and add the call
old_code = '''        Message ms = new Message("WIN", winnerName + "|" + balanceData.toString());
        for (Player p : players) {
            p.setPlaying(false);
            connections.get(p).sendMessage(Protocol.encode(ms));
        }
        
        setGameRunning(false);'''

new_code = '''        Message ms = new Message("WIN", winnerName + "|" + balanceData.toString());
        for (Player p : players) {
            p.setPlaying(false);
            connections.get(p).sendMessage(Protocol.encode(ms));
        }
        
        // Broadcast updated player list with new balances
        broadcastPlayerList();
        
        setGameRunning(false);'''

content = content.replace(old_code, new_code)

with open('g:\\EclipseProject\\TienLenTCP_v3_javafx\\TienLenTCP_v3\\src\\main\\java\\tienlen\\server\\GameSession.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("GameSession.java updated successfully!")
