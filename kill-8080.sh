#!/bin/bash

# Kill any process running on port 8010
lsof -ti:8010 | xargs kill -9 2>/dev/null

if [ $? -eq 0 ]; then
    echo "Killed process on port 8010"
else
    echo "No process found on port 8010"
fi