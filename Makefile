# Freedom Suite — common developer operations
# Requires: JDK 17+, Android SDK (adb + emulator), Python 3 for ML scripts

SHELL := /bin/bash
ROOT := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

# Android SDK (override if needed: make ANDROID_SDK=/path install-dev)
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
  ANDROID_SDK ?= $(HOME)/Library/Android/sdk
else
  ANDROID_SDK ?= $(HOME)/Android/Sdk
endif

export ANDROID_SDK_ROOT := $(ANDROID_SDK)
export PATH := $(ANDROID_SDK)/platform-tools:$(ANDROID_SDK)/emulator:$(PATH)

GRADLE := ./gradlew --no-daemon
APPS := inbox calendar messages auth files keyboard search chat

.PHONY: help setup check-sdk devices emulator-start emulator-stop sim \
        build-dev build-prod install-dev install-app clean \
        test integration-test ml-test privacy-audit fdroid-verify ci-verify \
        dev-mail-server fetch-llm push-llm-model fetch-ml fetch-keyboard-models publish publish-local

help:
	@echo "Freedom Suite"
	@echo ""
	@echo "Setup"
	@echo "  make setup              SDK check + fetch ML assets"
	@echo "  make check-sdk          Verify adb/java/sdk paths"
	@echo "  make devices            List adb devices"
	@echo ""
	@echo "Emulator"
	@echo "  make emulator-start     Start AVD (AVD_NAME=GrokChat to override)"
	@echo "  make emulator-stop      Kill running emulators"
	@echo "  make sim                Start emulator + install all dev apps"
	@echo ""
	@echo "Build & install"
	@echo "  make build-dev          assembleDevDebug (all apps)"
	@echo "  make build-prod         assembleFdroidRelease (F-Droid APKs)"
	@echo "  make install-dev        Install devDebug on device/emulator"
	@echo "  make install-app APP=auth   Install one app (auth, files, …)"
	@echo ""
	@echo "Verify & test"
	@echo "  make dev-mail-server    Local IMAP/SMTP for Inbox testing (see docs/DEV-MAIL-SERVER.md)"
	@echo "  make test               Unit tests (all modules)"
	@echo "  make integration-test   Audits + mock server + ML tests"
	@echo "  make ml-test            ONNX model regression only"
	@echo "  make privacy-audit      Dependency + manifest audits"
	@echo "  make fdroid-verify      Full F-Droid CI gate"
	@echo "  make ci-verify          Same as fdroid-verify locally"
	@echo ""
	@echo "Assets & release"
	@echo "  make fetch-llm          GenAI AAR + on-device chat model (~420 MB)"
	@echo "  make push-llm-model     Push model to chat app on device/emulator"
	@echo "  make fetch-ml           Download + quantize vision ML models"
	@echo "  make clean              Gradle clean"
	@echo "  make publish            GitHub release (all apps)"
	@echo ""
	@echo "Notes"
	@echo "  • Use devDebug installs (org.freedomsuite.*.dev), not installDebug"
	@echo "  • x86 emulators: set freedom.includeEmulatorAbis=true in local.properties"

setup: check-sdk fetch-ml
	@echo "setup complete"

check-sdk:
	@test -d "$(ANDROID_SDK)" || (echo "Android SDK not found at $(ANDROID_SDK)"; exit 1)
	@command -v adb >/dev/null || (echo "adb missing — install platform-tools"; exit 1)
	@command -v java >/dev/null || (echo "java missing — install JDK 17+"; exit 1)
	@test -x "$(ROOT)gradlew" || (echo "gradlew missing"; exit 1)
	@echo "SDK: $(ANDROID_SDK)"
	@adb version | head -1
	@java -version 2>&1 | head -1

devices:
	@adb devices -l

emulator-start:
	@$(ROOT)scripts/emulator-start.sh

emulator-stop:
	@adb devices | awk '/^emulator-/{print $$1}' | while read -r serial; do \
		adb -s "$$serial" emu kill 2>/dev/null || true; \
	done
	@echo "Emulator stop requested"

sim: emulator-start install-dev
	@echo ""
	@echo "Simulator ready — dev apps installed."
	@echo "Open launcher on the emulator to use suite apps (Dev builds show \"(Dev)\" suffix)."
	@echo "Keyboard: Settings → System → Keyboard → enable Keyboard"

build-dev:
	$(GRADLE) assembleDevDebug

build-prod:
	$(GRADLE) assembleFdroidRelease

install-dev:
	@$(ROOT)scripts/install-dev-apps.sh

install-app:
	@test -n "$(APP)" || (echo "usage: make install-app APP=auth"; exit 1)
	@$(ROOT)scripts/fdroid-prebuild.sh
	$(GRADLE) :apps:$(APP):installDevDebug

clean:
	$(GRADLE) clean

test:
	$(GRADLE) test

dev-mail-server:
	@$(ROOT)scripts/dev-mail-server.sh

integration-test:
	$(GRADLE) integrationTest

ml-test:
	$(GRADLE) mlIntegrationTest

privacy-audit:
	$(GRADLE) privacyAudit

fdroid-verify ci-verify:
	@$(ROOT)scripts/ci-verify.sh

fetch-llm:
	@$(ROOT)scripts/fetch-genai-aar.sh
	@$(ROOT)scripts/fetch-llm-model.sh

push-llm-model:
	@$(ROOT)scripts/push-llm-model.sh

fetch-ml:
	@$(ROOT)scripts/fetch-ml-models.sh

fetch-keyboard-models:
	@FETCH_KEYBOARD_MODELS=1 $(ROOT)scripts/fdroid-prebuild.sh

publish:
	@$(ROOT)scripts/publish.sh

publish-local:
	@$(ROOT)scripts/publish.sh --local
