---
name: Performance Issue
about: Report performance problems with BigQuery Backup Automation
title: '[PERF] '
labels: performance
assignees: ''
---

## Performance Issue Description
<!-- A clear and concise description of the performance issue you're experiencing. -->

## Environment Details
- **App Version**: <!-- e.g., 1.1.6 -->
- **Number of tables/datasets**: <!-- Approximate size of data being processed -->
- **GCP Resources**: <!-- Details of your Cloud Run configuration, memory, CPU, etc. -->
- **Network Environment**: <!-- Any relevant details about network conditions -->

## Reproduction Steps
<!-- How can we reproduce this performance issue? -->
1.
2.
3.

## Performance Metrics
<!-- Include any metrics you've gathered, such as response times, memory usage, CPU usage, etc. -->

## Expected Performance
<!-- What performance would you consider acceptable? -->

## Current Performance
<!-- What performance are you currently experiencing? -->

## Thread Configuration
<!-- If applicable, include your thread pool configuration -->
```
threadpool.backup.core-size=?
threadpool.backup.max-size=?
threadpool.backup.queue-capacity=?
```

## Logs or Profiles
<!-- Include any relevant logs or profiling data that shows the performance issue -->
```
Paste logs here
```

## Community Support
👍 If you are also experiencing this performance issue, please add a thumbs up reaction to help us prioritize it!