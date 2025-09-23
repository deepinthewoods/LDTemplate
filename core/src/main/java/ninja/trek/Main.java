package ninja.trek;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.ScreenUtils;
import com.kotcrab.vis.ui.VisUI;

import ninja.trek.Entity.Entity;
import ninja.trek.Entity.Player;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public SpriteBatch batch;
    private Texture image;
    private Array<Entity> entities = new Array<Entity>();
    public World world;
    public Box2DDebugRenderer debugR;
    private Vector2 gravity = new Vector2(0, -20);
    private ContactListener contactListener;
    public CutScene cutScene = null;
    private Stage stage;
    public TextureAtlas atlas;
    private Skin skin;
    private boolean release = false;
    public OrthographicCamera camera;
    public ShapeRenderer shapeRenderer;
    private float accumulator, t;
    private float dt = 0.01f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        Gdx.app.log("main", "create");
        stage = new Stage();
        atlas = new TextureAtlas(Gdx.files.internal("pack.atlas"));
        VisUI.load();
        skin = VisUI.getSkin();
        world = new World(gravity, true);
        debugR = new Box2DDebugRenderer();
        contactListener = new GameContactListener() ;
        world.setContactListener(contactListener);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 20, 20);
        shapeRenderer = new ShapeRenderer();

        // Add background first so its preRender runs before others
        add(ninja.trek.Entity.Background.class);

        Player player = add(Player.class);
        player.x = 3;
        player.y = 3;
    }

    @Override
    public void render() {

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        float deltaTime = Gdx.graphics.getDeltaTime();
        if (cutScene != null){
            cutScene.render(batch, deltaTime);
            return;
        }

        accumulator += deltaTime;
        while ( accumulator >= dt )
        {
            world.step(deltaTime, 2, 2);
            for (int i = 0; i < entities.size; i++){
                Entity e = entities.get(i);
                e.update(dt, this);
            }
            for (int i = entities.size-1; i >= 0; i--){
                Entity e = entities.get(i);
                if (e.remove){
                    entities.removeIndex(i);
                    e.onRemove(this);
                    e.remove = false;
                    Pools.free(e);
                }
            }
            accumulator -= dt;
            t += dt;
        }

        // camera updated by CameraC (if attached to an entity)
        // fallback: ensure camera has a sane default transform
        camera.position.set(camera.position.x, camera.position.y,  0f);
        camera.update();

        // Pre-render pass (e.g., parallax background) before SpriteBatch
        for (int i = 0; i < entities.size; i++){
            Entity e = entities.get(i);
            e.preRender(deltaTime, this);
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (int i = 0; i < entities.size; i++){
            Entity e = entities.get(i);
            e.updateRender(deltaTime, this);
        }
        batch.end();
        if (!release) {
            debugR.render(world, camera.combined);
        }
        stage.act(deltaTime);
        stage.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        VisUI.dispose();
        atlas.dispose();
        stage.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    public <T extends Entity> T add(Class<T> cl) {
        T e = Pools.obtain(cl);
        e.init();
        e.onAdded(this);
        entities.add(e);
        return e;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    public void setCutScene(CutScene scene){
        this.cutScene = scene;
        scene.init(stage, atlas, this);
    }
    public void cancelCutScene(){
        this.cutScene = null;
    }
}
