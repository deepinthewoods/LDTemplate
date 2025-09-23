package ninja.trek.actionlist;

import com.badlogic.gdx.math.MathUtils;

import ninja.trek.Entity.Entity;

public class CameraFollowAction extends Action {
    public Entity target; // if null, autodetect first Player

    public float speedFactor = 0.03f;           // proportional move
    public float minMoveSpeed = 18f * 0.0166667f; // ~ 18 * timestep
    public float verticalClampRange = 8f;       // +/- band around target

    private float targetY;

    @Override
    public void update(float dt) {
        if (parent == null || parent.e == null) {
            isFinished = true; return;
        }
        if (target == null) {
            // Try to discover a Player instance from entity list
            // Parent.e does not expose Main; rely on target being set externally
            isFinished = true; return;
        }

        if (first) {
            targetY = target.y;
        }

        // Clamp camera-entity Y around player
        float playerY = target.y;
        parent.e.y = MathUtils.clamp(parent.e.y, playerY - verticalClampRange, playerY + verticalClampRange);

        // Ease targetY toward player when far
        if (Math.abs(targetY - playerY) > 2f) {
            targetY = playerY;
        }

        float dy = parent.e.y - targetY;
        float v = -dy * speedFactor;
        if (Math.abs(v) < minMoveSpeed) {
            float factor = Math.max(0.0001f, Math.abs(v) / minMoveSpeed);
            v /= factor;
        }
        if (Math.abs(dy) > 0.1f) parent.e.y += v;

        // Lock X to player
        parent.e.x = target.x;
    }

    @Override
    public void updateRender(float dt) { }

    @Override
    public void onEnd() { }

    @Override
    public void onStart() { }
}

