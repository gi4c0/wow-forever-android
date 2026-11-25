#!/usr/bin/env bash
# One-time setup of the gi4c0 fork's automatic upstream sync and releases (.github/workflows/fork-*.yml).
#
#   - creates the release signing key in ~/.wow-forever-custom/ (kept if it already exists; BACK THIS FOLDER UP)
#   - stores it as the fork's Actions secrets
#   - copies it to app/keystores/ so local custom builds are signed with the same key
#   - makes custom the default branch, enables Actions, the workflows and issues
#
# Needs gh logged in to the account that owns the fork (gh auth login). Safe to re-run.
set -euo pipefail

REPO="${FORK_REPO:-gi4c0/wow-forever-android}"
KEY_DIR="$HOME/.wow-forever-custom"
ALIAS="wowforever-custom"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17 ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
KEYTOOL="${JAVA_HOME:+$JAVA_HOME/bin/}keytool"

gh auth status > /dev/null 2>&1 || gh auth login

if [ ! -f "$KEY_DIR/release.jks" ]; then
    mkdir -p "$KEY_DIR"
    chmod 700 "$KEY_DIR"
    umask 077
    openssl rand -base64 32 | tr -d '/+=\n' > "$KEY_DIR/password"
    "$KEYTOOL" -genkeypair -keystore "$KEY_DIR/release.jks" -storetype PKCS12 -alias "$ALIAS" \
        -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=WoW Forever Custom" \
        -storepass:file "$KEY_DIR/password" -keypass:file "$KEY_DIR/password" > /dev/null
    echo "Created signing key in $KEY_DIR (back it up: losing it means reinstalling the app)."
else
    echo "Using existing signing key in $KEY_DIR."
fi

base64 < "$KEY_DIR/release.jks" | tr -d '\n' | gh secret set RELEASE_KEYSTORE_BASE64 -R "$REPO"
gh secret set RELEASE_STORE_PASSWORD -R "$REPO" < "$KEY_DIR/password"
gh secret set RELEASE_KEY_PASSWORD -R "$REPO" < "$KEY_DIR/password"
printf '%s' "$ALIAS" | gh secret set RELEASE_KEY_ALIAS -R "$REPO"
echo "Stored signing secrets in $REPO."

mkdir -p "$ROOT/app/keystores"
cp "$KEY_DIR/release.jks" "$ROOT/app/keystores/release.jks"
(
    umask 077
    {
        echo "storeFile=keystores/release.jks"
        echo "storePassword=$(cat "$KEY_DIR/password")"
        echo "keyAlias=$ALIAS"
        echo "keyPassword=$(cat "$KEY_DIR/password")"
    } > "$ROOT/app/keystores/keystore.properties"
)
echo "Local builds now use the same key (app/keystores/, gitignored)."

gh repo edit "$REPO" --default-branch custom --enable-issues
gh api -X PUT "repos/$REPO/actions/permissions" -F enabled=true -f allowed_actions=all > /dev/null
for wf in fork-sync.yml fork-release.yml; do
    gh workflow enable "$wf" -R "$REPO" 2> /dev/null || true
done
echo "Default branch is custom; Actions, workflows and issues are enabled."
