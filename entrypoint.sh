#!/usr/bin/env bash
set -euo pipefail

# ── Clean up stale X lock files from a previous container run ───────────────
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true

# ── Virtual X display (xkbcomp keysym warnings are harmless noise → /dev/null)
Xvfb :99 -screen 0 1280x800x24 -ac 2>/dev/null &
export DISPLAY=:99

# Poll until Xvfb is actually ready (avoids the race condition with fixed sleep)
echo "Waiting for Xvfb..."
timeout 15 bash -c 'until xdpyinfo -display :99 >/dev/null 2>&1; do sleep 0.3; done'
echo "Xvfb ready."

# ── VNC server (DPMS / xkbcomp noise → /dev/null) ───────────────────────────
VNC_PASSWORD=${VNC_PASSWORD:-}
if [ -n "${VNC_PASSWORD}" ]; then
    x11vnc -display :99 -passwd "${VNC_PASSWORD}" -listen 127.0.0.1 \
           -xkb -forever -quiet 2>/dev/null &
else
    x11vnc -display :99 -nopw -listen 127.0.0.1 \
           -xkb -forever -quiet 2>/dev/null &
fi

# ── noVNC web proxy (HTTP access logs → /dev/null) ───────────────────────────
websockify --web=/usr/share/novnc 6080 127.0.0.1:5900 >/dev/null 2>&1 &

echo "==> E5 & Dragons accessible at http://localhost:6080/vnc.html"

# ── Game ─────────────────────────────────────────────────────────────────────
exec sbt "endGame/run"
