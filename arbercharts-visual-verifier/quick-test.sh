#!/bin/bash

# ArberCharts Visual Verifier - Quick Test Script
# This script starts the app and opens test pages automatically

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   ArberCharts Visual Verifier - Quick Test"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if port 8080 is available
if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "⚠️  Port 8080 is already in use!"
    echo "   Killing existing process..."
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    sleep 2
    echo "   ✓ Port freed"
    echo ""
fi

# Go to project directory
cd "$(dirname "$0")"

echo "🔨 Building project..."
mvn clean package -DskipTests -q
echo "   ✓ Build successful"
echo ""

echo "🚀 Starting Visual Verifier..."
echo "   (This will take 5-10 seconds)"
echo ""

# Start in background
mvn spring-boot:run > /tmp/verifier.log 2>&1 &
APP_PID=$!

# Wait for startup
MAX_WAIT=30
WAIT_COUNT=0
while [ $WAIT_COUNT -lt $MAX_WAIT ]; do
    if grep -q "Started VisualVerifierApplication" /tmp/verifier.log 2>/dev/null; then
        echo "   ✓ Application started successfully!"
        break
    fi
    if grep -q "APPLICATION FAILED TO START" /tmp/verifier.log 2>/dev/null; then
        echo "   ✗ Application failed to start!"
        echo ""
        tail -20 /tmp/verifier.log
        exit 1
    fi
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
    printf "."
done

if [ $WAIT_COUNT -eq $MAX_WAIT ]; then
    echo ""
    echo "   ✗ Timeout waiting for application to start"
    tail -20 /tmp/verifier.log
    kill $APP_PID 2>/dev/null
    exit 1
fi

echo ""
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   ✓ Visual Verifier is running!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📝 Test URLs:"
echo ""
echo "   Test Page (API Tests):"
echo "   http://localhost:8080/test.html"
echo ""
echo "   Main Application:"
echo "   http://localhost:8080"
echo ""
echo "   Health Check:"
echo "   http://localhost:8080/actuator/health"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test API
echo "🧪 Testing API..."
sleep 2
if curl -s -o /tmp/test-chart.png "http://localhost:8080/api/renderer?className=com.arbergashi.charts.render.standard.LineRenderer&width=800&height=480&theme=light"; then
    if file /tmp/test-chart.png | grep -q "PNG image"; then
        SIZE=$(ls -lh /tmp/test-chart.png | awk '{print $5}')
        echo "   ✓ API working! Generated PNG: $SIZE"
    else
        echo "   ✗ API returned invalid PNG"
    fi
else
    echo "   ✗ API call failed"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   Opening browser..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Open browser (macOS)
if command -v open >/dev/null 2>&1; then
    echo "   Opening test page..."
    open http://localhost:8080/test.html
    sleep 2
    echo "   Opening main app..."
    open http://localhost:8080
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "   Application is running in background"
echo "   PID: $APP_PID"
echo ""
echo "   To stop:"
echo "   kill $APP_PID"
echo ""
echo "   Or:"
echo "   lsof -ti:8080 | xargs kill -9"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✓ Done! Check your browser."
echo ""

# Keep script running to show logs
echo "Press Ctrl+C to stop the application and exit"
echo ""
tail -f /tmp/verifier.log

