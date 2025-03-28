# Contributing to BigQuery Backup & Restore Automation

Thank you for your interest in contributing to this project! This document provides simple guidelines to help you get started.

## Getting Started

### Prerequisites

- JDK 17
- Maven 3.8+
- Docker
- Git
- GCP account with appropriate permissions

### Development Setup

1. Fork and clone the repository:
   ```bash
   git clone https://github.com/sahmadov/bigquery-backup-restore-service.git
   cd bigquery-backup-restore-service
   ```

2. Build the project:
   ```bash
   mvn clean install
   ```

## Development Workflow

### Branching

We use the following branch naming convention (this helps with automatic release notes):

- `feature/*` - For new features (triggers minor version)
- `fix/*` - For bug fixes (triggers patch version)
- `docs/*` - For documentation updates
- `chore/*` - For maintenance tasks
- `major/*` or `breaking/*` - For breaking changes (triggers major version)

### Pull Request Process

1. Create a branch with the appropriate prefix
2. Make your changes
3. Submit a PR with a descriptive title that follows the convention:
  - `feat: description` for features
  - `fix: description` for bug fixes
  - `docs: description` for documentation
  - `chore: description` for maintenance

The CI/CD pipeline will automatically build and deploy your PR to our public artifact registry.

### Testing Your Changes

Contributors are expected to:

1. Pull the image that was built from your PR
   ```bash
   docker pull europe-west3-docker.pkg.dev/bigquery-automation-454819/bigquery-service-repo/bigquery-backup-restore-service:your-pr-tag
   ```

2. Deploy it to your own GCP project
3. Test the changes in your environment

**Note:** I am working on streamlining this process further in the future.

## Code Standards

- Follow standard Java coding conventions
- Include appropriate tests for your changes
- Maintain minimum 75% code coverage

## Communication

- Use GitHub Issues for bug reports, feature requests, and questions
- Tag issues appropriately to help with organization

Thank you for contributing!