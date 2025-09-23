package ninja.trek.Components;

import com.badlogic.gdx.math.Vector2;

/**
 * Line segment collider defined in local-space relative to the Entity position.
 * World endpoints are (e.x, e.y) + p1 and + p2.
 */
public class OSLineColliderC extends OSColliderC {
    public final Vector2 p1 = new Vector2(-0.5f, 0f);
    public final Vector2 p2 = new Vector2(0.5f, 0f);

    public OSLineColliderC() { }
    public OSLineColliderC(float x1, float y1, float x2, float y2) {
        this.p1.set(x1, y1);
        this.p2.set(x2, y2);
    }

    public void getWorldPoints(Vector2 outP1, Vector2 outP2) {
        outP1.set(e.x + p1.x, e.y + p1.y);
        outP2.set(e.x + p2.x, e.y + p2.y);
    }

    @Override
    public void getWorldAABB(Vector2 outMin, Vector2 outMax) {
        float x1 = e.x + p1.x, y1 = e.y + p1.y;
        float x2 = e.x + p2.x, y2 = e.y + p2.y;
        outMin.set(Math.min(x1, x2), Math.min(y1, y2));
        outMax.set(Math.max(x1, x2), Math.max(y1, y2));
    }
}

