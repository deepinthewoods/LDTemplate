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
    public final TextureAtlas atlas;
    public final ObjectMap<Texture, CenteredSpriteBatch> textureToBatch = new ObjectMap<>();
    public final Array<CenteredSpriteBatch> batches = new Array<>();

    // Name -> Animation (if multi-frame)
    private final Map<String, Animation<CenteredAtlasSprite>> animations = new HashMap<>();
    // Name -> single sprite (for one-off regions)
    private final Map<String, CenteredAtlasSprite> sprites = new HashMap<>();

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

        for (Map.Entry<String, List<Integer>> e : nameToIndices.entrySet()) {
            String name = e.getKey();
            List<Integer> idxs = e.getValue();
            if (idxs.isEmpty()) {
                // Single region with no explicit index
                TextureAtlas.AtlasRegion ar = atlas.findRegion(name);
                if (ar != null) {
                    CenteredAtlasSprite cs = new CenteredAtlasSprite(ar);
                    cs.setSize(ar.originalWidth, ar.originalHeight);
                    cs.setCenter(0, 0);
                    sprites.put(name, cs);
                }
            } else {
                Collections.sort(idxs);
                Array<CenteredAtlasSprite> frames = new Array<>(CenteredAtlasSprite[]::new);
                for (Integer idx : idxs) {
                    TextureAtlas.AtlasRegion ar = atlas.findRegion(name, idx);
                    if (ar == null) continue;
                    CenteredAtlasSprite cs = new CenteredAtlasSprite(ar);
                    cs.setSize(ar.originalWidth, ar.originalHeight);
                    cs.setCenter(0, 0);
                    frames.add(cs);
                }
                if (frames.size == 1) {
                    sprites.put(name, frames.first());
                } else if (frames.size > 1) {
                    animations.put(name, new Animation<>(1f/12f, frames, Animation.PlayMode.LOOP));
                }
            }
        }
    }

    public CenteredAtlasSprite getSprite(String name) {
        return sprites.get(name);
    }

    public Animation<CenteredAtlasSprite> getAnimation(String name) {
        return animations.get(name);
    }
}
