package ninja.trek.Components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import ninja.trek.Main;

public class ParallaxBackgroundC extends Component {
    // Configurable fields
    public int layers = 6;
    public int renderLayer = 0; // background layer
    public float[] parallaxFactors; // horizontal scroll factors per layer (0=far ... near)
    public float[] baseY;           // base vertical offsets per layer
    public float[] amplitude;       // sine amplitude per layer
    public float[] frequency;       // sine frequency per layer
    public float[] colorRGBA;       // packed RGBA per layer (length = layers*4)

    // Slice width in world units
    public float sliceWidth = 1f;
    // Additional global offsets
    public float timeScroll = 0f;    // small auto scroll
    public float timeScrollSpeed = 0f;

    private float[] bottomCurr;
    private float[] bottomNext;

    @Override
    public void update(float dt, Main main) {
        timeScroll += timeScrollSpeed * dt;
    }

    @Override
    public void updateRender(float dt, Main main) {
        // No-op: rendering is done in preRender to ensure it happens before SpriteBatch
    }

    @Override
    public void preRender(float dt, Main main) {
        if (layers <= 0) return;
        ensureArrays();

        final ShapeRenderer rend = main.shapeRenderer;
        final float zoom = main.camera.zoom;

        // Set projection
        rend.setProjectionMatrix(main.camera.combined);

        // Determine visible range in world units
        float viewW = Gdx.graphics.getWidth() / (float)Gdx.graphics.getPpiX(); // not ideal; rely on camera
        // Use camera viewport instead (in world units)
        float halfW = main.camera.viewportWidth * 0.5f * zoom;
        float camLeft = main.camera.position.x - halfW - 2f;
        float camRight = main.camera.position.x + halfW + 2f;

        // Align to slice grid
        int startX = MathUtils.floor(camLeft / sliceWidth);
        int endX = MathUtils.ceil(camRight / sliceWidth);
        int sliceCount = endX - startX;

        if (bottomCurr.length < sliceCount + 2) {
            bottomCurr = new float[sliceCount + 2];
            bottomNext = new float[sliceCount + 2];
        }

        // Initialize bottoms to a very low value so first layer draws fully
        for (int i = 0; i < sliceCount + 2; i++) {
            bottomCurr[i] = -1e9f;
            bottomNext[i] = -1e9f;
        }

        rend.begin(ShapeRenderer.ShapeType.Filled);

        // Draw far -> near to minimize overdraw, clipping by accumulated bottoms
        for (int layer = 0; layer < layers; layer++) {
            // Color
            int ci = layer * 4;
            Color col = rend.getColor();
            if (colorRGBA != null && colorRGBA.length >= ci + 4) {
                col.set(colorRGBA[ci], colorRGBA[ci + 1], colorRGBA[ci + 2], colorRGBA[ci + 3]);
            } else {
                // sensible defaults
                float t = (float)layer / Math.max(1, layers - 1);
                col.set(0.15f + 0.35f * t, 0.2f + 0.3f * t, 0.25f + 0.35f * t, 1f);
            }
            rend.setColor(col);

            float pf = (parallaxFactors != null && parallaxFactors.length > layer) ? parallaxFactors[layer] : (0.15f + 0.15f * layer);
            float by = (baseY != null && baseY.length > layer) ? baseY[layer] : (2f + layer * 1.8f);
            float amp = (amplitude != null && amplitude.length > layer) ? amplitude[layer] : (2.5f * Math.max(0.1f, 1f - layer * 0.15f));
            float freq = (frequency != null && frequency.length > layer) ? frequency[layer] : (0.12f + 0.03f * layer);

            // Iterate slices
            for (int s = 0; s < sliceCount; s++) {
                float x0 = (startX + s) * sliceWidth;
                float x1 = x0 + sliceWidth;

                // Parallaxed sample positions
                float sx0 = (x0 + timeScroll) * pf;
                float sx1 = (x1 + timeScroll) * pf;

                float yTop0 = heightFunc(sx0, by, amp, freq);
                float yTop1 = heightFunc(sx1, by, amp, freq);

                // Clip to bottoms accumulated so far
                float yBot0 = bottomCurr[s];
                float yBot1 = bottomNext[s];

                float cy0 = Math.max(yTop0, yBot0);
                float cy1 = Math.max(yTop1, yBot1);

                if (cy0 > yBot0 || cy1 > yBot1) {
                    // Draw two triangles as a quad between (x0,yBot0)-(x0,cy0)-(x1,cy1)-(x1,yBot1)
                    rend.triangle(x0, yBot0, x0, cy0, x1, cy1);
                    rend.triangle(x0, yBot0, x1, cy1, x1, yBot1);
                }

                // Update bottoms for next (nearer) layers
                bottomCurr[s] = Math.max(bottomCurr[s], cy0);
                bottomNext[s] = Math.max(bottomNext[s], cy1);
            }
        }

        rend.end();
    }

    private float heightFunc(float x, float base, float amp, float freq) {
        // Combine a couple of sines for variation
        float h = base
                + amp * MathUtils.sin(x * freq)
                + (amp * 0.4f) * MathUtils.sin(x * (freq * 0.57f) + 1.7f)
                + (amp * 0.25f) * MathUtils.sin(x * (freq * 1.73f) + 3.1f);
        return h;
    }

    private void ensureArrays() {
        if (parallaxFactors == null || parallaxFactors.length < layers) {
            parallaxFactors = new float[layers];
            for (int i = 0; i < layers; i++) parallaxFactors[i] = 0.15f + 0.15f * i;
        }
        if (baseY == null || baseY.length < layers) {
            baseY = new float[layers];
            for (int i = 0; i < layers; i++) baseY[i] = 1.5f + i * 1.5f;
        }
        if (amplitude == null || amplitude.length < layers) {
            amplitude = new float[layers];
            for (int i = 0; i < layers; i++) amplitude[i] = 2.5f * Math.max(0.1f, 1f - i * 0.15f);
        }
        if (frequency == null || frequency.length < layers) {
            frequency = new float[layers];
            for (int i = 0; i < layers; i++) frequency[i] = 0.12f + 0.03f * i;
        }
        if (colorRGBA == null || colorRGBA.length < layers * 4) {
            colorRGBA = new float[layers * 4];
            for (int i = 0; i < layers; i++) {
                // default cool greys from near-black to light grey
                float t = (float)i / Math.max(1, layers - 1);
                float r = 0.15f + 0.35f * t;
                float g = 0.2f + 0.3f * t;
                float b = 0.25f + 0.35f * t;
                colorRGBA[i*4] = r;
                colorRGBA[i*4+1] = g;
                colorRGBA[i*4+2] = b;
                colorRGBA[i*4+3] = 1f;
            }
        }
        if (bottomCurr == null) bottomCurr = new float[64];
        if (bottomNext == null) bottomNext = new float[64];
    }

    @Override
    public void onAdded(Main main) {
        ensureArrays();
        main.registerRenderEntity(e, renderLayer);
    }

    @Override
    public void onRemove(Main main) {
        main.unregisterRenderEntity(e, renderLayer);
    }
}
