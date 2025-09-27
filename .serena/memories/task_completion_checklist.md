# Task Completion Checklist

## When Development Task is Complete

### Code Quality Checks
1. **Code Style Verification**
   - Ensure 4-space indentation for Java files
   - Check that `.editorconfig` rules are followed
   - Verify proper naming conventions (PascalCase classes, camelCase methods)
   - Confirm package organization follows `ninja.trek.*` structure

2. **Functionality Testing**
   - **Desktop**: `gradlew.bat :lwjgl3:run` - verify game runs without errors
   - **Build Verification**: `gradlew.bat build` - ensure all modules compile
   - **HTML Testing** (if relevant): `gradlew.bat :html:superDev` - check web version

3. **Component Integration**
   - Verify Entity/Component wiring works correctly
   - Test ActionList sequences if modified
   - Check that pooling/lifecycle management is proper
   - Ensure time rewind functionality still works (R key)

4. **Asset Management**
   - Confirm new assets are in `assets/` directory
   - Verify `assets.txt` auto-generation works
   - Check texture atlas integration if sprites added

### Documentation Updates
- Update README.md if architecture changes
- Add JavaDoc for new public APIs
- Update this checklist if new verification steps needed
- Document any new gradle tasks or dependencies

### Version Control
- Commit with descriptive, present-tense message
- Push to appropriate branch
- Create PR with clear description of changes
- Include testing notes and platform compatibility

### Performance Considerations
- Verify no memory leaks in component lifecycle
- Check that pooling is used appropriately
- Ensure render layers are properly managed
- Test with release build if performance-critical

### Multi-Platform Compatibility
- Android: Ensure no desktop-only dependencies in core
- HTML: Verify GWT-compatible code in core module
- Check that Box2D usage is platform-neutral