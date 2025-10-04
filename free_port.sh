#!/bin/bash

# Kill process using port 8091 if exists
function free_port_8091() {
  local pid
  pid=$(lsof -t -i:8091)
  if [ -n "$pid" ]; then
    echo "Port 8091 is in use by PID $pid. Killing..."
    kill -9 "$pid"
  else
    echo "Port 8091 is free."
  fi
}

free_port_8091
