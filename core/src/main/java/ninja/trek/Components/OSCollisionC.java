package ninja.trek.Components;

import ninja.trek.Main;

/**
 * Placeholder collision component for the old-school physics path.
 * This does not wire into Box2D; collision handling would be implemented
 * in user code or a future detector using these hooks.
 */
public class OSCollisionC extends Component {

    @Override
    public void update(float dt, Main main) {
        // No-op by default
    }

    @Override
    public void updateRender(float dt, Main main) {
        // No-op
    }

    @Override
    public void onAdded(Main main) {
        // No-op
    }

    @Override
    public void onRemove(Main main) {
        // No-op
    }

    // Simple hooks for custom collision logic if/when a detector is added
    public void onCollisionEnter(OSCollisionC other) { }
    public void onCollisionStay(OSCollisionC other) { }
    public void onCollisionExit(OSCollisionC other) { }
}

