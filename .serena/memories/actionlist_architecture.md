# ActionList System Architecture

## Core Concepts

### ActionList Management
- **Container**: `ninja.trek.actionlist.ActionList` manages doubly-linked list of actions
- **Component Wrapper**: `ninja.trek.Components.ActionListC` integrates ActionList into Entity system
- **Execution Model**: Sequential execution with blocking lanes and delay support

### Action Lifecycle
1. **onStart()**: Called once when action begins
2. **update(dt)**: Called every frame while active
3. **onEnd()**: Called when action finishes
4. **updateRender(dt)**: Rendering-specific updates

### Key Features

#### Blocking Lanes
- Actions can block lanes using `lanes` bitmask
- Prevents other actions in same lanes from running
- Enables coordination between parallel actions

#### Delay System
- `delay(seconds)` pauses action and schedules resume
- Uses `LANE_DELAY` and min-heap timer system
- Action continues after delay expires

#### Pooling Integration
- Actions implement `Pool.Poolable` interface
- `reset()` method clears state for reuse
- Compatible with libGDX `Pools.obtain()/Pools.free()`

## Usage Patterns

### Simple Action
```java
public class WaitAction extends Action {
    @Override public void onStart() { delay(1f); }
    @Override public void update(float dt) { 
        if ((lanes & LANE_DELAY) == 0) isFinished = true; 
    }
}
```

### Blocking Action
```java
public class MovementAction extends Action {
    @Override public void onStart() { 
        lanes = LANE_MOVEMENT; 
        isBlocking = true; 
    }
}
```

### ActionList as Component
```java
// In Entity
public ActionListC actions;  // Auto-created

// Adding actions
actions.addToEnd(new WaitAction());
actions.addToEnd(new MovementAction());
```

## Integration Points
- **Main.java**: ActionList timing uses fixed timestep (1/120 sec)
- **Entity System**: ActionListC forwards lifecycle to contained ActionList
- **Pooling**: Actions are pooled for memory efficiency
- **Camera**: CameraFollowAction demonstrates action-based camera control