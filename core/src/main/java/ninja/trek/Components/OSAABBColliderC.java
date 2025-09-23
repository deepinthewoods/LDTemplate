package ninja.trek.Components;

import com.badlogic.gdx.math.Vector2;

/**
 * Axis-aligned box collider, centered on the Entity position.
 */
public class OSAABBColliderC extends OSColliderC {
    public float halfWidth = 0.5f;
    public float halfHeight = 0.5f;

    public OSAABBColliderC() { }
    public OSAABBColliderC(float halfWidth, float halfHeight) {
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
    }

    @Override
    public void getWorldAABB(Vector2 outMin, Vector2 outMax) {
        outMin.set(e.x - halfWidth, e.y - halfHeight);
        outMax.set(e.x + halfWidth, e.y + halfHeight);
    }
}

