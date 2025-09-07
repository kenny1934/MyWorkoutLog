# Release Management

This document outlines the release management process for MyWorkoutLog, including version control strategy, release planning, deployment procedures, and post-release activities.

## Table of Contents

- [Release Strategy](#release-strategy)
- [Version Management](#version-management)
- [Release Planning](#release-planning)
- [Release Process](#release-process)
- [Deployment Pipeline](#deployment-pipeline)
- [Quality Assurance](#quality-assurance)
- [Post-Release Activities](#post-release-activities)
- [Rollback Procedures](#rollback-procedures)
- [Release Automation](#release-automation)

## Release Strategy

### Release Types

#### Regular Releases
- **Schedule**: Monthly major/minor releases
- **Content**: New features, improvements, non-critical bug fixes
- **Planning**: 2-week planning, 2-week development, 1-week testing
- **Approval**: Full QA cycle, stakeholder approval required

#### Patch Releases
- **Schedule**: As needed for bug fixes
- **Content**: Critical bug fixes, security patches
- **Planning**: Minimal planning, focused scope
- **Approval**: Technical lead approval, expedited testing

#### Hotfix Releases
- **Schedule**: Emergency releases within 24-48 hours
- **Content**: Critical security fixes, data corruption fixes, app crashes
- **Planning**: Immediate response, minimal scope
- **Approval**: CTO/Technical Director approval required

#### Beta Releases
- **Schedule**: 1-2 weeks before major releases
- **Content**: Feature-complete versions for testing
- **Distribution**: Internal testing, select power users
- **Purpose**: Final validation before production release

### Release Cadence

```
Month 1    Month 2    Month 3    Month 4
  |          |          |          |
  v1.1.0     v1.2.0     v1.3.0     v2.0.0
  ├─beta     ├─beta     ├─beta     ├─beta
  ├─patch    ├─patch    ├─patch    ├─patch
  └─hotfix   └─hotfix   └─hotfix   └─hotfix
```

## Version Management

### Semantic Versioning

We follow [Semantic Versioning 2.0.0](https://semver.org/):

```
MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]

Examples:
1.0.0        # Initial release
1.1.0        # New features added
1.1.1        # Bug fixes only
2.0.0        # Breaking changes
2.0.0-beta.1 # Pre-release
1.0.0+build.1 # Build metadata
```

#### Version Increment Rules

| Change Type | Version Impact | Example |
|------------|----------------|---------|
| Breaking changes | MAJOR | 1.5.2 → 2.0.0 |
| New features (backward compatible) | MINOR | 1.5.2 → 1.6.0 |
| Bug fixes (backward compatible) | PATCH | 1.5.2 → 1.5.3 |
| Pre-release versions | PRERELEASE | 2.0.0-alpha.1 |

### Version Code Management

Android version codes must always increment:

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        // Version code: MAJOR * 1000000 + MINOR * 10000 + PATCH * 100 + BUILD
        versionCode = 1020003  // Version 1.2.0, build 3
        versionName = "1.2.0"
    }
}
```

#### Version Code Calculation
```
Version 1.2.3 build 5:
1 * 1000000 + 2 * 10000 + 3 * 100 + 5 = 1020305

Version 2.0.0 build 1:
2 * 1000000 + 0 * 10000 + 0 * 100 + 1 = 2000001
```

### Branch Strategy for Releases

#### Release Branches
```bash
# Create release branch
git checkout -b release/v1.2.0 master

# Prepare release
# - Update version numbers
# - Update changelog
# - Final testing
# - Bug fixes only

# Merge to master and tag
git checkout master
git merge release/v1.2.0
git tag v1.2.0
git push origin master --tags

# Merge back to develop
git checkout develop
git merge master
```

#### Hotfix Branches
```bash
# Create hotfix from master
git checkout -b hotfix/v1.1.1 master

# Fix critical issue
git commit -m "fix: Resolve critical data corruption bug"

# Update version
# v1.1.0 → v1.1.1

# Merge to master and develop
git checkout master
git merge hotfix/v1.1.1
git tag v1.1.1

git checkout develop
git merge master
```

## Release Planning

### Release Planning Timeline

#### 4 Weeks Before Release (Planning Phase)
- **Week -4**: 
  - Define release scope and objectives
  - Create release milestone in GitHub
  - Identify feature owners and reviewers
  - Plan testing strategy

#### 3-2 Weeks Before Release (Development Phase)
- **Week -3**: 
  - Feature development begins
  - Daily standup tracking
  - Weekly progress reviews
- **Week -2**: 
  - Feature freeze for major features
  - Begin integration testing
  - Performance testing starts

#### 1 Week Before Release (Testing Phase)
- **Week -1**: 
  - Code freeze (bug fixes only)
  - Comprehensive QA testing
  - Beta release to internal testers
  - Final documentation updates

#### Release Week
- **Monday**: Final build and release candidate
- **Tuesday**: Final approval and deployment prep
- **Wednesday**: Production deployment
- **Thursday-Friday**: Monitor release and address issues

### Release Scope Definition

#### Feature Categorization
```markdown
## Release v1.2.0 Scope

### Must-Have (P0)
- [ ] Video reference integration for exercise logging
- [ ] Large screen UI optimization for Galaxy Z Fold
- [ ] Critical security patch for user data

### Should-Have (P1)
- [ ] Enhanced analytics dashboard
- [ ] Workout template improvements
- [ ] Performance optimizations

### Could-Have (P2)
- [ ] Additional chart types
- [ ] UI polish improvements
- [ ] Documentation updates

### Won't-Have (P3)
- [ ] Cloud backup redesign (moved to v1.3.0)
- [ ] Social sharing features (moved to v2.0.0)
```

### Risk Assessment

#### High-Risk Features
- Database schema changes
- Authentication system modifications
- Core workout logging changes
- Cloud sync alterations

#### Risk Mitigation
- **Database Changes**: Thorough migration testing, rollback procedures
- **Core Features**: Extended beta testing period
- **Third-party Integrations**: Fallback mechanisms
- **Performance Changes**: Benchmarking and monitoring

## Release Process

### Pre-Release Checklist

#### Code Quality
- [ ] All automated tests pass (unit, integration, UI)
- [ ] Code coverage meets minimum threshold (80%)
- [ ] No critical or high-severity security vulnerabilities
- [ ] Performance benchmarks meet targets
- [ ] Static analysis tools report no issues

#### Documentation
- [ ] CHANGELOG.md updated with all changes
- [ ] API documentation updated
- [ ] User documentation updated
- [ ] Migration guides created (if applicable)
- [ ] Release notes prepared

#### Testing
- [ ] Full regression testing completed
- [ ] Device compatibility testing on target devices
- [ ] Accessibility testing completed
- [ ] Performance testing on low-end devices
- [ ] Beta testing feedback addressed

#### Legal and Compliance
- [ ] Privacy policy updated (if applicable)
- [ ] Terms of service reviewed
- [ ] Third-party license compliance verified
- [ ] Data protection impact assessment (if applicable)

### Release Execution

#### 1. Create Release Branch
```bash
#!/bin/bash
# scripts/create-release.sh

set -e

VERSION=$1
if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.2.0"
    exit 1
fi

echo "Creating release branch for version $VERSION"

# Ensure we're on master and up to date
git checkout master
git pull origin master

# Create release branch
git checkout -b release/v$VERSION

# Update version in build.gradle.kts
# This would be automated with sed or a script

echo "Release branch release/v$VERSION created"
echo "Don't forget to:"
echo "1. Update version numbers"
echo "2. Update CHANGELOG.md"
echo "3. Test thoroughly"
echo "4. Create pull request to master"
```

#### 2. Version Updates
```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        versionCode = calculateVersionCode("1.2.0")
        versionName = "1.2.0"
    }
}

// Calculate version code programmatically
fun calculateVersionCode(version: String): Int {
    val parts = version.split(".")
    val major = parts[0].toInt()
    val minor = parts[1].toInt()
    val patch = parts[2].toInt()
    
    return major * 1000000 + minor * 10000 + patch * 100 + getBuildNumber()
}
```

#### 3. Changelog Update
```markdown
# Changelog

## [1.2.0] - 2023-12-01

### Added
- Video reference integration for workout logging
- Large screen optimization for foldable devices
- Enhanced analytics with muscle group analysis
- Workout template sharing functionality

### Changed
- Improved performance of workout history loading
- Updated Material 3 components throughout app
- Enhanced accessibility support

### Fixed
- Resolved rest timer accuracy issue
- Fixed chart rendering on small screens
- Corrected exercise deletion confirmation behavior

### Security
- Updated authentication library to latest version
- Enhanced data encryption for cloud backup

### Removed
- Deprecated legacy import format support

## [1.1.0] - 2023-11-01
...
```

#### 4. Release Build
```bash
#!/bin/bash
# scripts/build-release.sh

set -e

VERSION=$1

echo "Building release version $VERSION"

# Clean build
./gradlew clean

# Build release APK
./gradlew assembleRelease

# Build App Bundle for Play Store
./gradlew bundleRelease

# Run final tests
./gradlew test
./gradlew connectedAndroidTest

echo "Release build completed"
echo "APK: app/build/outputs/apk/release/app-release.apk"
echo "AAB: app/build/outputs/bundle/release/app-release.aab"
```

#### 5. Release Approval
```markdown
## Release Approval Checklist v1.2.0

### Technical Approval
- [ ] **Lead Developer**: @johndoe
- [ ] **QA Lead**: @janedoe  
- [ ] **DevOps**: @devops-team

### Business Approval
- [ ] **Product Manager**: @product-manager
- [ ] **Release Manager**: @release-manager

### Final Checks
- [ ] All release criteria met
- [ ] Risk assessment completed
- [ ] Rollback plan prepared
- [ ] Communication plan ready

**Approved by**: [Name], [Role], [Date]
**Deployment scheduled**: [Date and Time]
```

## Deployment Pipeline

### Automated Deployment

#### CI/CD Pipeline
```yaml
# .github/workflows/release.yml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Build release
      run: ./gradlew bundleRelease
      
    - name: Sign App Bundle
      uses: r0adkll/sign-android-release@v1
      with:
        releaseDirectory: app/build/outputs/bundle/release
        signingKeyBase64: ${{ secrets.SIGNING_KEY }}
        alias: ${{ secrets.ALIAS }}
        keyStorePassword: ${{ secrets.KEY_STORE_PASSWORD }}
        keyPassword: ${{ secrets.KEY_PASSWORD }}
        
    - name: Upload to Play Store
      uses: r0adkll/upload-google-play@v1.1.1
      with:
        serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT }}
        packageName: com.example.myworkoutlog
        releaseFiles: app/build/outputs/bundle/release/app-release.aab
        track: production
        status: completed
```

#### Deployment Stages

1. **Internal Testing** (Alpha)
   - Deploy to internal testing track
   - Limited to development team
   - Full feature access

2. **Beta Testing** (Beta)
   - Deploy to beta testing track
   - Extended to trusted users
   - Feedback collection enabled

3. **Staged Rollout** (Production)
   - Start with 5% of users
   - Monitor for 24 hours
   - Gradually increase to 100%

4. **Full Release**
   - Complete rollout to all users
   - Marketing campaign begins
   - Support team prepared

### Manual Deployment Steps

#### Google Play Store
```bash
# 1. Generate signed App Bundle
./gradlew bundleRelease

# 2. Upload to Play Console
# - Navigate to Play Console
# - Select app
# - Go to Release > Production
# - Upload new release

# 3. Configure release
# - Add release notes
# - Set rollout percentage
# - Configure targeting

# 4. Review and publish
# - Review all settings
# - Publish release
```

#### Internal Distribution
```bash
# 1. Build debug/internal APK
./gradlew assembleDebug

# 2. Distribute via Firebase App Distribution
firebase appdistribution:distribute app-debug.apk \
    --app 1:123456789:android:abcd1234 \
    --groups internal-testers \
    --release-notes "Internal testing release v1.2.0-beta"
```

## Quality Assurance

### Testing Strategy

#### Pre-Release Testing
1. **Automated Testing** (Continuous)
   - Unit tests (70% coverage minimum)
   - Integration tests (20% coverage)
   - UI tests (10% coverage)
   - Performance benchmarks

2. **Manual Testing** (Pre-release)
   - Device compatibility testing
   - Accessibility testing
   - Usability testing
   - Edge case testing

3. **Beta Testing** (1 week)
   - Internal dogfooding
   - External beta users
   - Crash reporting analysis
   - User feedback integration

#### Device Testing Matrix

| Category | Devices | Android Versions | Screen Sizes |
|----------|---------|------------------|--------------|
| **Primary** | Pixel 7, Galaxy S23 | Android 13, 14 | Standard (5.5-6.5") |
| **Foldable** | Galaxy Z Fold 6, Pixel Fold | Android 13, 14 | Large (7.6"+) |
| **Tablet** | Galaxy Tab S8, iPad (Android emulator) | Android 12+ | Tablet (10"+) |
| **Budget** | Galaxy A54, Pixel 6a | Android 12+ | Standard |
| **Legacy** | Galaxy S10, Pixel 4 | Android 10, 11 | Standard |

### Quality Gates

#### Automated Quality Gates
```yaml
quality_gates:
  - name: "Test Coverage"
    threshold: 80%
    type: "minimum"
    
  - name: "Performance Regression"
    threshold: 5%
    type: "maximum_increase"
    
  - name: "Security Vulnerabilities"
    threshold: 0
    type: "critical_high"
    
  - name: "App Size"
    threshold: 50MB
    type: "maximum"
    
  - name: "Startup Time"
    threshold: 2000ms
    type: "maximum"
```

#### Manual Quality Gates
- [ ] **Accessibility**: WCAG 2.1 AA compliance verified
- [ ] **Performance**: No regressions on target devices
- [ ] **Usability**: Key user journeys tested and verified
- [ ] **Compatibility**: Works on all supported devices and OS versions
- [ ] **Security**: Security review completed, no high-risk vulnerabilities

### Release Metrics

#### Success Metrics
- **Crash Rate**: < 0.5% of sessions
- **ANR Rate**: < 0.1% of sessions
- **User Rating**: Maintain > 4.5 stars
- **Adoption Rate**: > 80% within 30 days
- **Performance**: No regression in key metrics

#### Monitoring and Alerting
```yaml
alerts:
  - name: "High Crash Rate"
    condition: "crash_rate > 1%"
    action: "immediate_notification"
    
  - name: "Performance Regression"
    condition: "startup_time > 3000ms"
    action: "investigation_required"
    
  - name: "Low User Rating"
    condition: "rating < 4.0"
    action: "review_feedback"
```

## Post-Release Activities

### Release Monitoring

#### First 24 Hours
- [ ] Monitor crash reporting (Firebase Crashlytics)
- [ ] Check performance metrics (Firebase Performance)
- [ ] Monitor Play Store reviews and ratings
- [ ] Verify analytics are reporting correctly
- [ ] Check cloud sync functionality

#### First Week
- [ ] Analyze user adoption rates
- [ ] Review feature usage analytics
- [ ] Monitor support ticket volume
- [ ] Assess performance on different devices
- [ ] Gather user feedback

#### First Month
- [ ] Complete post-release review
- [ ] Update roadmap based on feedback
- [ ] Plan next release cycle
- [ ] Update documentation based on learnings

### Issue Response

#### Critical Issues (P0)
- **Response Time**: Within 2 hours
- **Resolution Time**: Within 24 hours
- **Actions**: Immediate hotfix release if necessary

#### High Priority Issues (P1)
- **Response Time**: Within 8 hours
- **Resolution Time**: Within 72 hours
- **Actions**: Patch release in next release cycle

#### Medium/Low Priority Issues (P2/P3)
- **Response Time**: Within 48 hours
- **Resolution Time**: Next release cycle
- **Actions**: Regular development process

### Communication

#### Release Announcement
```markdown
# MyWorkoutLog v1.2.0 is Now Available! 🎉

We're excited to announce the release of MyWorkoutLog v1.2.0, packed with new features and improvements to enhance your fitness tracking experience.

## 🆕 What's New
- **Video References**: Attach videos to your logged sets for form checking
- **Large Screen Support**: Optimized for tablets and foldable devices
- **Enhanced Analytics**: New muscle group analysis with trend indicators

## 🐛 Bug Fixes
- Fixed rest timer accuracy issues
- Improved chart rendering on small screens
- Better exercise deletion confirmation

## 📱 How to Update
The update is rolling out now on Google Play Store. You should receive it automatically within the next few days, or you can check for updates manually in the Play Store.

## 🛠 Need Help?
If you encounter any issues, please:
1. Check our [FAQ](link)
2. Contact support at support@myworkoutlog.app
3. Report bugs on our [GitHub Issues](link)

Thank you for using MyWorkoutLog!
```

#### Status Page Updates
```markdown
**December 1, 2023 - 10:00 AM UTC**
🟢 MyWorkoutLog v1.2.0 deployment completed successfully
- All systems operational
- Rollout: 100% of users
- No critical issues reported

**November 30, 2023 - 2:00 PM UTC** 
🟡 MyWorkoutLog v1.2.0 deployment in progress
- Rollout: 50% of users
- Monitoring performance metrics
- No issues reported so far
```

## Rollback Procedures

### Rollback Decision Matrix

| Scenario | Rollback Required | Action |
|----------|-------------------|---------|
| Crash rate > 2% | Yes | Immediate |
| Data corruption | Yes | Immediate |
| Security vulnerability | Yes | Immediate |
| Performance regression > 20% | Yes | Within 4 hours |
| Feature not working | No | Fix in next release |
| Minor UI issues | No | Fix in patch release |

### Rollback Process

#### Play Store Rollback
```bash
# 1. Access Play Console
# 2. Navigate to Release Management > App releases
# 3. Go to Production track
# 4. Select "Manage release"
# 5. Choose "Release rollback"
# 6. Confirm rollback to previous version
```

#### Emergency Rollback Automation
```yaml
# .github/workflows/emergency-rollback.yml
name: Emergency Rollback

on:
  workflow_dispatch:
    inputs:
      reason:
        description: 'Reason for rollback'
        required: true
      previous_version:
        description: 'Version to rollback to'
        required: true

jobs:
  rollback:
    runs-on: ubuntu-latest
    steps:
    - name: Rollback Play Store Release
      uses: r0adkll/upload-google-play@v1.1.1
      with:
        serviceAccountJsonPlainText: ${{ secrets.GOOGLE_PLAY_SERVICE_ACCOUNT }}
        packageName: com.example.myworkoutlog
        track: production
        status: halt
        
    - name: Notify Team
      uses: 8398a7/action-slack@v3
      with:
        status: custom
        custom_payload: |
          {
            "text": "🚨 Emergency rollback initiated",
            "attachments": [
              {
                "color": "danger",
                "fields": [
                  {
                    "title": "Reason",
                    "value": "${{ github.event.inputs.reason }}",
                    "short": false
                  },
                  {
                    "title": "Previous Version",
                    "value": "${{ github.event.inputs.previous_version }}",
                    "short": true
                  }
                ]
              }
            ]
          }
```

### Post-Rollback Activities

#### Immediate Actions (0-2 hours)
- [ ] Verify rollback completed successfully
- [ ] Monitor crash rates and performance
- [ ] Update status page
- [ ] Notify stakeholders
- [ ] Begin root cause analysis

#### Short Term (2-24 hours)
- [ ] Complete incident report
- [ ] Identify and fix root cause
- [ ] Plan corrected release
- [ ] Update testing procedures
- [ ] Review quality gates

#### Long Term (1-7 days)
- [ ] Conduct post-mortem meeting
- [ ] Update release procedures
- [ ] Implement additional safeguards
- [ ] Share learnings with team
- [ ] Plan prevention measures

## Release Automation

### Scripts and Tools

#### Version Management Script
```bash
#!/bin/bash
# scripts/version-manager.sh

set -e

COMMAND=$1
shift

case $COMMAND in
    "bump")
        TYPE=$1  # major, minor, patch
        ./scripts/bump-version.sh $TYPE
        ;;
    "release")
        VERSION=$1
        ./scripts/create-release.sh $VERSION
        ;;
    "deploy")
        ./scripts/deploy-release.sh
        ;;
    *)
        echo "Usage: $0 {bump|release|deploy} [args]"
        exit 1
        ;;
esac
```

#### Automated Changelog Generation
```bash
#!/bin/bash
# scripts/generate-changelog.sh

# Generate changelog from Git commits
git-changelog --output CHANGELOG.md \
    --format markdown \
    --sections "feat:Features,fix:Bug Fixes,perf:Performance,docs:Documentation" \
    --from-tag $(git describe --tags --abbrev=0) \
    --to-ref HEAD
```

#### Release Health Check
```bash
#!/bin/bash
# scripts/release-health-check.sh

echo "Performing release health check..."

# Check app is responding
curl -f https://api.myworkoutlog.app/health || exit 1

# Check crash rate
CRASH_RATE=$(curl -s https://api.firebase.google.com/v1beta1/projects/PROJECT_ID/crashlytics/data/crashFreePercent)
if (( $(echo "$CRASH_RATE < 99" | bc -l) )); then
    echo "⚠️  High crash rate detected: $CRASH_RATE%"
    exit 1
fi

# Check performance metrics
# Add performance checks here

echo "✅ Release health check passed"
```

### Monitoring and Alerts

#### Release Dashboard
```yaml
# monitoring/release-dashboard.yml
dashboard:
  name: "Release Monitoring"
  panels:
    - name: "Crash Rate"
      type: "metric"
      query: "crash_rate_percentage"
      threshold: 1.0
      
    - name: "User Adoption"
      type: "chart"
      query: "version_adoption_rate"
      timeframe: "30d"
      
    - name: "Performance"
      type: "metric"
      query: "app_startup_time"
      threshold: 2000
      
    - name: "User Rating"
      type: "metric"
      query: "play_store_rating"
      threshold: 4.0
```

#### Automated Notifications
```yaml
notifications:
  slack:
    webhook: ${{ secrets.SLACK_WEBHOOK }}
    channels:
      - "#releases"
      - "#alerts"
      
  email:
    recipients:
      - "tech-leads@company.com"
      - "product@company.com"
      
  conditions:
    critical:
      - "crash_rate > 2%"
      - "security_vulnerability_detected"
      
    warning:
      - "crash_rate > 1%"
      - "performance_regression > 10%"
      - "user_rating < 4.5"
```

---

This release management process ensures MyWorkoutLog maintains high quality standards while enabling rapid, reliable deployments. The combination of automation, quality gates, and monitoring provides confidence in our release pipeline while minimizing risk to users.