package ninja.trek.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Manages loading TextureAtlas and building CenteredSprites and Animations. */
public class Assets {
    // Pixels Per Meter for world scaling
    public static final float PPM = 16f;
    public final TextureAtlas atlas;
    public final ObjectMap<Texture, CenteredSpriteBatch> textureToBatch = new ObjectMap<>();
    public final Array<CenteredSpriteBatch> batches = new Array<>();

    // Name -> Animation (if multi-frame)
    private final Map<String, Animation<CenteredAtlasSprite>> animations = new HashMap<>();
    // Name -> single sprite (for one-off regions)
    private final Map<String, CenteredAtlasSprite> sprites = new HashMap<>();

    // Handle-based clip system (stateless rendering)
    public static final class Clip {
        public final String name;
        public final Array<TextureAtlas.AtlasRegion> frames;
        public int fps = 12; // default fps (global per clip)
        public float speed = 1f; // global speed multiplier per clip
        public Clip(String name, Array<TextureAtlas.AtlasRegion> frames) { this.name = name; this.frames = frames; }
    }
    private final Map<String, Integer> nameToHandle = new HashMap<>();
    private final Array<Clip> clipsByHandle = new Array<Clip>();

    public Assets(String atlasPath) {
        this(Gdx.files.internal(atlasPath));
    }

    public Assets(FileHandle packFile) {
        this.atlas = new TextureAtlas(packFile);
        buildBatches();
        indexAtlas(packFile);
    }

    private void buildBatches() {
        for (Texture tex : atlas.getTextures()) {
            CenteredSpriteBatch cb = new CenteredSpriteBatch(1000);
            cb.setTexture(tex);
            batches.add(cb);
            textureToBatch.put(tex, cb);
        }
    }

    private void indexAtlas(FileHandle packFile) {
        Map<String, List<Integer>> nameToIndices = new HashMap<>();
        List<String> lines = packFile.readString().lines().toList();
        boolean expectHeaderOrRegion = true;
        boolean inPage = false;
        String pendingRegion = null;
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw.trim();
            if (line.isEmpty()) { expectHeaderOrRegion = true; inPage = false; pendingRegion = null; continue; }
            if (expectHeaderOrRegion) {
                expectHeaderOrRegion = false;
                // Page header if it looks like a filename with extension
                String lower = line.toLowerCase();
                boolean looksLikePage = lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".ktx") || lower.endsWith(".zktx");
                if (looksLikePage) { inPage = true; continue; }
                // Otherwise it's a region name
                pendingRegion = line;
                nameToIndices.computeIfAbsent(pendingRegion, k -> new ArrayList<>());
                continue;
            }
            if (pendingRegion != null) {
                // Parse region properties; capture index if present
                int colon = line.indexOf(':');
                if (colon == -1) continue;
                String key = line.substring(0, colon).trim();
                String val = line.substring(colon + 1).trim();
                if (key.equals("index")) {
                    try {
                        int idx = Integer.parseInt(val);
                        nameToIndices.get(pendingRegion).add(idx);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // assign handles in stable sorted name order
        List<String> names = new ArrayList<>(nameToIndices.keySet());
        Collections.sort(names);
        for (String name : names) {
            List<Integer> idxs = nameToIndices.get(name);
            if (idxs.isEmpty()) {
                // Single region with no explicit index
                TextureAtlas.AtlasRegion ar = atlas.findRegion(name);
                if (ar != null) {
                    CenteredAtlasSprite cs = new CenteredAtlasSprite(ar);
                    cs.setSize(ar.originalWidth / PPM, ar.originalHeight / PPM);
                    cs.setCenter(0, 0);
                    sprites.put(name, cs);

                    Array<TextureAtlas.AtlasRegion> framesRegions = new Array<TextureAtlas.AtlasRegion>();
                    framesRegions.add(ar);
                    addClip(name, framesRegions);
                }
            } else {
                Collections.sort(idxs);
                Array<CenteredAtlasSprite> frames = new Array<CenteredAtlasSprite>();
                Array<TextureAtlas.AtlasRegion> framesRegions = new Array<TextureAtlas.AtlasRegion>();
                for (Integer idx : idxs) {
                    TextureAtlas.AtlasRegion ar = atlas.findRegion(name, idx);
                    if (ar == null) continue;
                    CenteredAtlasSprite cs = new CenteredAtlasSprite(ar);
                    cs.setSize(ar.originalWidth / PPM, ar.originalHeight / PPM);
                    cs.setCenter(0, 0);
                    frames.add(cs);
                    framesRegions.add(ar);
                }
                if (frames.size == 1) {
                    sprites.put(name, frames.first());
                } else if (frames.size > 1) {
                    animations.put(name, new Animation<>(1f/12f, frames, Animation.PlayMode.LOOP));
                }
                if (framesRegions.size > 0) addClip(name, framesRegions);
            }
        }
    }

    private void addClip(String name, Array<TextureAtlas.AtlasRegion> frames) {
        Clip c = new Clip(name, frames);
        int handle = clipsByHandle.size; // 0-based
        clipsByHandle.add(c);
        nameToHandle.put(name, handle);
    }

    public CenteredAtlasSprite getSprite(String name) {
        return sprites.get(name);
    }

    public Animation<CenteredAtlasSprite> getAnimation(String name) {
        return animations.get(name);
    }

    // Handle-based API
    public int handle(String name) {
        Integer h = nameToHandle.get(name);
        return h == null ? -1 : h;
    }
    public int frameCount(int handle) {
        if (handle < 0 || handle >= clipsByHandle.size) return 0;
        return clipsByHandle.get(handle).frames.size;
    }
    public int fps(int handle) {
        if (handle < 0 || handle >= clipsByHandle.size) return 12;
        return clipsByHandle.get(handle).fps;
    }
    public float speed(int handle) {
        if (handle < 0 || handle >= clipsByHandle.size) return 1f;
        return clipsByHandle.get(handle).speed;
    }
    public TextureAtlas.AtlasRegion frameRegion(int handle, int frameIndex) {
        if (handle < 0 || handle >= clipsByHandle.size) return null;
        Array<TextureAtlas.AtlasRegion> f = clipsByHandle.get(handle).frames;
        if (f.size == 0) return null;
        int idx = Math.max(0, Math.min(frameIndex, f.size - 1));
        return f.get(idx);
    }
    public void drawFrameOriginalCentered(int handle, int frameIndex, CenteredSpriteBatch batch,
                                          float cx, float cy, float rotationDegrees) {
        TextureAtlas.AtlasRegion region = frameRegion(handle, frameIndex);
        if (region == null) return;
        // Scale to world units: meters via PPM
        float width = region.originalWidth / PPM;
        float height = region.originalHeight / PPM;

        // Scale ratios relative to original pixel size
        float sx = 1f / PPM, sy = 1f / PPM;
        float packedW = region.getRotatedPackedWidth();
        float packedH = region.getRotatedPackedHeight();
        float drawW = packedW * sx;
        float drawH = packedH * sy;

        float dx = (region.offsetX * sx) + (drawW * 0.5f) - (width * 0.5f);
        float dy = (region.offsetY * sy) + (drawH * 0.5f) - (height * 0.5f);

        float cos = 1f, sin = 0f;
        if (rotationDegrees != 0f) {
            float rad = (float)Math.toRadians(rotationDegrees);
            cos = (float)Math.cos(rad);
            sin = (float)Math.sin(rad);
        }
        float rcx = cx + (dx * cos - dy * sin);
        float rcy = cy + (dx * sin + dy * cos);

        boolean rotateUV = region.rotate;
        batch.draw(region, rcx, rcy, drawW, drawH, rotationDegrees,
                com.badlogic.gdx.graphics.Color.WHITE,
                com.badlogic.gdx.graphics.Color.WHITE,
                com.badlogic.gdx.graphics.Color.WHITE,
                com.badlogic.gdx.graphics.Color.WHITE,
                rotateUV);
    }

    // Named configuration (per your preference: by name)
    public void setClipFps(String name, int fps) {
        Integer h = nameToHandle.get(name);
        if (h == null) return;
        if (fps <= 0) fps = 12;
        clipsByHandle.get(h).fps = fps;
    }
    public void setClipSpeed(String name, float speed) {
        Integer h = nameToHandle.get(name);
        if (h == null) return;
        if (speed <= 0) speed = 1f;
        clipsByHandle.get(h).speed = speed;
    }
    public int fps(String name) {
        Integer h = nameToHandle.get(name);
        return (h == null) ? 12 : clipsByHandle.get(h).fps;
    }
    public float speed(String name) {
        Integer h = nameToHandle.get(name);
        return (h == null) ? 1f : clipsByHandle.get(h).speed;
    }
}
