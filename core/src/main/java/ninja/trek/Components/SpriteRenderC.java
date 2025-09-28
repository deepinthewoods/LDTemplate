package ninja.trek.Components;

import ninja.trek.Main;
import ninja.trek.g2d.CenteredAtlasSprite;
import ninja.trek.g2d.CenteredSpriteBatch;
import com.badlogic.gdx.graphics.g2d.Animation;

public class SpriteRenderC extends Component{
    private String name;
    private CenteredAtlasSprite sprite;
    private Animation<CenteredAtlasSprite> anim;
    private float stateTime = 0f;
    public int renderLayer = 5; // default mid layer

    public SpriteRenderC(String name){
        this.name = name;
    }
    @Override
    public void update(float dt, Main main) {
        stateTime += dt;
    }

    @Override
    public void updateRender(float dt, Main main) {
        CenteredAtlasSprite toDraw;
        if (anim != null) {
            toDraw = anim.getKeyFrame(stateTime, true);
        } else {
            toDraw = sprite;
        }
        if (toDraw == null) return;
        toDraw.setCenter(e.x, e.y);
        // size already set to original in assets; override here if you want a world scale

        CenteredSpriteBatch b = main.assets.textureToBatch.get(toDraw.getTexture());
        if (b != null) toDraw.draw(b);
    }

    @Override
    public void onAdded(Main main) {
        anim = main.assets.getAnimation(name);
        if (anim == null) sprite = main.assets.getSprite(name);
        main.registerRenderEntity(e, renderLayer);
    }

    @Override
    public void onRemove(Main main) {
        main.unregisterRenderEntity(e, renderLayer);
    }
}
