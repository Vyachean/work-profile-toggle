#!/usr/bin/env sh

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
PROPERTIES_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Missing $PROPERTIES_FILE" >&2
  exit 1
fi

DISTRIBUTION_URL=$(sed -n 's/^distributionUrl=//p' "$PROPERTIES_FILE" | sed 's#\\:#:#')
GRADLE_VERSION=$(printf '%s\n' "$DISTRIBUTION_URL" | sed -n 's#.*/gradle-\([^-]*\)-bin.zip#\1#p')

if [ -z "$GRADLE_VERSION" ]; then
  echo "Unable to determine Gradle version from distributionUrl" >&2
  exit 1
fi

CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/manual-dists"
DIST_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"
ZIP_FILE="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_BIN="$DIST_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$CACHE_DIR"

  if [ ! -f "$ZIP_FILE" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl --fail --location --output "$ZIP_FILE" "$DISTRIBUTION_URL"
    elif command -v wget >/dev/null 2>&1; then
      wget --output-document="$ZIP_FILE" "$DISTRIBUTION_URL"
    else
      echo "curl or wget is required to download Gradle" >&2
      exit 1
    fi
  fi

  rm -rf "$DIST_DIR"
  mkdir -p "$DIST_DIR"
  unzip -q "$ZIP_FILE" -d "$CACHE_DIR"
fi

exec "$GRADLE_BIN" "$@"
