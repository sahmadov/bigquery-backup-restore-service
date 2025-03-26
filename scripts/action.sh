#!/bin/bash

# Define endpoints
CLOUD_RUN_BASE_URL="https://bigquery-backup-restore-service-590805821164.europe-west3.run.app"
BACKUP_ENDPOINT="$CLOUD_RUN_BASE_URL/api/v1/backup"
RESTORE_ENDPOINT="$CLOUD_RUN_BASE_URL/api/v1/restore"
LOGGERS_ENDPOINT="$CLOUD_RUN_BASE_URL/actuator/loggers/com.bigquery.app"

# Usage function
usage() {
  echo "Usage: $0 <backup|restore> <path-to-request-file> [log-level]"
  echo "  <backup|restore>: Specify the operation (backup or restore)"
  echo "  <path-to-request-file>: JSON file containing request data"
  echo "  [log-level]: Optional. Set application log level (DEBUG, INFO, WARN, ERROR, etc.)"
  echo "               Default is INFO if not specified"
  exit 1
}

# Check if at least two arguments are provided
if [[ $# -lt 2 ]]; then
  usage
fi

# Assign arguments
ACTION=$1
REQUEST_FILE=$2
LOG_LEVEL=${3:-INFO}  # Default to INFO if no log level specified

# Validate the action
if [[ "$ACTION" != "backup" && "$ACTION" != "restore" ]]; then
  echo "Error: Invalid action '$ACTION'. Use 'backup' or 'restore'."
  usage
fi

# Validate the JSON file existence
if [[ ! -f "$REQUEST_FILE" ]]; then
  echo "Error: JSON file '$REQUEST_FILE' not found!"
  exit 1
fi

# Get authentication token from gcloud
AUTH_TOKEN=$(gcloud auth print-identity-token 2>/dev/null)
if [[ -z "$AUTH_TOKEN" ]]; then
  echo "Error: Unable to retrieve AUTH_TOKEN using gcloud."
  exit 1
fi

# Set log level using Spring Actuator
echo "Setting log level to $LOG_LEVEL for com.bigquery.app..."
LOGGER_PAYLOAD="{\"configuredLevel\": \"$LOG_LEVEL\"}"

LOG_RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}" -X POST \
  "$LOGGERS_ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "$LOGGER_PAYLOAD")

# Check if log level setting was successful
LOG_STATUS=$(echo "$LOG_RESPONSE" | grep "HTTP Status" | cut -d' ' -f3)
if [[ "$LOG_STATUS" =~ ^2[0-9]{2}$ ]]; then
  echo "Log level set successfully to $LOG_LEVEL"
else
  echo "Warning: Failed to set log level. Response:"
  echo "$LOG_RESPONSE"
  echo "Continuing with operation anyway..."
fi

# Read the JSON payload
JSON_PAYLOAD=$(cat "$REQUEST_FILE")

# Determine endpoint based on action
if [[ "$ACTION" == "backup" ]]; then
  ENDPOINT=$BACKUP_ENDPOINT
elif [[ "$ACTION" == "restore" ]]; then
  ENDPOINT=$RESTORE_ENDPOINT
fi

# Debugging: Print the payload being sent
echo "Sending request to: $ENDPOINT"
echo "Payload being sent:"
echo "$JSON_PAYLOAD"

# Send the request using curl
RESPONSE=$(curl -s -w "\nHTTP Status: %{http_code}" -X POST \
  "$ENDPOINT" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "$JSON_PAYLOAD")

# Print the response
echo "Response received:"
echo "$RESPONSE"

# Check if operation was successful
OP_STATUS=$(echo "$RESPONSE" | grep "HTTP Status" | cut -d' ' -f3)
if [[ "$OP_STATUS" == "200" ]]; then
  echo "Operation completed successfully"
else
  echo "Operation failed with status code $OP_STATUS"
fi