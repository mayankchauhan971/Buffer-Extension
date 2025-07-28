#!/bin/bash

echo "Testing content flow..."

# Test with a simple POST request
curl -X POST http://localhost:8080/api/context \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Article",
    "fullText": "This is a test article about software development best practices. It covers testing, debugging, and clean code principles.",
    "description": "A comprehensive guide to software development",
    "url": "https://example.com/test",
    "headings": ["Introduction", "Best Practices", "Conclusion"]
  }' \
  --verbose

echo "Test completed."
