#!/bin/bash
# Double-click this file in Finder to start FIDES ID Management.
# macOS opens .command files in Terminal automatically.

cd "$(dirname "$0")" || exit 1

JAR="target/user-registration-system-0.0.1-SNAPSHOT.jar"
URL="http://localhost:8080"

echo "============================================"
echo "  FIDES ID Management — Starting..."
echo "============================================"
echo "Folder: $(pwd)"
echo

# Find a usable Java (Homebrew / JDK installs / PATH)
find_java() {
  if command -v java >/dev/null 2>&1; then
    if java -version >/dev/null 2>&1; then
      command -v java
      return 0
    fi
  fi
  for candidate in \
    "$JAVA_HOME/bin/java" \
    /Library/Java/JavaVirtualMachines/*/Contents/Home/bin/java \
    "$HOME/Library/Java/JavaVirtualMachines/*/Contents/Home/bin/java" \
    /opt/homebrew/opt/openjdk*/bin/java \
    /usr/local/opt/openjdk*/bin/java
  do
    # Expand globs
    for path in $candidate; do
      if [ -x "$path" ] && "$path" -version >/dev/null 2>&1; then
        echo "$path"
        return 0
      fi
    done
  done
  return 1
}

JAVA_BIN="$(find_java)" || {
  echo "ERROR: Java not found."
  echo "Install JDK 17+ (or open the project once in IntelliJ so JDK is available)."
  echo
  read -r -p "Press Enter to close..."
  exit 1
}

echo "Java: $JAVA_BIN"
"$JAVA_BIN" -version 2>&1 | head -1
echo

if [ ! -f "$JAR" ]; then
  echo "JAR not found: $JAR"
  echo "Building now (first time may take a few minutes)..."
  echo
  if [ -x "./mvnw" ]; then
    ./mvnw -DskipTests package || {
      echo
      echo "ERROR: Build failed."
      read -r -p "Press Enter to close..."
      exit 1
    }
  else
    echo "ERROR: mvnw not found. Run from the module folder that contains pom.xml."
    read -r -p "Press Enter to close..."
    exit 1
  fi
fi

# Free port 8080 if another instance is running
if lsof -ti :8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Port 8080 is busy — stopping old process..."
  lsof -ti :8080 -sTCP:LISTEN | xargs kill 2>/dev/null
  sleep 1
fi

echo "Opening browser: $URL"
open "$URL" 2>/dev/null || true
echo
echo "App is starting. Keep this window open."
echo "Stop the app: press Ctrl+C"
echo "============================================"
echo

"$JAVA_BIN" -jar "$JAR"
EXIT_CODE=$?

echo
echo "App stopped (exit $EXIT_CODE)."
read -r -p "Press Enter to close..."
exit "$EXIT_CODE"
