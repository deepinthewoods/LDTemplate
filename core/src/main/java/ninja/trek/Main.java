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
import ninja.trek.time.SnapshotManager;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public SpriteBatch batch; // kept for cutscenes/UI
    private Texture image;
    private Array<Entity> entities = new Array<Entity>();
    @SuppressWarnings("unchecked")
    private Array<Entity>[] renderLayers = (Array<Entity>[]) new Array[10];
    public World world;
    public Box2DDebugRenderer debugR;
    private Vector2 gravity = new Vector2(0, -20);
    private ContactListener contactListener;
    public CutScene cutScene = null;
    private Stage stage;
    public TextureAtlas atlas;
    public ninja.trek.g2d.Assets assets;
    private Skin skin;
    private boolean release = false;
    public OrthographicCamera camera;
    public ShapeRenderer shapeRenderer;
    private float accumulator, t;
    private float dt = 1f/120f;
    private int frameIndex = 0;
    private boolean rewinding = false;
    private SnapshotManager snapshots;
    private int rewindTargetFrame = 0;
    private int nextEntityId = 1;
    private java.util.HashMap<Integer, Class<? extends Entity>> entityClassById = new java.util.HashMap<>();

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        Gdx.app.log("main", "create");
        stage = new Stage();
        assets = new ninja.trek.g2d.Assets("pack.atlas");
        atlas = assets.atlas;
        VisUI.load();
        skin = VisUI.getSkin();
        world = new World(gravity, true);
        debugR = new Box2DDebugRenderer();
        contactListener = new GameContactListener() ;
        world.setContactListener(contactListener);
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 20, 20);
        shapeRenderer = new ShapeRenderer();

        snapshots = new SnapshotManager(this);

        // init render layers
        for (int i = 0; i < renderLayers.length; i++) {
            renderLayers[i] = new Array<Entity>();
        }

        // Batches are built by assets

        // Add background first so its preRender runs before others
        add(ninja.trek.Entity.Background.class);

        Player player = add(Player.class);
        player.x = 3;
        player.y = 3;

        // Add a separate camera entity that follows the player
        ninja.trek.Entity.CameraEntity camEnt = add(ninja.trek.Entity.CameraEntity.class);
        // Schedule follow action
        ninja.trek.actionlist.CameraFollowAction follow = new ninja.trek.actionlist.CameraFollowAction();
        follow.target = player;
        camEnt.get(ninja.trek.Components.ActionListC.class).addToEnd(follow);
    }

    @Override
    public void render() {

        ScreenUtils.clear(0f, 0f, 0f, 1f);
        float deltaTime = Gdx.graphics.getDeltaTime();
        if (cutScene != null){
            cutScene.render(batch, deltaTime);
            return;
        }

        boolean rKey = com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.R);
        if (rKey && !rewinding) {
            rewinding = true;
            rewindTargetFrame = Math.max(0, frameIndex);
        } else if (!rKey && rewinding) {
            rewinding = false;
            // On exit, we already restored+resim each frame; nothing extra
        }

        if (rewinding) {
            // Step target backwards at sim rate based on deltaTime
            int steps = Math.max(1, (int)Math.floor(deltaTime / dt));
            rewindTargetFrame = Math.max(0, rewindTargetFrame - steps);
            snapshots.restoreAndResimTo(rewindTargetFrame);
        } else {
            accumulator += deltaTime;
            while ( accumulator >= dt )
            {
                world.step(dt, 2, 2);
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
                if ((frameIndex % 8) == 0) {
                    snapshots.captureKeyframe(frameIndex);
                }
                frameIndex++;
                accumulator -= dt;
                t += dt;
            }
        }

        // camera updated by CameraC (if attached to an entity)
        // fallback: ensure camera has a sane default transform
        camera.position.set(camera.position.x, camera.position.y,  0f);
        camera.update();

        // Pre-render pass per layer (e.g., parallax/background) before SpriteBatch
        for (int l = 0; l < renderLayers.length; l++) {
            Array<Entity> layer = renderLayers[l];
            for (int i = 0; i < layer.size; i++) {
                Entity e = layer.get(i);
                e.preRender(deltaTime, this);
            }
        }

        // Start all centered batches
        for (int i = 0; i < assets.batches.size; i++) {
            ninja.trek.g2d.CenteredSpriteBatch cb = assets.batches.get(i);
            cb.setProjectionMatrix(camera.combined);
            cb.start();
        }

        // Render pass per layer in order
        for (int l = 0; l < renderLayers.length; l++) {
            Array<Entity> layer = renderLayers[l];
            for (int i = 0; i < layer.size; i++) {
                Entity e = layer.get(i);
                e.updateRender(deltaTime, this);
            }
        }
        // End all centered batches (issue GL calls now)
        for (int i = 0; i < assets.batches.size; i++) {
            assets.batches.get(i).end();
        }
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
        for (int i = 0; i < assets.batches.size; i++) assets.batches.get(i).dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    public <T extends Entity> T add(Class<T> cl) {
        T e = Pools.obtain(cl);
        e.id = nextEntityId++;
        entityClassById.put(e.id, cl);
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

    public Array<Entity> getEntities(){
        return entities;
    }

    public Class<? extends Entity> getEntityClassById(int id) {
        return entityClassById.get(id);
    }

    public <T extends Entity> T createEntityWithId(Class<T> cl, int id) {
        T e = Pools.obtain(cl);
        e.id = id;
        if (!entityClassById.containsKey(id)) entityClassById.put(id, cl);
        e.init();
        e.onAdded(this);
        entities.add(e);
        return e;
    }

    public void registerRenderEntity(Entity e, int layer) {
        if (e == null) return;
        int li = Math.max(0, Math.min(layer, renderLayers.length - 1));
        Array<Entity> arr = renderLayers[li];
        // avoid duplicates
        for (int i = 0; i < arr.size; i++) if (arr.get(i) == e) return;
        arr.add(e);
    }

    public void unregisterRenderEntity(Entity e, int layer) {
        if (e == null) return;
        int li = Math.max(0, Math.min(layer, renderLayers.length - 1));
        renderLayers[li].removeValue(e, true);
    }
}
