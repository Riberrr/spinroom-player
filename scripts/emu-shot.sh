#!/usr/bin/env bash
# Press D-pad keys on the emulator and save a 1280x720 screenshot (TV dp size, ~4x fewer tokens than 4K).
# usage: bash scripts/emu-shot.sh <name> [keys...]
#   keys: U D L R C(OK) B(Back) H(long OK) M(Menu) or a raw keycode number
# env: ADB (default: SDK platform-tools), SERIAL (default: emulator-5554), OUT (default: $TMPDIR/spinroom-shots)
set -euo pipefail
export MSYS_NO_PATHCONV=1
ADB="${ADB:-C:/Users/lukas/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
SERIAL="${SERIAL:-emulator-5554}"
OUT="${OUT:-${TMPDIR:-/tmp}/spinroom-shots}"
mkdir -p "$OUT"
name="$1"; shift
adb() { "$ADB" -s "$SERIAL" "$@"; }
for k in "$@"; do
  case "$k" in
    U) adb shell input keyevent 19 ;; D) adb shell input keyevent 20 ;;
    L) adb shell input keyevent 21 ;; R) adb shell input keyevent 22 ;;
    C) adb shell input keyevent 23 ;; B) adb shell input keyevent 4 ;;
    M) adb shell input keyevent 82 ;; H) adb shell input keyevent --longpress 23 ;;
    *) adb shell input keyevent "$k" ;;
  esac
  sleep 0.4
done
sleep 1.2
raw="$OUT/raw.png"
adb exec-out screencap -p > "$raw"
if command -v powershell >/dev/null 2>&1; then
  ps1="$OUT/resize.ps1"
  cat > "$ps1" <<'PS'
param($in,$out)
Add-Type -AssemblyName System.Drawing
$img=[System.Drawing.Image]::FromFile($in)
$bmp=New-Object System.Drawing.Bitmap 1280,720
$g=[System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode='HighQualityBicubic'
$g.DrawImage($img,0,0,1280,720)
$bmp.Save($out,[System.Drawing.Imaging.ImageFormat]::Png)
$img.Dispose();$bmp.Dispose()
PS
  powershell -NoProfile -File "$(cygpath -w "$ps1" 2>/dev/null || echo "$ps1")" "$(cygpath -w "$raw" 2>/dev/null || echo "$raw")" "$(cygpath -w "$OUT/$name.png" 2>/dev/null || echo "$OUT/$name.png")"
else
  cp "$raw" "$OUT/$name.png"
fi
echo "$OUT/$name.png"
