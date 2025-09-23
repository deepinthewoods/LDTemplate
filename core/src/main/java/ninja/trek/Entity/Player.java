package ninja.trek.Entity;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;

import ninja.trek.Components.CollisionC;
import ninja.trek.Components.PhysicsC;
import ninja.trek.Components.PlayerC;
import ninja.trek.Components.SpriteRenderC;

public class Player extends Entity{
    public PlayerC  player;
    public PhysicsC physics = new PhysicsC(bd, fd);

    public SpriteRenderC render = new SpriteRenderC("Icosphere_aim");


    static BodyDef bd = new BodyDef();
    static FixtureDef fd = new FixtureDef();
    static PolygonShape shape = new PolygonShape();
    static {
        shape.set(new float[]{-.25f, -.5f, -.25f, .5f, .25f, .5f, .25f, -.5f});
        fd.shape = shape;
        bd.type = BodyDef.BodyType.DynamicBody;

    }
}
