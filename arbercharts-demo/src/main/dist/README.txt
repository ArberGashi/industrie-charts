Industrie Charts Swing Demo @PROJECT_VERSION@

Runtime
- Eclipse Temurin 25 from Eclipse Adoptium.
- Use an existing installation when available. No commercial JDK is required.
- https://adoptium.net/

Start on macOS/Linux
  ./run-demo.sh

Start on Windows
  run-demo.bat

Manual start
  java --add-modules jdk.incubator.vector -jar @DEMO_JAR@

This directory belongs to one build version. Its launchers select the exact
JAR produced with them, even when older distributions exist elsewhere.
The Vector API flag enables optional acceleration; the library also supports
a scalar fallback when the module is unavailable.
