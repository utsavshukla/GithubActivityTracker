#!/bin/bash
# Setup PAT
redis-cli SET "PAT:testuser" "1d0118933766e0e786fe2cbbbc967e40cbab1e95f2603304be84766a93fac2c5"


# Setup repositories
redis-cli HSET "repos:testuser" "my-web-app" '{"name":"my-web-app","description":"A modern web application built with React","recentCommits":[]}'
redis-cli HSET "repos:testuser" "data-processor" '{"name":"data-processor","description":"High-performance data processing pipeline","recentCommits":[]}'
redis-cli HSET "repos:testuser" "empty-repo" '{"name":"empty-repo","description":"New repository with no commits yet","recentCommits":[]}'


# Add 5 commits to my-web-app
for i in {1..5}; do
  redis-cli LPUSH "commits:testuser:my-web-app" "{\"message\":\"Feature update $i for web app\",\"author\":\"testuser\",\"timestamp\":\"2024-01-$(printf %02d $((15+i)))T10:$(printf %02d $((i*5)))00\"}"
done


# Add 25 commits to data-processor
for i in {1..25}; do
  redis-cli LPUSH "commits:testuser:data-processor" "{\"message\":\"Data processing improvement $i\",\"author\":\"testuser\",\"timestamp\":\"2024-01-$(printf %02d $i)T$(printf %02d $((9+i%12))):$(printf %02d $((i*3%60))):00\"}"
done


echo "Test data populated successfully!"
