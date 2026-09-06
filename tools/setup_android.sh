#!/usr/bin/env bash
# Bootstraps a headless Android build toolchain (JDK 17 + SDK 35 + Gradle 8.9) into $TOOLS.
# Usage: bash tools/setup_android.sh && source ~/.local/env.sh
set -euo pipefail
TOOLS="${TOOLS:-$HOME/.local}"
mkdir -p "$TOOLS"
cd "$TOOLS"

echo "== [1/4] JDK 17 =="
if [ ! -d "$TOOLS/jdk17" ]; then
  curl -sL --retry 3 -o jdk17.tar.gz \
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
  mkdir -p jdk17 && tar -xzf jdk17.tar.gz -C jdk17 --strip-components=1 && rm jdk17.tar.gz
fi
export JAVA_HOME="$TOOLS/jdk17"
export PATH="$JAVA_HOME/bin:$PATH"

echo "== [2/4] Android cmdline-tools =="
export ANDROID_HOME="$TOOLS/android-sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  curl -sL --retry 3 -o cmdtools.zip \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  rm -rf cmdtmp && mkdir cmdtmp && unzip -q cmdtools.zip -d cmdtmp
  mv cmdtmp/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
  rm -rf cmdtmp cmdtools.zip
fi
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "== [3/4] SDK packages =="
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" 2>&1 | grep -v "^\[=" || true
yes | sdkmanager --licenses >/dev/null 2>&1 || true

echo "== [4/4] Gradle 8.9 =="
if [ ! -d "$TOOLS/gradle-8.9" ]; then
  curl -sL --retry 3 -o gradle.zip "https://services.gradle.org/distributions/gradle-8.9-bin.zip"
  unzip -q gradle.zip && rm gradle.zip
fi

mkdir -p "$TOOLS/gradle-home"
# Low-memory settings – suitable for a 2 GB build box. Remove on a normal workstation.
cat > "$TOOLS/gradle-home/gradle.properties" <<'EOF'
org.gradle.jvmargs=-Xmx1100m -XX:MaxMetaspaceSize=320m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8
org.gradle.parallel=false
org.gradle.workers.max=1
org.gradle.daemon=false
org.gradle.caching=true
kotlin.compiler.execution.strategy=in-process
kotlin.daemon.jvmargs=-Xmx512m
kotlin.incremental=false
EOF

cat > "$TOOLS/env.sh" <<EOF
export JAVA_HOME="$TOOLS/jdk17"
export ANDROID_HOME="$TOOLS/android-sdk"
export ANDROID_SDK_ROOT="$TOOLS/android-sdk"
export GRADLE_HOME="$TOOLS/gradle-8.9"
export PATH="\$JAVA_HOME/bin:\$GRADLE_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"
export GRADLE_USER_HOME="$TOOLS/gradle-home"
EOF
echo "SETUP_DONE"
