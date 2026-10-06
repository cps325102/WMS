# Upgrade Plan: WMS (20260625011347)

- **Generated**: 2026-06-25 01:13:47
- **HEAD Branch**: N/A
- **HEAD Commit ID**: N/A

## Available Tools

**JDKs**
- JDK 17: not available (baseline will be skipped)
- JDK 21: **<TO_BE_INSTALLED>** (required by Step 1)

**Build Tools**
- Maven 3.9.9: D:\Software\apache-maven\apache-maven-3.9.9\bin
- Maven Wrapper: not present

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

## Options

- Working branch: appmod/java-upgrade-20260625011347
- Run tests before and after the upgrade: true

## Upgrade Goals

- Upgrade runtime target to Java 21

## Technology Stack

| Technology/Dependency | Current | Min Compatible | Why Incompatible |
| --------------------- | ------- | -------------- | ---------------------------------------------- |
| Java | 17 | 21 | User requested latest LTS runtime upgrade |
| Spring Boot | 3.3.5 | 3.3.5 | Current Spring Boot supports Java 21 |
| Maven | 3.9.9 | 3.9.9 | Compatible with Java 21 |
| `spring-boot-maven-plugin` | managed by Spring Boot | N/A | No explicit plugin upgrade required for Java 21 |

## Derived Upgrades

- Java 21 → Update `<java.version>` in `backend/pom.xml` from `17` to `21`.
- No Spring Boot version upgrade required because Spring Boot 3.3.5 is compatible with Java 21.
- No Maven build plugin version upgrade required for this project given Maven 3.9.9 and Spring Boot 3.3.5.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|------------|---------|--------|--------|--------|
| backend/pom.xml | `<java.version>` | 17 | upgrade | 21 | User requested runtime upgrade to latest LTS |

### Source Code Changes

| File | Location | Current | Required Change | Reason |
|------|----------|---------|----------------|--------|
| None | N/A | N/A | N/A | No code-level source changes are expected for a pure runtime target bump |

### Configuration Changes

| File | Property/Setting | Current | Required Change | Reason |
|------|------------------|---------|-----------------|--------|
| None | N/A | N/A | N/A | No application configuration changes identified for this runtime upgrade |

### CI/CD Changes

| File | Location | Current | Required Change |
|------|----------|---------|-----------------|
| None | N/A | N/A | N/A |

### Risks & Warnings

- **No version control available**: This workspace is not a git repository. All changes will be written without a commit history. Mitigation: record a clear note in the final summary and progress files.
- **Baseline JDK unavailable**: JDK 17 was not detected, so baseline compilation and tests will be skipped unless a base JDK is later found. Mitigation: verify the upgrade build on JDK 21 and document if the prior baseline could not be established.
- **Runtime-only incompatibility risk**: A pure target bump can still expose JDK 21 runtime issues in third-party libraries or reflection code. Mitigation: run full `mvn clean test` after the upgrade and resolve any failures.

## Upgrade Steps

- Step 1: Install Java 21
  - **Rationale**: The upgrade requires JDK 21 to compile and validate the project against the requested latest LTS runtime.
  - **Changes to Make**: Install JDK 21 and make it available to Maven; no source files are modified in this step.
  - **Verification**: Use JDK 21 with `mvn -version`; expected success.

- Step 2: Baseline Verification (skipped if no base JDK available)
  - **Rationale**: Capture current build/test health before the upgrade, if the original runtime is available.
  - **Changes to Make**: None; run baseline compile/test with the current JDK.
  - **Verification**: `mvn clean test-compile -q && mvn clean test -q` with the base JDK.

- Step 3: Update runtime target in Maven
  - **Rationale**: The only required code change for this upgrade is to raise the Maven Java target to Java 21.
  - **Changes to Make**: Apply `java.version` property update in `backend/pom.xml` from `17` to `21`.
  - **Verification**: `mvn clean test-compile -q` with JDK 21.

- Step 4: CVE Validation & Fix
  - **Rationale**: Confirm direct dependencies have no known CVE issues and fix any that are reported.
  - **Changes to Make**: Run a direct dependency CVE scan, update patched versions if needed.
  - **Verification**: `mvn clean test-compile -q`; confirm no remaining flagged CVEs.

- Step 5: Final Validation
  - **Rationale**: Ensure the project compiles and all tests pass under Java 21.
  - **Changes to Make**: Resolve any test failures revealed by the target upgrade.
  - **Verification**: `mvn clean test -q` with JDK 21.
