# Amethyst - A Mobile ToDo App for Obsidian with TaskNotes plugin.

A Kotlin Multiplatform todo app fully compatible with Obsidian's TaskNotes plugin.

## Features

### Core Functionality
- ✅ **Create, Edit, Delete Tasks** - Full CRUD operations for tasks
- ✅ **TaskNotes Format** - 100% compatible with Obsidian TaskNotes plugin
- ✅ **File-based Storage** - Tasks stored as Markdown files with YAML frontmatter
- ✅ **Search and Filter** - Search tasks by title, content, contexts, projects, and tags
- ✅ **Status Management** - Todo, In Progress, Done, Cancelled
- ✅ **Priority Levels** - Low, Medium, High, Urgent
- ✅ **Due Dates & Scheduling** - Set due dates and scheduled dates
- ✅ **Contexts & Projects** - Organize tasks with contexts (@work, @home) and projects
- ✅ **Time Tracking** - Start/stop timer, track time entries
- ✅ **Recurring Tasks** - RRULE format support for repeating tasks
- ✅ **Dark Mode** - Full dark mode support with theme toggle

### Platform Support
- **Android** (API 24+)
- **iOS** (ARM64 + Simulator)
- **Desktop** (JVM - macOS, Windows, Linux)
- **Web** (JavaScript + WebAssembly)

## TaskNotes Compatibility

Amethyst uses the exact same format as Obsidian's TaskNotes plugin:

### YAML Frontmatter Structure
```yaml
---
title: Task Title
status: todo | in-progress | done | cancelled
priority: low | medium | high | urgent
due: YYYY-MM-DD
scheduled: YYYY-MM-DD
contexts:
  - work
  - personal
projects:
  - Project Name
tags:
  - task
  - tag1
timeEstimate: 60  # minutes
timeEntries:
  - startTime: 2026-01-20T10:00:00Z
    endTime: 2026-01-20T11:00:00Z
recurrence: FREQ=WEEKLY;BYDAY=MO  # RRULE format
completeInstances:
  - 2026-01-13
createdAt: 2026-01-20T09:00:00Z
modifiedAt: 2026-01-20T10:00:00Z
---

Task content in markdown format...
```

### Supported Properties
- `title`: Task name
- `status`: Current state (todo/in-progress/done/cancelled)
- `priority`: Priority level (low/medium/high/urgent)
- `due`: Due date (YYYY-MM-DD)
- `scheduled`: Scheduled date (YYYY-MM-DD)
- `contexts`: Array of context tags
- `projects`: Array of related projects
- `tags`: Array of tags
- `timeEstimate`: Estimated duration in minutes
- `timeEntries`: Array of time tracking sessions
- `completedDate`: Date when task was completed
- `recurrence`: RRULE format string for recurring tasks
- `completeInstances`: Dates of completed recurring instances
- `createdAt`: Creation timestamp (ISO 8601)
- `modifiedAt`: Last modification timestamp (ISO 8601)

## Getting Started

### Initial Setup
1. Launch the app
2. On first run, you'll see the Settings screen
3. Enter the path to your Obsidian vault task folder
   - Example: `/Users/yourname/Documents/ObsidianVault/Tasks`
4. Click Browse (on Desktop) to select the folder
5. Tasks will automatically load from the specified folder

### Creating Tasks
1. Click the **+** button on the Task List screen
2. Fill in the task details:
   - Title (required)
   - Status and Priority
   - Due date and scheduled date
   - Contexts (e.g., @work, @home)
   - Projects
   - Tags
   - Time estimate
   - Notes
3. Click the Save button

### Time Tracking
1. Open a task
2. Scroll to the Time Tracking section
3. Click "Start Timer" to begin tracking
4. Click "Stop Timer" to end the session
5. All time entries are saved in the task file

### Search and Filter
- Use the search bar to find tasks by title, content, contexts, projects, or tags
- Click filter chips to filter by status (todo/in-progress/done)
- Click "Overdue" to show only overdue tasks
- Click "Filters" to filter by specific contexts or projects
- Click "Clear" to remove all filters

## Architecture

### Technology Stack
- **Kotlin Multiplatform** - Shared code across all platforms
- **Compose Multiplatform** - UI framework
- **Kotlinx DateTime** - Date and time handling
- **Kotlinx Coroutines** - Asynchronous operations

### Project Structure
```
Amethyst/
├── composeApp/              # Shared multiplatform library
│   └── src/
│       ├── commonMain/      # Shared code across all platforms
│       │   ├── model/       # Data models (Task, Status, Priority, etc.)
│       │   ├── data/        # Data layer (Repository, FileService, Serializer)
│       │   ├── viewmodel/   # ViewModels
│       │   ├── ui/
│       │   │   ├── screens/ # UI screens
│       │   │   └── theme/   # Theme and styling
│       │   └── App.kt       # Main app composable
│       ├── androidMain/     # Android-specific implementations
│       ├── iosMain/         # iOS-specific implementations
│       ├── jvmMain/         # Desktop-specific implementations
│       ├── jsMain/          # Web JS-specific implementations
│       └── wasmJsMain/      # Web Wasm-specific implementations
├── androidApp/              # Android application module
│   └── src/main/
│       ├── kotlin/          # Android MainActivity
│       ├── res/             # Android resources
│       └── AndroidManifest.xml
└── iosApp/                  # iOS application module
    └── iosApp/
        ├── iOSApp.swift     # SwiftUI wrapper
        └── ContentView.swift
```

### Key Components

#### Data Layer
- **Task Model** - Core task data structure with all TaskNotes properties
- **TaskSerializer** - Parses and serializes TaskNotes markdown format
- **FileService** - Platform-specific file I/O operations
- **TaskRepository** - Manages task CRUD operations and state

#### UI Layer
- **TaskListScreen** - Main screen with search, filter, and task list
- **TaskDetailScreen** - Create/edit task with all properties
- **SettingsScreen** - Configure Obsidian vault path
- **AmethystTheme** - Material 3 theme with dark mode support

## Building

### Android
```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Install on connected device/emulator
./gradlew :androidApp:installDebug

# Or open in Android Studio and run
```
Output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop (JVM)
```bash
# Run directly
./gradlew :composeApp:run

# Create distributable package
./gradlew :composeApp:packageDistributionForCurrentOS
```

### iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run

### Web
```bash
# WebAssembly (faster, modern browsers)
./gradlew :composeApp:wasmJsBrowserRun

# JavaScript (slower, wider compatibility)
./gradlew :composeApp:jsBrowserRun
```

## Compatibility with Obsidian

Tasks created in Amethyst can be opened and edited in Obsidian with the TaskNotes plugin, and vice versa. The app maintains full compatibility by:

1. Using identical YAML frontmatter structure
2. Storing tasks as `.md` files
3. Following TaskNotes naming conventions
4. Supporting all TaskNotes properties
5. Preserving markdown content

## Limitations

### Web Platform
- Web versions (JS/Wasm) don't have direct file system access
- Use Desktop or Mobile versions for full functionality

### File Picker
- Android and iOS: Manual path entry (folder picker integration requires additional permissions setup)
- Desktop (JVM): Full folder picker support via Swing
- Web: Not supported

## Future Enhancements

Potential features for future versions:
- Settings persistence (SharedPreferences/UserDefaults/LocalStorage)
- Full folder picker integration for Android/iOS
- Calendar view
- Task templates
- Bulk operations
- Sync status indicator
- Statistics and analytics
- Export/import functionality

## License

This is a demonstration project for TaskNotes compatibility.

## References

- [TaskNotes Plugin](https://github.com/callumalpass/tasknotes)
- [TaskNotes Documentation](https://tasknotes.dev/)
- [Obsidian](https://obsidian.md/)
- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/)
