package ninja.trek.Entity;

import ninja.trek.Components.OSCollisionC;
import ninja.trek.Components.OSPhysicsC;
import ninja.trek.Components.PlayerC;
import ninja.trek.Components.SpriteRenderC;

public class PlayerOS extends Entity {
    public PlayerC player;
    public OSPhysicsC physics = new OSPhysicsC();
    public OSCollisionC collision; // optional placeholder
    public SpriteRenderC render = new SpriteRenderC("Icosphere_aim");
}

