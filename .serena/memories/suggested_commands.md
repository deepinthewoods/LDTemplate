# Suggested Commands for LDTemplate Development

## Essential Development Commands (Windows)

### Running the Game
- **Desktop run**: `gradlew.bat :lwjgl3:run`
- **HTML dev server**: `gradlew.bat :html:superDev` → http://localhost:8080/html
- **Android run**: `gradlew.bat :android:run` (requires Android SDK/ADB)

### Building
- **Build all modules**: `gradlew.bat build`
- **Desktop JAR**: `gradlew.bat :lwjgl3:jar` → `lwjgl3/build/libs/`
- **HTML distribution**: `gradlew.bat :html:dist` → `html/build/dist/`

### Testing
- **Run tests**: `gradlew.bat test` (if tests exist)

### Gradle Management
- **Clean build**: `gradlew.bat clean`
- **Refresh dependencies**: `gradlew.bat --refresh-dependencies`
- **Gradle wrapper update**: `gradlew.bat wrapper --gradle-version=X.X`

## Windows System Commands

### File Operations
- **List directory**: `dir` or `ls` (if using Git Bash)
- **Find files**: `dir /s /b *.java` or `find . -name "*.java"`
- **Search in files**: `findstr /s /r "pattern" *.java`
- **Change directory**: `cd path\to\directory`

### Git Operations
- **Status**: `git status`
- **Add files**: `git add .` or `git add filename`
- **Commit**: `git commit -m "commit message"`
- **Push**: `git push origin main`
- **Pull**: `git pull origin main`
- **Branch**: `git checkout -b feature/branch-name`

### Asset Management
- **Generate asset list**: `gradlew.bat generateAssetList` (auto-run during build)

## Development Workflow
1. Make code changes in `core/src/main/java/ninja/trek/`
2. Test with `gradlew.bat :lwjgl3:run`
3. Verify multi-platform with `gradlew.bat build`
4. For HTML testing, use `gradlew.bat :html:superDev`
5. Commit changes with descriptive messages