package ninja.trek.Entity;

import ninja.trek.Components.OSAABBColliderC;
import ninja.trek.Components.OSPhysicsC;
import ninja.trek.Components.PlayerC;
import ninja.trek.Components.SpriteRenderC;

public class PlayerOS extends Entity {
    public PlayerC player;
    public OSPhysicsC physics = new OSPhysicsC();
    public OSAABBColliderC collider = new OSAABBColliderC(0.25f, 0.5f);
    public SpriteRenderC render = new SpriteRenderC("Icosphere_aim");
}
