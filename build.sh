#!/bin/bash
# build.sh

# Biên dịch
javac --module-path ~/Downloads/javafx-sdk-21/lib \
      --add-modules javafx.controls,javafx.fxml \
      -d bin $(find src/main/java -name "*.java")

# Copy resource
cp -r src/main/resources/* bin/

# Chạy (có thể đổi ClientFX thành class bạn muốn chạy)
java --module-path ~/Downloads/javafx-sdk-21/lib \
     --add-modules javafx.controls,javafx.fxml \
     -cp bin tienlen.client.ClientFX
