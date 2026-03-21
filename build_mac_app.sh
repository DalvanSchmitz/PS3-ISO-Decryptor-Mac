#!/bin/bash
# Script para compilar o projeto e gerar o .app para macOS

echo "Compilando com Maven..."
mvn clean package

echo "Construindo PS3Decryptor.app..."
APP_DIR="dist/PS3Decryptor.app"
JAR_FILE="target/ps3dec-ui-2.0.0.jar"

mkdir -p "$APP_DIR/Contents/MacOS"
mkdir -p "$APP_DIR/Contents/Java"
mkdir -p "$APP_DIR/Contents/Resources"

# Copiando o executável JAR
cp "$JAR_FILE" "$APP_DIR/Contents/Java/PS3Decryptor.jar"

# Copiando os assets (Ícones e Binários)
cp assets/AppIcon.icns "$APP_DIR/Contents/Resources/"
cp ps3decrs "$APP_DIR/Contents/Resources/"

# Gerando Info.plist
cat <<EOF > "$APP_DIR/Contents/Info.plist"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>PS3Decryptor</string>
    <key>CFBundleIdentifier</key>
    <string>com.ps3dec.app</string>
    <key>CFBundleVersion</key>
    <string>2.0.0</string>
    <key>CFBundleExecutable</key>
    <string>launcher</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
</dict>
</plist>
EOF

# Gerando Launcher BASH para macOS
cat <<'EOF' > "$APP_DIR/Contents/MacOS/launcher"
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_ROOT="$DIR/../.."
CONTENTS_DIR="$DIR/.."

export PATH="$CONTENTS_DIR/Resources:$PATH"

if /usr/libexec/java_home -v 11+ &> /dev/null; then
    JAVA_HOME=$(/usr/libexec/java_home -v 11+)
    JAVA_EXEC="$JAVA_HOME/bin/java"
elif command -v java &> /dev/null; then
    JAVA_EXEC="java"
else
    osascript -e 'display dialog "Java não encontrado. Instale o Java 11 ou superior." buttons {"OK"} default button "OK" with icon stop'
    exit 1
fi

exec "$JAVA_EXEC" -jar "$CONTENTS_DIR/Java/PS3Decryptor.jar"
EOF

chmod +x "$APP_DIR/Contents/MacOS/launcher"
chmod +x "$APP_DIR/Contents/Resources/ps3decrs"

# Força atualização do ícone no macOS
touch "$APP_DIR"

echo "Build concluído com sucesso. Aplicativo gerado em dist/PS3Decryptor.app"
