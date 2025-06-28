#!/bin/bash

# Uygulama ve Filebeat logları aynı anda görülsün
echo "🔄 Uygulama ve Filebeat başlatılıyor..."

# Gradle bootRun
echo "🚀 Spring Boot uygulaması başlatılıyor..."
  ./gradlew :bootRun --args='--spring.profiles.active=local'
