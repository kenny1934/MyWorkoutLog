# Known Issues

**Last Updated**: 2025-09-08  
**Status**: Critical issues identified after Galaxy Z Fold 6 optimization completion

## 🚨 Critical Issues (HIGH PRIORITY)

### Issue #1: Large Screen Dashboard Missing Hidden Widget Recovery

**Status**: 🔴 **CRITICAL**  
**Discovered**: 2025-09-08  
**Impact**: High - Users lose access to widgets with no recovery method

#### Problem Description
The large screen dashboard implementation lacks the ability to show and restore hidden widgets, while the small screen version has this functionality through a "Hidden Widgets" section in customization mode.

#### Current Behavior
- **Small Screen (Single Column)**: 
  - Has "Hidden Widgets" section when `isCustomizationMode = true`
  - Users can tap hidden widgets to restore them
  - Located in `DashboardScreen.kt` lines 2376-2430

- **Large Screen (Multi-Column)**:
  - No hidden widget recovery mechanism
  - Users can hide widgets but cannot unhide them
  - Creates permanent widget loss scenario

#### Reproduction Steps
1. Open app on large screen device (Galaxy Z Fold 6)
2. Enter dashboard customization mode
3. Hide any widget using the hide button
4. Try to find a way to restore the hidden widget
5. **Result**: No UI exists to restore hidden widgets

#### Expected Behavior
Large screen dashboard should have equivalent hidden widget recovery functionality as small screen, either through:
- A collapsible "Hidden Widgets" section in customization mode
- A dedicated button/menu to access hidden widgets
- Integration with existing customization UI

#### Technical Notes
- Hidden widgets are tracked in `hiddenWidgets` list from DashboardViewModel
- Small screen implementation is in `DashboardContent` function
- Large screen uses different layout structure in multi-column grid
- Need to add hidden widget UI to large screen layout paths

#### Files Involved
- `DashboardScreen.kt` - Main dashboard implementation
- `DashboardViewModel.kt` - Widget state management

---

### Issue #2: No Delete Functionality for Templates and Programs

**Status**: 🔴 **CRITICAL**  
**Discovered**: 2025-09-08  
**Impact**: Medium-High - Database accumulation with no cleanup option

#### Problem Description
Users cannot delete workout templates or program blueprints, leading to database accumulation of unused items. While exercises can be deleted, templates and programs lack any delete functionality.

#### Current State Analysis

##### Templates (Partial Implementation)
- **DAO Layer**: `WorkoutTemplateDao.kt` has `deleteById(templateId: String)` method
- **UI Layer**: No delete button or confirmation dialog implemented
- **Status**: Backend ready, frontend missing

##### Programs (No Implementation)
- **DAO Layer**: `ProgramDao.kt` has no delete methods at all
- **UI Layer**: No delete functionality
- **Status**: Complete implementation needed

##### Comparison (Working Example)
- **Exercises**: Full delete implementation with confirmation dialogs
- **Location**: Exercise Management screen has delete buttons and proper safety confirmations

#### Reproduction Steps

**Templates**:
1. Navigate to Library → Manage Templates
2. Try to find delete option for any template
3. **Result**: No delete button or option available

**Programs**:
1. Navigate to Library → Manage Program Blueprints  
2. Try to find delete option for any program
3. **Result**: No delete button or option available

#### Expected Behavior
Both templates and programs should have:
- Delete buttons in their respective management screens
- Confirmation dialogs with safety warnings
- Proper error handling and user feedback
- Consistent UX with exercise deletion functionality

#### Implementation Requirements

##### Templates
- Add delete button to template list/detail views
- Implement delete confirmation dialog
- Connect to existing `WorkoutTemplateDao.deleteById()` method
- Add proper error handling and loading states

##### Programs  
- Add delete method to `ProgramDao.kt`
- Add delete button to program management screens
- Implement delete confirmation dialog with program details
- Consider cascade deletion of associated program sessions
- Add proper error handling and loading states

#### Technical Notes
- Reference exercise deletion implementation in `ExerciseManagementScreens.kt`
- Use similar confirmation dialog patterns as workout deletion
- Consider data relationships (templates may be used in programs, programs may have active cycles)
- Implement proper cascade deletion or prevent deletion of items in use

#### Files Involved
- `ProgramDao.kt` - Add delete methods
- `TemplateManagementScreens.kt` - Add delete UI
- `ProgramManagementScreens.kt` - Add delete UI
- `WorkoutTemplateViewModel.kt` - Add delete functionality
- `ProgramViewModel.kt` - Add delete functionality

## 📋 Issue Resolution Process

### Immediate Actions Required
1. **Fix Issue #1**: Add hidden widget recovery to large screen dashboard
2. **Fix Issue #2**: Implement complete delete functionality for templates and programs
3. **Testing**: Verify fixes on Galaxy Z Fold 6 and other large screen devices
4. **Documentation**: Update user-facing documentation with new delete capabilities

### Success Criteria
- [ ] Large screen users can recover hidden widgets through intuitive UI
- [ ] Users can delete templates with appropriate confirmation
- [ ] Users can delete programs with appropriate confirmation
- [ ] All delete operations include safety confirmations
- [ ] Consistent UX across all delete functionalities
- [ ] No data loss or corruption during delete operations

### Timeline
**Target Completion**: 1-2 days  
**Priority**: Must be completed before Advanced Personalization work begins

---

*These issues represent critical gaps in user experience that should be resolved immediately to maintain the production-ready status of the application.*