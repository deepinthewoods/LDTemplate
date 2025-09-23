package ninja.trek.Components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import ninja.trek.Main;

/**
 * Old-school (non-Box2D) physics component.
 * Integrates position using basic kinematics with per-entity acceleration and gravity.
 * Keep alongside Box2D-based PhysicsC; select by choosing this component in an Entity.
 */
public class OSPhysicsC extends Component {

    // Position is taken from owning Entity (e.x / e.y)
    public final Vector2 vel = new Vector2();
    public final Vector2 acc = new Vector2();
    public final Vector2 gravity = new Vector2(0f, -20f);

    // Optional velocity limits: x, +y, -y (mirrors old system's Vector3 usage)
    public final Vector3 limit = new Vector3(Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);

    // State flags similar to old engine
    public boolean onGround;
    public boolean wasOnGround;
    public boolean wasOnGround2;
    public boolean left; // true if moving left beyond a small threshold

    private final Vector2 tmpV = new Vector2();
    private final Vector2 tmpW = new Vector2();

    public OSPhysicsC() { }

    @Override
    public void update(float dt, Main main) {
        // Mirror the two-phase integration from the old Physics2dSystem
        wasOnGround2 = wasOnGround;
        wasOnGround = onGround;
        onGround = false; // will be set by collision resolution if supported this frame

        // Horizontal step
        stepX(dt);

        // Vertical step
        stepY(dt);

        // Direction flag based on x velocity
        if (Math.abs(vel.x) > 0.0001f) {
            left = vel.x < 0f;
        }
    }

    private void stepX(float timestep) {
        // pos += v*dt + (a+g)*0.5*dt^2 (x only)
        tmpV.x = vel.x * timestep;
        tmpW.x = (acc.x + gravity.x) * timestep * timestep * 0.5f;
        tmpV.x += tmpW.x;
        e.x += tmpV.x;

        // v += (a+g)*dt (x only)
        tmpV.x = (acc.x + gravity.x) * timestep;
        vel.x += tmpV.x;

        enforceSpeedLimit(vel, limit);
    }

    private void stepY(float timestep) {
        // pos += v*dt + (a+g)*0.5*dt^2 (y only)
        tmpV.y = vel.y * timestep;
        tmpW.y = (acc.y + gravity.y) * timestep * timestep * 0.5f;
        tmpV.y += tmpW.y;
        e.y += tmpV.y;

        // v += (a+g)*dt (y only)
        tmpV.y = (acc.y + gravity.y) * timestep;
        vel.y += tmpV.y;

        // Accumulator cleared after vertical step (matches old flow)
        acc.set(0f, 0f);

        enforceSpeedLimit(vel, limit);
    }

    @Override
    public void updateRender(float dt, Main main) {
        // No-op; rendering uses Entity position
    }

    @Override
    public void onAdded(Main main) {
        // Align default gravity with Box2D world's gravity scale if available
        if (main != null && main.world != null && main.world.getGravity() != null) {
            gravity.set(main.world.getGravity());
        }
    }

    @Override
    public void onRemove(Main main) {
        // No-op
    }

    public static void enforceSpeedLimit(Vector2 v, Vector3 lim) {
        if (Math.abs(v.x) > lim.x) {
            v.x = v.x < 0f ? -lim.x : lim.x;
        }
        if (v.y > 0f) {
            if (v.y > lim.y) v.y = lim.y;
        } else {
            if (v.y < -lim.z) v.y = -lim.z;
        }
    }
}
