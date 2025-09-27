# Entity/Component System Architecture

## Core Concepts

### Entity System
- **Base Class**: `ninja.trek.Entity.Entity`
- **Auto-initialization**: `init()` method handles component creation and wiring
- **Lifecycle**: `onAdded()`, `update()`, `updateRender()`, `preRender()`, `onRemove()`
- **Component Access**: `get(ComponentClass.class)` method for runtime component lookup

### Component System
- **Base Class**: `ninja.trek.Components.Component`
- **Auto-instantiation**: Components with no-arg constructors are created automatically
- **Cross-referencing**: Components can declare fields referencing other components on same entity
- **Lifecycle Forwarding**: Entity forwards all lifecycle calls to components

### Wiring Process
1. **Phase 1**: Entity.init() finds all Component fields
2. **Phase 2**: Auto-instantiate null fields with no-arg constructors
3. **Phase 3**: Wire component-to-component references by type matching
4. **Error Handling**: Throws if multiple matches or missing required references

## Key Patterns

### Auto-Created Components
```java
public class Player extends Entity {
    public PlayerC player;  // Auto-created by init()
    public SpriteRenderC sprite;  // Auto-created
}
```

### Manual Components (with constructor args)
```java
public class Player extends Entity {
    public PhysicsC physics = new PhysicsC(bodyDef, fixtureDef);  // Manual
}
```

### Optional Component References
```java
public class RenderComponent extends Component {
    @OptionalComponent
    public PlayerC player;  // Won't throw if missing
}
```

### Component Cross-References
```java
public class AIComponent extends Component {
    public PhysicsC physics;  // Auto-wired to PhysicsC on same entity
    public SpriteRenderC sprite;  // Auto-wired to SpriteRenderC
}
```

## Time Serialization Integration
- Entities use `@TimeSerializable(typeId = X)` for snapshot system
- Fields marked with `@TimeField` are included in time rewind
- Used for R-key rewind functionality in Main.java