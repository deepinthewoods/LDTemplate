package ninja.trek.Components;

import com.badlogic.gdx.math.MathUtils;

import ninja.trek.Main;

public class CameraC extends Component {
    // Basic follow + zoom
    public float zoom = 1f;
    public float minZoom = 0.5f;
    public float maxZoom = 3f;
    public float followLerp = 10f; // higher is snappier
    public float offsetX = 0f;
    public float offsetY = 0f;

    @Override
    public void update(float dt, Main main) {
        // Smooth follow to this entity's position
        float targetX = e.x + offsetX;
        float targetY = e.y + offsetY;
        float lerp = 1f - (float)Math.exp(-followLerp * dt);

        main.camera.position.x += (targetX - main.camera.position.x) * lerp;
        main.camera.position.y += (targetY - main.camera.position.y) * lerp;

        main.camera.zoom = MathUtils.clamp(zoom, minZoom, maxZoom);
    }

    @Override
    public void updateRender(float dt, Main main) {
        // No-op
    }

    @Override
    public void onAdded(Main main) {
        // Initialize camera close to entity to prevent jump
        main.camera.position.set(e.x + offsetX, e.y + offsetY, 0f);
        main.camera.zoom = MathUtils.clamp(zoom, minZoom, maxZoom);
        main.camera.update();
    }

    @Override
    public void onRemove(Main main) {
        // No-op
    }
}

