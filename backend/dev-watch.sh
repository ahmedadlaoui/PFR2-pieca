#!/bin/bash
echo "🚀 Starting Spring Boot in background..."
mvn spring-boot:run -Dspring-boot.run.profiles=docker &

echo "👀 Watching for Java file changes in src/main/java..."
# Using 'entr' to watch the src directory. Whenever a file changes, it runs 'mvn compile'
# We use a while loop because entr exits when new files are added if not using -d
while sleep 1; do
    find src/main/java src/main/resources -name "*.java" -o -name "*.properties" -o -name "*.xml" | entr -d mvn compile
done
