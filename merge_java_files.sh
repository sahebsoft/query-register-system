#!/bin/bash

OUTPUT_FILE="MERGED.md"

> "$OUTPUT_FILE"

echo "Merging all Java files into $OUTPUT_FILE..."
echo ""

echo "# RULES" >> "$OUTPUT_FILE"
echo "- THIS FILE IS THE SOURCE OF TRUTH - IT WILL BE AUTO UPDATED USING HOOKS" >> "$OUTPUT_FILE"
echo "- YOU DO NOT NEED TO READ JAVA FILES IN THIS LIB - ALL FILES ARE HERE AND AUTO UPDATED" >> "$OUTPUT_FILE"
echo "- NO NEED TO MAINTAIN BACKWARD COMPATIBILITY - IT'S A NEW LIB, NO ONE USES IT YET" >> "$OUTPUT_FILE"
echo "- FOLLOW SOLID PRINCIPLE , MY GOAL IS TO MAKE THIS LIB SMALLER WITHOUT BREAK ANY FEATURE" >> "$OUTPUT_FILE"
echo "- After each feature you code , run spring boot app using mvnw , and test rest api manually " >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "# Java Source Code Files" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "This document contains all Java source files from the project." >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"
echo "## Table of Contents" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

find src/main/java -name "*.java" -type f | sort | while read -r file; do
    filename=$(basename "$file")
    echo "- [$file](#$(echo "$file" | sed 's/[^a-zA-Z0-9]/-/g' | tr '[:upper:]' '[:lower:]'))" >> "$OUTPUT_FILE"
done

echo "" >> "$OUTPUT_FILE"
echo "---" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

find src/main/java -name "*.java" -type f | sort | while read -r file; do
    echo "Processing: $file"

    echo "## $file" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo '```java' >> "$OUTPUT_FILE"

    # Remove imports, comments, and empty lines
    sed -E \
        -e '/^import /d' \
        -e '/^package /d' \
        -e 's|//.*$||' \
        -e '/\/\*/,/\*\//d' \
        -e '/^[[:space:]]*$/d' \
        "$file" | \
    sed '/^$/N;/^\n$/d' >> "$OUTPUT_FILE"

    echo '```' >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    echo "---" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
done

total_files=$(find src/main/java -name "*.java" -type f | wc -l)
total_lines=$(wc -l < "$OUTPUT_FILE")

echo ""
echo "✓ Merged $total_files Java files"
echo "✓ Total lines: $total_lines"
echo "✓ Output file: $OUTPUT_FILE"