# Contributing to BigQuery Backup&Restore Automation

Thank you for your interest in contributing to the BigQuery Backup Automation project! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
    - [Development Environment Setup](#development-environment-setup)
    - [Project Structure](#project-structure)
- [Development Workflow](#development-workflow)
    - [Branching Strategy](#branching-strategy)
    - [Commit Guidelines](#commit-guidelines)
    - [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Documentation](#documentation)
- [CI/CD Pipeline](#cicd-pipeline)
- [Release Process](#release-process)
- [Communication](#communication)

## Code of Conduct

This project and everyone participating in it are governed by our Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Development Environment Setup

**Prerequisites:**

- JDK 17
- Maven 3.8+
- Docker
- Git
- Terraform (for infrastructure changes)
- GCP account with appropriate permissions

**Fork and Clone the Repository:**
```bash
git clone https://github.com/sahmadov/bigquery-backup-restore-service.git
cd bigquery-backup-restore-service
```

**Set Up Local Environment:**
```bash
# Install dependencies
mvn clean install -DskipTests

# Set up pre-commit hooks (optional)
# ...
```

**Configure GCP Credentials (for local testing):**

Create a service account with necessary permissions
Download credentials and set the GOOGLE_APPLICATION_CREDENTIALS environment variable

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your/service-account-key.json
```

### Project Structure
```
├── infrastructure/         # Terraform IaC files
├── src/
│   ├── main/
│   │   ├── java/          # Java source code
│   │   └── resources/     # Configuration files
│   └── test/              # Test files
├── .github/               # GitHub Actions workflows
├── Dockerfile             # Container definition
├── pom.xml                # Maven build configuration
└── README.md              # Project documentation
```

## Development Workflow

### Branching Strategy

We follow a branch-based workflow:

- `main` - Production-ready code. Protected branch.
- `feature/*` - New features or enhancements.
- `fix/*` - Bug fixes.
- `docs/*` - Documentation updates.
- `chore/*` - Maintenance tasks, dependency updates, etc.

### Commit Guidelines

Please follow these guidelines for commit messages:

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests after the first line

Consider using the following prefixes:

- `feat:` for new features
- `fix:` for bug fixes
- `docs:` for documentation changes
- `chore:` for maintenance tasks
- `test:` for adding or updating tests

### Pull Request Process

1. Ensure your code adheres to the coding standards
2. Update documentation as necessary
3. Include tests for new features or bug fixes
4. Verify that all tests pass and code coverage requirements are met
5. Create a pull request with a descriptive title and detailed description
6. Address any feedback from code reviewers
7. Once approved, a maintainer will merge your PR

## Coding Standards

- Follow standard Java coding conventions
- Use meaningful variable and method names
- Write clear comments and Javadoc where appropriate
- Keep methods short and focused on a single responsibility
- Use proper exception handling
- Format code according to the project's style (see .editorconfig)
- Use Lombok annotations to reduce boilerplate code

## Testing Guidelines

- Write unit tests for new code with JUnit 5
- Ensure all tests pass before submitting a PR
- Maintain or improve code coverage (minimum 75%)
- Write integration tests for significant features
- Mock external dependencies when appropriate

To run tests:
```bash
# Run all tests
mvn test

# Run with coverage report
mvn verify
```

## Documentation

- Update documentation for any changes to APIs, endpoints, or configuration
- Document public methods with Javadoc
- Keep README and other documentation up to date
- Use markdown for documentation files

## CI/CD Pipeline

Our project uses GitHub Actions for CI/CD:

- The pipeline runs on every PR and push to main
- It checks code style, runs tests, and builds artifacts
- For merged PRs to main, it builds and publishes Docker images
- It also manages releases with release-drafter

## Release Process

Releases are managed automatically via the release-drafter GitHub Action:

- PRs merged to main are categorized and added to the draft release
- Version numbers are determined by PR labels:
    - `major` - Breaking changes
    - `minor` - New features (default for feature branches)
    - `patch` - Bug fixes and minor changes (default)

When a release is published:

- A new tag is created
- The Docker image is tagged with the version and 'latest'
- The release notes are generated automatically

## Communication

- Use GitHub Issues for bug reports, feature requests, and discussions
- For security issues, please contact the maintainers directly
- Tag issues appropriately to help with organization

Thank you for contributing to BigQuery Backup Automation!