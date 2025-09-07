# Development Workflow

This document outlines the development workflow, Git processes, and collaboration standards for MyWorkoutLog. Following these guidelines ensures consistent development practices and smooth team collaboration.

## Table of Contents

- [Git Workflow](#git-workflow)
- [Branch Management](#branch-management)
- [Commit Standards](#commit-standards)
- [Code Review Process](#code-review-process)
- [Release Process](#release-process)
- [Collaboration Guidelines](#collaboration-guidelines)
- [Development Environment](#development-environment)
- [Continuous Integration](#continuous-integration)

## Git Workflow

We use a **feature branch workflow** based on Git Flow principles, optimized for Android development and continuous integration.

### Workflow Overview

```
master (production)
  ├── release/v1.2.0 (release preparation)
  ├── feature/workout-logger-improvements (new features)
  ├── bugfix/rest-timer-accuracy (bug fixes)
  ├── hotfix/critical-crash-fix (emergency fixes)
  └── docs/api-documentation-update (documentation)
```

### Branch Types

#### Main Branches
- **`master`**: Production-ready code, always deployable
- **`develop`**: Integration branch for ongoing development (optional for smaller teams)

#### Supporting Branches
- **`feature/*`**: New features and enhancements
- **`bugfix/*`**: Non-critical bug fixes
- **`hotfix/*`**: Critical production fixes
- **`release/*`**: Release preparation and final testing
- **`docs/*`**: Documentation updates

## Branch Management

### Branch Naming Convention

Use descriptive, kebab-case names with appropriate prefixes:

```bash
# Feature branches
feature/workout-logger-ui-improvements
feature/exercise-video-references
feature/cloud-backup-integration

# Bug fix branches
bugfix/rest-timer-accuracy-issue
bugfix/chart-rendering-on-small-screens
bugfix/exercise-deletion-confirmation

# Hotfix branches
hotfix/data-corruption-in-cloud-sync
hotfix/app-crash-on-android-14

# Release branches
release/v1.2.0
release/v2.0.0-beta

# Documentation branches
docs/contributing-guidelines
docs/api-documentation-update
```

### Creating Feature Branches

1. **Start from master**:
   ```bash
   git checkout master
   git pull origin master
   git checkout -b feature/your-feature-name
   ```

2. **Keep branch focused**: One feature or logical change per branch

3. **Regular updates**: Rebase frequently to avoid merge conflicts
   ```bash
   git checkout master
   git pull origin master
   git checkout feature/your-feature-name
   git rebase master
   ```

### Branch Lifecycle

#### Feature Development
```bash
# 1. Create feature branch
git checkout -b feature/workout-analytics-dashboard

# 2. Develop and commit regularly
git add .
git commit -m "feat: Add basic analytics dashboard layout"
git commit -m "feat: Implement workout volume charts"
git commit -m "test: Add unit tests for analytics calculations"

# 3. Push branch and create PR
git push -u origin feature/workout-analytics-dashboard

# 4. Address review feedback
git commit -m "fix: Address code review feedback on chart rendering"
git push origin feature/workout-analytics-dashboard

# 5. After approval, branch is merged and deleted
```

#### Hotfix Process
```bash
# 1. Create hotfix from master
git checkout master
git pull origin master
git checkout -b hotfix/critical-data-loss-fix

# 2. Implement fix
git commit -m "fix: Prevent data loss in workout completion flow"

# 3. Test thoroughly
git commit -m "test: Add regression tests for data persistence"

# 4. Merge to master and develop (if used)
# This is typically done through PR process
```

### Branch Protection Rules

#### Master Branch Protection
- **Require PR reviews**: Minimum 1 approval
- **Require status checks**: All CI tests must pass
- **Require up-to-date branches**: Branch must be current with master
- **No direct pushes**: All changes via pull requests
- **Dismiss stale reviews**: When new commits are pushed

#### Develop Branch Protection (if used)
- **Require PR reviews**: Minimum 1 approval for external contributors
- **Require status checks**: All CI tests must pass
- **Allow direct pushes**: For core team members only

## Commit Standards

### Conventional Commits

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### Commit Types

```bash
feat:     # New feature
fix:      # Bug fix
docs:     # Documentation changes
style:    # Code style changes (formatting, semicolons, etc.)
refactor: # Code refactoring without changing functionality
perf:     # Performance improvements
test:     # Adding or modifying tests
build:    # Changes to build system or dependencies
ci:       # Changes to CI configuration
chore:    # Maintenance tasks
```

#### Scope Examples

```bash
feat(ui): Add dark mode toggle to settings screen
fix(database): Resolve workout data corruption issue
docs(api): Update exercise management documentation
test(analytics): Add integration tests for chart generation
refactor(viewmodel): Extract common state management patterns
```

#### Examples

```bash
# Simple feature
feat: Add video reference selector to exercise logging

# Feature with scope and body
feat(workout-logger): Implement rest time tracking

Track actual rest time between sets and display progress
indicator with customizable timer duration.

# Bug fix with detailed description
fix(analytics): Resolve chart rendering issue on small screens

The workout volume charts were not properly scaling on devices
with screen width < 400dp. Updated chart dimensions to use
percentage-based sizing instead of fixed pixel values.

Fixes #123

# Breaking change
feat(database)!: Migrate to new exercise categorization system

BREAKING CHANGE: Exercise muscle group field changed from single
enum to list of enums to support multi-muscle exercises.

Migration guide available in docs/migration/v2.0.0.md
```

### Commit Best Practices

#### Atomic Commits
- **One logical change per commit**: Each commit should represent a complete, logical change
- **Compilable commits**: Each commit should leave the code in a working state
- **Focused commits**: Avoid mixing formatting changes with logic changes

```bash
# ✅ Good: Focused commits
git commit -m "feat: Add exercise search functionality"
git commit -m "style: Format exercise search component"
git commit -m "test: Add unit tests for exercise search"

# ❌ Bad: Mixed concerns
git commit -m "feat: Add exercise search, fix formatting, and add tests"
```

#### Commit Message Quality

```bash
# ✅ Good: Clear and descriptive
feat: Add video reference integration to workout logging
fix: Resolve rest timer accuracy issue in workout sessions
docs: Update API documentation for exercise endpoints

# ❌ Bad: Vague or unclear
feat: Add stuff
fix: Fix bug
docs: Update docs
```

### Interactive Rebase for Clean History

Use interactive rebase to clean up commit history before merging:

```bash
# Rebase last 3 commits
git rebase -i HEAD~3

# Example interactive rebase session
pick f7f3f6d feat: Add basic exercise search
squash 310154e fix: Typo in search function
squash a5f4a0d style: Format search component

# Result: Clean single commit
feat: Add exercise search functionality with proper formatting
```

## Code Review Process

### Pull Request Requirements

#### PR Title and Description
```markdown
feat: Add video reference integration to workout logging

## Summary
Integrates Android Photo Picker to allow users to attach video references
to logged sets for form checking and progress tracking.

## Type of Change
- [x] New feature (non-breaking change that adds functionality)
- [ ] Bug fix (non-breaking change that fixes an issue)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Implementation Details
- Added VideoReferenceSelector composable using ActivityResultContract
- Integrated video selection into expandable "Additional Options" section
- Updated LoggedSet data model to include videoReference URI field
- Added proper URI permission handling for video persistence

## Testing
- [x] Unit tests added for video URI handling
- [x] UI tests added for video selection flow
- [x] Manual testing on Galaxy Z Fold 6 (large screen)
- [x] Manual testing on Pixel 7 (standard screen)
- [x] Tested video playback and URI persistence

## Screenshots
[Include screenshots of the new video selection UI]

## Checklist
- [x] My code follows the project's style guidelines
- [x] I have performed a self-review of my own code
- [x] I have commented my code, particularly in hard-to-understand areas
- [x] I have made corresponding changes to the documentation
- [x] My changes generate no new warnings
- [x] I have added tests that prove my fix is effective or that my feature works
- [x] New and existing unit tests pass locally with my changes
- [x] I have tested the changes on multiple screen sizes
```

#### Before Creating PR

1. **Self-review**: Review your own code thoroughly
2. **Run tests**: Ensure all tests pass locally
3. **Check formatting**: Run code formatter (`./gradlew ktlintFormat`)
4. **Update documentation**: Update relevant docs if needed
5. **Test on devices**: Test on target devices/screen sizes
6. **Rebase on master**: Ensure branch is up-to-date

### Review Guidelines

#### For Authors
- **Respond promptly**: Address feedback within 24-48 hours
- **Be open to feedback**: Consider suggestions constructively
- **Explain decisions**: Clarify complex or controversial changes
- **Keep PRs manageable**: Aim for < 400 lines of changes when possible
- **Test suggestions**: Try out reviewer suggestions when reasonable

#### For Reviewers
- **Review promptly**: Provide feedback within 24-48 hours
- **Be constructive**: Focus on code quality and maintainability
- **Ask questions**: Seek clarification when logic is unclear
- **Suggest improvements**: Offer specific suggestions when possible
- **Consider user impact**: Think about how changes affect end users

### Review Checklist

#### Code Quality
- [ ] **Follows style guide**: Code adheres to established patterns
- [ ] **Proper error handling**: Errors are handled gracefully
- [ ] **Performance considerations**: No obvious performance issues
- [ ] **Security**: No security vulnerabilities introduced
- [ ] **Accessibility**: UI changes follow accessibility guidelines

#### Functionality
- [ ] **Requirements met**: PR addresses stated requirements
- [ ] **Edge cases handled**: Considers boundary conditions and error scenarios
- [ ] **Backward compatibility**: No breaking changes without proper versioning
- [ ] **Integration**: Changes work well with existing features

#### Testing
- [ ] **Adequate test coverage**: New code is properly tested
- [ ] **Test quality**: Tests are meaningful and maintainable
- [ ] **Manual testing**: Complex changes have been manually verified
- [ ] **Regression testing**: Existing functionality still works

#### Documentation
- [ ] **Code comments**: Complex logic is well-documented
- [ ] **API documentation**: Public interfaces are documented
- [ ] **User documentation**: User-facing changes are documented
- [ ] **Migration guides**: Breaking changes include migration instructions

### Approval Process

#### Approval Requirements
- **Minor changes**: 1 approval from any team member
- **Major features**: 2 approvals including 1 from core maintainer
- **Breaking changes**: All core maintainers must approve
- **Hotfixes**: 1 approval from core maintainer (expedited process)

#### Merge Strategies
- **Feature branches**: Squash and merge for clean history
- **Hotfixes**: Create merge commit to preserve urgency context
- **Documentation**: Squash and merge for clean history
- **Release branches**: Create merge commit to preserve release context

## Release Process

### Version Numbering

We use [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
1.0.0       # Initial release
1.1.0       # New features added
1.1.1       # Bug fixes
2.0.0       # Breaking changes
2.0.0-beta  # Pre-release
1.0.0+20230615  # Build metadata
```

#### Version Increment Rules
- **MAJOR**: Breaking changes, incompatible API changes
- **MINOR**: New features, backward-compatible functionality
- **PATCH**: Bug fixes, backward-compatible fixes

### Release Workflow

#### 1. Release Planning
```bash
# Create release branch from master
git checkout master
git pull origin master
git checkout -b release/v1.2.0

# Update version in build.gradle.kts
android {
    defaultConfig {
        versionCode = 12
        versionName = "1.2.0"
    }
}
```

#### 2. Release Preparation
```bash
# Update changelog
# Update documentation
# Final testing
# Address any critical issues

# Commit release preparation
git commit -m "chore: Prepare release v1.2.0"
git push origin release/v1.2.0
```

#### 3. Release Approval
- Create PR from release branch to master
- Comprehensive testing on target devices
- Final code review and approval
- Merge to master

#### 4. Release Deployment
```bash
# Tag the release
git checkout master
git pull origin master
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0

# Deploy to app stores (automated via CI)
```

#### 5. Post-Release
```bash
# Merge back to develop (if used)
git checkout develop
git merge master

# Delete release branch
git branch -d release/v1.2.0
git push origin --delete release/v1.2.0
```

### Release Notes

#### Format
```markdown
# MyWorkoutLog v1.2.0

## 🎯 New Features
- **Video References**: Add video attachments to logged sets for form checking
- **Enhanced Analytics**: New muscle group volume analysis with trend indicators
- **Large Screen Support**: Optimized UI for tablets and foldable devices

## 🐛 Bug Fixes
- Fixed rest timer accuracy issue in workout sessions
- Resolved chart rendering problems on small screens
- Corrected exercise deletion confirmation dialog behavior

## 🔧 Improvements
- Improved performance of workout history loading
- Enhanced accessibility support throughout the app
- Updated Material 3 components for better visual consistency

## ⚠️ Breaking Changes
- Database schema updated to support multi-muscle exercises
- API endpoints restructured for better RESTful design
- Configuration file format changed for cloud backup settings

## 📱 Compatibility
- **Minimum Android Version**: Android 8.0 (API 26)
- **Target Android Version**: Android 14 (API 34)
- **Recommended Devices**: All Android devices, optimized for Galaxy Z Fold series

## 🔄 Migration
For users upgrading from v1.1.x, please see the [migration guide](docs/migration/v1.2.0.md).

## 📊 Statistics
- **Lines of code added**: 2,847
- **Lines of code removed**: 1,203
- **Files changed**: 47
- **Test coverage**: 87%
```

## Collaboration Guidelines

### Communication

#### Issue Discussion
- **Use GitHub Issues**: For bug reports, feature requests, and discussions
- **Reference issues in commits**: Use "Fixes #123" or "Closes #123"
- **Label appropriately**: Use labels to categorize and prioritize issues
- **Provide context**: Include screenshots, device info, and steps to reproduce

#### Pull Request Discussion
- **Ask for clarification**: Don't hesitate to ask questions
- **Suggest alternatives**: Offer different approaches when appropriate
- **Be specific**: Point to specific lines and provide actionable feedback
- **Stay focused**: Keep discussions relevant to the PR scope

### Conflict Resolution

#### Code Conflicts
1. **Discuss technically**: Focus on technical merits of different approaches
2. **Seek consensus**: Try to find mutually agreeable solutions
3. **Escalate if needed**: Involve senior team members for major disagreements
4. **Document decisions**: Record architectural decisions in ADRs

#### Merge Conflicts
```bash
# Resolve merge conflicts during rebase
git checkout feature/your-branch
git rebase master

# Edit conflicted files
# Mark as resolved
git add .
git rebase --continue

# Force push (be careful!)
git push --force-with-lease origin feature/your-branch
```

### Team Responsibilities

#### Core Maintainers
- **Code review**: Review PRs within 24-48 hours
- **Architecture guidance**: Make decisions on major architectural changes
- **Release management**: Oversee release process and quality
- **Mentoring**: Help onboard new contributors

#### Contributors
- **Follow guidelines**: Adhere to established coding and process standards
- **Test thoroughly**: Ensure changes are well-tested
- **Communicate clearly**: Provide clear PR descriptions and commit messages
- **Be responsive**: Address review feedback promptly

#### Community Members
- **Report issues**: Help identify bugs and suggest improvements
- **Contribute documentation**: Improve guides and documentation
- **Share feedback**: Provide user experience feedback
- **Help others**: Answer questions and provide support

## Development Environment

### Required Setup

```bash
# 1. Clone repository
git clone https://github.com/yourusername/MyWorkoutLog.git
cd MyWorkoutLog

# 2. Setup Git hooks
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# 3. Configure Git
git config user.name "Your Name"
git config user.email "your.email@example.com"

# 4. Install dependencies
./gradlew build

# 5. Run tests to verify setup
./gradlew test
```

### IDE Configuration

#### Android Studio Settings
```xml
<!-- .idea/codeStyles/Project.xml -->
<code_scheme name="MyWorkoutLog">
  <option name="RIGHT_MARGIN" value="120" />
  <option name="WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN" value="true" />
  
  <!-- Kotlin style settings -->
  <codeStyleSettings language="kotlin">
    <option name="CALL_PARAMETERS_WRAP" value="5" />
    <option name="METHOD_PARAMETERS_WRAP" value="5" />
    <option name="EXTENDS_LIST_WRAP" value="1" />
    <option name="METHOD_CALL_CHAIN_WRAP" value="5" />
    <option name="ASSIGNMENT_WRAP" value="1" />
  </codeStyleSettings>
</code_scheme>
```

### Development Scripts

```bash
# scripts/dev-setup.sh
#!/bin/bash
echo "Setting up MyWorkoutLog development environment..."

# Check Android Studio installation
if ! command -v studio &> /dev/null; then
    echo "Android Studio not found. Please install it first."
    exit 1
fi

# Setup Git hooks
cp scripts/pre-commit .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit

# Install dependencies
./gradlew build

# Verify setup
./gradlew test

echo "Development environment setup complete!"
```

## Continuous Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run lint
      run: ./gradlew lint
      
    - name: Run unit tests
      run: ./gradlew test
      
    - name: Generate test coverage report
      run: ./gradlew jacocoTestReport
      
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v3
      with:
        file: app/build/reports/jacoco/test/jacocoTestReport.xml
        
  build:
    needs: test
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Build debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload debug APK
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/app-debug.apk
```

### Quality Gates

#### Pre-merge Requirements
- [ ] All automated tests pass
- [ ] Code coverage meets minimum threshold (80%)
- [ ] No lint violations
- [ ] Security scan passes
- [ ] At least 1 code review approval

#### Pre-release Requirements
- [ ] All integration tests pass
- [ ] Performance benchmarks meet targets
- [ ] Security audit completed
- [ ] Documentation updated
- [ ] Manual testing on target devices completed

---

Following this development workflow ensures consistent, high-quality development practices and smooth collaboration across the MyWorkoutLog team. When in doubt, prioritize code quality, clear communication, and user experience.