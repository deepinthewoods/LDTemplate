package ninja.trek.Components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;

import ninja.trek.Main;

public class PhysicsC extends Component{

    private final BodyDef bodyD;
    private final FixtureDef fd;
    public Body body;

    public PhysicsC(BodyDef bodyD, FixtureDef fd){
        this.bodyD = bodyD;
        this.fd = fd;
    }
    @Override
    public void update(float dt, Main main) {
        e.x = body.getPosition().x;
        e.y = body.getPosition().y;
    }

    @Override
    public void updateRender(float dt, Main main) {

    }

    @Override
    public void onAdded(Main main) {
        body = main.world.createBody(bodyD);
        Fixture fx = body.createFixture(fd);

        Gdx.app.log("phys", "added");
    }

    @Override
    public void onRemove(Main main) {
        main.world.destroyBody(body);
    }
}
