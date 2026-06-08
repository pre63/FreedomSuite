#!/usr/bin/env bash
# Shared app list for publish / CI scripts.

FDROID_APPS=(inbox calendar messages auth files keyboard search)

app_module() {
  echo ":apps:$1"
}

app_gradle_file() {
  echo "apps/$1/build.gradle.kts"
}

app_package_id() {
  local app="$1"
  grep 'applicationId' "$(app_gradle_file "$app")" | head -1 | sed -E 's/.*"([^"]+)".*/\1/'
}

app_version_name() {
  local app="$1"
  grep 'versionName' "$(app_gradle_file "$app")" | head -1 | sed -E 's/.*"([^"]+)".*/\1/'
}

app_version_code() {
  local app="$1"
  grep 'versionCode' "$(app_gradle_file "$app")" | head -1 | grep -oE '[0-9]+'
}

is_valid_app() {
  local app="$1"
  local candidate
  for candidate in "${FDROID_APPS[@]}"; do
    if [[ "$candidate" == "$app" ]]; then
      return 0
    fi
  done
  return 1
}

resolve_apps_from_tag() {
  local tag="$1"
  if [[ "$tag" == release-* ]] || [[ "$tag" == suite-* ]] || [[ "$tag" == v[0-9]* ]]; then
    printf '%s\n' "${FDROID_APPS[@]}"
    return
  fi
  local app="${tag%%-v*}"
  if is_valid_app "$app"; then
    echo "$app"
    return
  fi
  printf '%s\n' "${FDROID_APPS[@]}"
}
