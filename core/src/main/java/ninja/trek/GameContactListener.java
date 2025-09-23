package ninja.trek;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Body;

import ninja.trek.Components.CollisionC;

public class GameContactListener implements ContactListener {
    @Override
    public void beginContact(Contact contact) {
        CollisionC a = getCollision(contact.getFixtureA());
        CollisionC b = getCollision(contact.getFixtureB());
        if (a != null) a.beginContact(b, contact);
        if (b != null) b.beginContact(a, contact);
    }

    @Override
    public void endContact(Contact contact) {
        CollisionC a = getCollision(contact.getFixtureA());
        CollisionC b = getCollision(contact.getFixtureB());
        if (a != null) a.endContact(b, contact);
        if (b != null) b.endContact(a, contact);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        CollisionC a = getCollision(contact.getFixtureA());
        CollisionC b = getCollision(contact.getFixtureB());
        if (a != null) a.preSolve(b, contact, oldManifold);
        if (b != null) b.preSolve(a, contact, oldManifold);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        CollisionC a = getCollision(contact.getFixtureA());
        CollisionC b = getCollision(contact.getFixtureB());
        if (a != null) a.postSolve(b, contact, impulse);
        if (b != null) b.postSolve(a, contact, impulse);
    }

    private CollisionC getCollision(Fixture fixture) {
        if (fixture == null) return null;
        Object u = fixture.getUserData();
        if (u instanceof CollisionC) return (CollisionC) u;
        Body body = fixture.getBody();
        Object bu = body != null ? body.getUserData() : null;
        if (bu instanceof CollisionC) return (CollisionC) bu;
        return null;
    }
}
