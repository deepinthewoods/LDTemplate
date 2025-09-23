package ninja.trek.Components;

import com.badlogic.gdx.math.Vector2;

import ninja.trek.Main;

/**
 * Base collider for old-school physics. Subclasses must provide a world AABB
 * for broadphase and shape data for narrowphase.
 */
public abstract class OSColliderC extends Component {
    // Broadphase bucket bookkeeping
    int cellX, cellY;
    int bucketKey = Integer.MIN_VALUE;

    // Optional: treat as trigger (no position/velocity resolution)
    public boolean trigger = false;

    // Scratch vectors for AABB computation
    protected final Vector2 tmpMin = new Vector2();
    protected final Vector2 tmpMax = new Vector2();

    @Override
    public void update(float dt, Main main) {
        // Update bucket location and process collisions with neighbors
        OSCollisionManager.get().updateAndCollide(this, dt);
    }

    @Override
    public void updateRender(float dt, Main main) { }

    @Override
    public void onAdded(Main main) {
        OSCollisionManager.get().register(this);
    }

    @Override
    public void onRemove(Main main) {
        OSCollisionManager.get().unregister(this);
    }

    // Implemented by subclasses
    public abstract void getWorldAABB(Vector2 outMin, Vector2 outMax);
}

