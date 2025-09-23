package ninja.trek.Components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;

import ninja.trek.Main;

public class SpriteRenderC extends Component{
    private String name;
    private Sprite sprite;
    public int renderLayer = 5; // default mid layer

    public SpriteRenderC(String name){
        this.name = name;
    }
    @Override
    public void update(float dt, Main main) {
//        Gdx.app.log("spr", "upd");
    }

    @Override
    public void updateRender(float dt, Main main) {
        sprite.setPosition(e.x-5f, e.y-5f);
        sprite.setSize(10f, 10f);

        sprite.draw(main.batch);
//        Gdx.app.log("spr", "draw2");
    }

    @Override
    public void onAdded(Main main) {
        sprite = main.atlas.createSprite(name);
        main.registerRenderEntity(e, renderLayer);
    }

    @Override
    public void onRemove(Main main) {
        main.unregisterRenderEntity(e, renderLayer);
    }
}
