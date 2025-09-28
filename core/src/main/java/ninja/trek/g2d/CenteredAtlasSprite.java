package ninja.trek.g2d;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** A center-based sprite wrapper for atlas regions with per-corner colors. */
public class CenteredAtlasSprite {
    private final TextureAtlas.AtlasRegion region;
    private float cx, cy;
    private float width, height;
    private float rotation;

    private final Color c00 = new Color(Color.WHITE);
    private final Color c10 = new Color(Color.WHITE);
    private final Color c11 = new Color(Color.WHITE);
    private final Color c01 = new Color(Color.WHITE);

    public CenteredAtlasSprite(TextureAtlas.AtlasRegion region) {
        this.region = region;
        this.width = region.getRegionWidth();
        this.height = region.getRegionHeight();
    }

    public void setCenter(float x, float y) { this.cx = x; this.cy = y; }
    public void setSize(float w, float h) { this.width = w; this.height = h; }
    public void setRotation(float degrees) { this.rotation = degrees; }

    public void setColor(Color color) {
        c00.set(color); c10.set(color); c11.set(color); c01.set(color);
    }
    public void setCornerColors(Color c00, Color c10, Color c11, Color c01) {
        this.c00.set(c00); this.c10.set(c10); this.c11.set(c11); this.c01.set(c01);
    }

    public void draw(CenteredSpriteBatch batch) {
        // Scale ratios from original size
        float sx = width / region.originalWidth;
        float sy = height / region.originalHeight;

        // Packed size considering rotation flag
        float packedW = region.getRotatedPackedWidth();
        float packedH = region.getRotatedPackedHeight();
        float drawW = packedW * sx;
        float drawH = packedH * sy;

        // Offset of packed rect center relative to original rect center (before rotation), scaled
        float dx = (region.offsetX * sx) + (drawW * 0.5f) - (width * 0.5f);
        float dy = (region.offsetY * sy) + (drawH * 0.5f) - (height * 0.5f);

        float cos = 1f, sin = 0f;
        if (rotation != 0f) {
            float rad = (float)Math.toRadians(rotation);
            cos = (float)Math.cos(rad);
            sin = (float)Math.sin(rad);
        }
        float rcx = cx + (dx * cos - dy * sin);
        float rcy = cy + (dx * sin + dy * cos);

        boolean rotateUV = region.rotate; // packed CCW -> rotate UV CW to correct
        batch.draw(region, rcx, rcy, drawW, drawH, rotation, c00, c10, c11, c01, rotateUV);
    }

    public Texture getTexture() { return region.getTexture(); }
    public TextureRegion getRegion() { return region; }
}
