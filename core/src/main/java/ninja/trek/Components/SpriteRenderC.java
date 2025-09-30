package ninja.trek.Components;

import com.badlogic.gdx.graphics.Texture;
import ninja.trek.Main;
import ninja.trek.g2d.CenteredSpriteBatch;

// Static sprite renderer using handle-based lookup; no stored Sprite objects.
public class SpriteRenderC extends Component{
    private String name;
    public int handle = -1; // 1-frame clip handle
    public int renderLayer = 5; // default mid layer

    public SpriteRenderC() {}
    public SpriteRenderC(String name){ this.name = name; }

    @Override
    public void update(float dt, Main main) { }

    @Override
    public void updateRender(float dt, Main main) {
        if (handle < 0 && name != null) handle = main.assets.handle(name);
        if (handle < 0) return;

        // Always draw frame 0, original pixel size, centered at entity position
        Texture tex = main.assets.frameRegion(handle, 0).getTexture();
        CenteredSpriteBatch b = main.assets.textureToBatch.get(tex);
        if (b == null) return;
        main.assets.drawFrameOriginalCentered(handle, 0, b, e.x, e.y, 0f);
    }

    @Override
    public void onAdded(Main main) {
        if (handle < 0 && name != null) handle = main.assets.handle(name);
        main.registerRenderEntity(e, renderLayer);
    }

    @Override
    public void onRemove(Main main) {
        main.unregisterRenderEntity(e, renderLayer);
    }
}
