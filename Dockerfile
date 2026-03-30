# ── Base image: Eclipse Temurin Java 25 LTS (Ubuntu 24.04 Noble) ────────────
FROM eclipse-temurin:25-jdk-noble

# ── System deps: Swing libs + virtual display + VNC + noVNC ─────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    xvfb \
    x11vnc \
    novnc \
    websockify \
    x11-utils \
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    fonts-liberation \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# ── sbt (version must match project/build.properties) ───────────────────────
ARG SBT_VERSION=1.11.7
RUN curl -fsSL \
    "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
    | tar xz -C /usr/local \
    && ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app

# ── Layer 1: dependency cache ────────────────────────────────────────────────
# Copy only build metadata first so this layer is reused on source-only changes.
COPY build.sbt .
COPY project/ project/
RUN sbt update

# ── Layer 2: compile ─────────────────────────────────────────────────────────
COPY . .
RUN sbt endGame/compile

# ── Runtime ──────────────────────────────────────────────────────────────────
EXPOSE 6080

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
