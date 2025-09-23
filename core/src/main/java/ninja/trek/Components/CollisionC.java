package ninja.trek.Components;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;

import ninja.trek.Main;

public class CollisionC extends Component{
    @Override
    public void update(float dt, Main main) {

    }

    @Override
    public void updateRender(float dt, Main main) {

    }

    @Override
    public void onAdded(Main main) {
        PhysicsC physicsC = e.get(PhysicsC.class);
        physicsC.body.setUserData(this);
    }

    @Override
    public void onRemove(Main main) {

    }

    // Collision callbacks (no-op by default; override as needed)
    public void beginContact(CollisionC other, Contact contact) { }
    public void endContact(CollisionC other, Contact contact) { }
    public void preSolve(CollisionC other, Contact contact, Manifold oldManifold) { }
    public void postSolve(CollisionC other, Contact contact, ContactImpulse impulse) { }
}
