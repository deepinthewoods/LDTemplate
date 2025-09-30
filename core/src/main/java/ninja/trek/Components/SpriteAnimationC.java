package ninja.trek.Components;

import com.badlogic.gdx.graphics.Texture;
import ninja.trek.Main;
import ninja.trek.g2d.CenteredSpriteBatch;

public class SpriteAnimationC extends Component {
    // Optional: name for inline-constructed components
    private String name;
    public int handle = -1;
    public int startFrame = 0;
    public int frameOffsetFrames = 0; // optional local offset in frames
    public int renderLayer = 5;

    public SpriteAnimationC() {}
    public SpriteAnimationC(String name) { this.name = name; }

    @Override
    public void update(float dt, Main main) { }

    @Override
    public void updateRender(float dt, Main main) {
        if (handle < 0 && name != null) handle = main.assets.handle(name);
        if (handle < 0) return;

        int simRate = 120; // matches Main's sim rate
        int fps = main.assets.fps(handle);
        float speed = main.assets.speed(handle);
        int frames = main.assets.frameCount(handle);
        if (frames <= 0 || fps <= 0) return;

        int global = main.getFrameIndex();
        int played = (int)Math.floor(((global - startFrame) * fps * speed) / (float)simRate) + frameOffsetFrames;
        if (played < 0) played = 0;

        int frameIdx = frames == 0 ? 0 : Math.floorMod(played, frames); // assume loop

        // Choose batch by texture
        Texture tex = main.assets.frameRegion(handle, frameIdx).getTexture();
        CenteredSpriteBatch b = main.assets.textureToBatch.get(tex);
        if (b == null) return;

        main.assets.drawFrameOriginalCentered(handle, frameIdx, b, e.x, e.y, 0f);
    }

    @Override
    public void onAdded(Main main) {
        if (handle < 0 && name != null) handle = main.assets.handle(name);
        startFrame = main.getFrameIndex();
        main.registerRenderEntity(e, renderLayer);
    }

    @Override
    public void onRemove(Main main) {
        main.unregisterRenderEntity(e, renderLayer);
    }
}
