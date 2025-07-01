#!/bin/bash

# Gradle bootRun
echo "🚀 Starting app..."
  ./gradlew :bootRun --args='--spring.profiles.active=local'
