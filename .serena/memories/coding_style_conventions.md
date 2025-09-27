# Coding Style and Conventions

## Code Style Standards

### Formatting (.editorconfig enforced)
- **Indentation**: 4 spaces for Java, 2 spaces for Gradle files
- **Line endings**: LF (Unix style)
- **Encoding**: UTF-8
- **Trailing whitespace**: Trimmed
- **Final newline**: Required

### Java Naming Conventions
- **Classes**: PascalCase (e.g., `PlayerComponent`, `ActionList`)
- **Methods/Fields**: camelCase (e.g., `updateRender()`, `isFinished`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `LANE_DELAY`)
- **Packages**: lowercase with dots (e.g., `ninja.trek.Components`)

### Package Organization
- **Main package**: `ninja.trek`
- **Components**: `ninja.trek.Components`
- **Entities**: `ninja.trek.Entity`
- **Actions**: `ninja.trek.actionlist`
- **Platform-specific**: Keep in respective modules (`ninja.trek.lwjgl3`, etc.)

## Entity/Component Patterns

### Component Design
- Extend `ninja.trek.Components.Component`
- Provide no-arg constructor for auto-instantiation
- Use `@OptionalComponent` annotation for optional cross-references
- Keep components focused on single responsibility

### Entity Design
- Extend `ninja.trek.Entity.Entity`
- Declare component fields as public
- Let `init()` handle auto-wiring
- Use `@TimeField` for fields that need time tracking

### Action Design
- Extend `ninja.trek.actionlist.Action`
- Implement `Pool.Poolable` for memory efficiency
- Use lanes and blocking for coordination
- Call `delay(seconds)` for timed actions

## Lifecycle Management
- Always call `Pools.free()` when done with pooled objects
- Use `onAdded()/onRemove()` for resource management
- Implement `reset()` method for poolable classes

## Documentation
- Use JavaDoc for public APIs
- Inline comments for complex logic
- Keep README.md updated with architecture changes