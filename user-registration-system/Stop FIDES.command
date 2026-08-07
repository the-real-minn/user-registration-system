#!/bin/bash
# Double-click to stop FIDES (frees port 8080).

cd "$(dirname "$0")" || exit 1

echo "Stopping FIDES on port 8080..."
if lsof -ti :8080 -sTCP:LISTEN >/dev/null 2>&1; then
  lsof -ti :8080 -sTCP:LISTEN | xargs kill 2>/dev/null
  sleep 1
  echo "Stopped."
else
  echo "Nothing was running on port 8080."
fi
echo
read -r -p "Press Enter to close..."
