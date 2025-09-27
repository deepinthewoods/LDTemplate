# LDTemplate Project Overview

## Purpose
LDTemplate is a LibGDX-based game template that demonstrates:
- Entity/Component system with automatic wiring and lifecycle management
- ActionList system for sequencing gameplay actions
- Multi-platform game development (Desktop, Android, HTML5)
- Physics simulation with Box2D
- Time rewind/snapshot system using custom timecode annotations
- Parallax background rendering

## Tech Stack
- **Framework**: LibGDX 1.12.1
- **Language**: Java 11+
- **Build System**: Gradle with multi-module structure
- **Physics**: Box2D integration
- **UI**: VisUI (libGDX Scene2D extension)
- **Graphics**: Sprite batching, texture atlas, orthographic camera
- **Platforms**: Desktop (LWJGL3), Android, HTML5 (GWT)

## Custom Features
- **Entity/Component System**: Automatic component instantiation and cross-referencing via reflection
- **ActionList**: Sequence management with delays, blocking lanes, and pooling
- **TimeCodec**: Custom serialization system for game state snapshots and time rewind
- **Render Layers**: Multi-layer rendering system with pre-render hooks

## Module Structure
- `core/`: Shared game logic (ninja.trek package)
- `lwjgl3/`: Desktop launcher 
- `android/`: Android launcher
- `html/`: GWT/HTML5 launcher
- `timecode-runtime/`: Custom time serialization runtime
- `timecode-processor/`: Annotation processor for timecode generation
- `assets/`: Game assets (auto-generates assets.txt listing)