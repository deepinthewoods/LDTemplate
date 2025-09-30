package ninja.trek.time;

import com.badlogic.gdx.utils.Array;
import ninja.trek.Main;
import ninja.trek.Entity.Entity;
import ninja.trek.Components.Component;
import ninja.trek.Components.PhysicsC;
import ninja.trek.Components.ActionListC;
import ninja.trek.actionlist.Action;
import ninja.trek.actionlist.CameraFollowAction;
import timecode.runtime.TimeCodec;
import timecode.runtime.TimeCodecRegistry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public final class SnapshotManager {
    private static final int KEYFRAME_STRIDE = 8; // 120 Hz sim / 8 = 15 Hz keyframes
    private static final int MAX_SECONDS = 300; // 5 minutes
    private static final int SIM_RATE = 120; // frames/sec
    private static final int MAX_KEYFRAMES = (MAX_SECONDS * SIM_RATE) / KEYFRAME_STRIDE;

    private static final short TYPEID_ACTIONLISTC = (short)3000; // manual codec id for ActionListC
    private static final short TYPEID_SPRITEANIMC = (short)3001; // manual codec id for SpriteAnimationC

    private final Main main;
    private final TimeCodecRegistry registry = new TimeCodecRegistry();

    private final Deque<Frame> ring = new ArrayDeque<>();

    private static final class Frame {
        int frameIndex;
        long rngState;
        ByteBuffer data;
    }

    public SnapshotManager(Main main) {
        this.main = main;
        // Register generated codecs directly (GWT-friendly)
        timecode.generated.GeneratedTimeCodecs.registerAll(registry);
        // register manual codecs
        registry.register(new ActionListCCodec(registry));
    }

    private static final class GrowingBuffer {
        ByteBuffer buf = ByteBuffer.allocate(64 * 1024).order(ByteOrder.LITTLE_ENDIAN);
        void ensure(int add) {
            if (buf.remaining() >= add) return;
            int need = add - buf.remaining();
            int newCap = buf.capacity();
            while (newCap - buf.position() < add) newCap *= 2;
            ByteBuffer nb = ByteBuffer.allocate(newCap).order(buf.order());
            int pos = buf.position();
            buf.flip();
            nb.put(buf);
            nb.position(pos);
            buf = nb;
        }
        ByteBuffer toByteBufferSlice() {
            ByteBuffer out = buf.duplicate().order(buf.order());
            out.flip();
            return out;
        }
    }

    public void captureKeyframe(int frameIndex) {
        GrowingBuffer gb = new GrowingBuffer();
        ByteBuffer out = gb.buf;

        // Header
        gb.ensure(4 + 8 + 4);
        out.putInt(frameIndex);
        out.putLong(0L); // rngState placeholder

        Array<Entity> entities = main.getEntities();
        gb.ensure(4);
        out.putInt(entities.size);

        for (int i = 0; i < entities.size; i++) {
            Entity e = entities.get(i);

            // Collect components to write
            List<ComponentWriter> comps = new ArrayList<>();
            // 1) Entity basics via generated codec if present
            @SuppressWarnings("unchecked")
            TimeCodec<Entity> ec = registry.forClass((Class<Entity>)(Class<?>)Entity.class);
            if (ec != null) comps.add(new ComponentWriter((short)ec.typeId(), (objOut) -> ec.write(e, objOut)));

            // 2) PhysicsC (sync from body before write)
            PhysicsC pc = e.get(PhysicsC.class);
            if (pc != null) {
                if (pc.body != null) {
                    pc.posX = pc.body.getPosition().x;
                    pc.posY = pc.body.getPosition().y;
                    pc.angle = pc.body.getAngle();
                    pc.velX = pc.body.getLinearVelocity().x;
                    pc.velY = pc.body.getLinearVelocity().y;
                    pc.angVel = pc.body.getAngularVelocity();
                    pc.awake = pc.body.isAwake();
                }
                @SuppressWarnings("unchecked")
                TimeCodec<PhysicsC> cc = registry.forClass(PhysicsC.class);
                if (cc != null) comps.add(new ComponentWriter((short)cc.typeId(), (objOut) -> cc.write(pc, objOut)));
            }

            // 3) ActionListC via manual codec (generic, uses registry for actions)
            ActionListC ac = e.get(ActionListC.class);
            if (ac != null) comps.add(new ComponentWriter(TYPEID_ACTIONLISTC, (objOut) -> new ActionListCCodec(registry).write(ac, objOut)));
            // 4) SpriteAnimationC minimal state for rewind (handle/startFrame/offset/layer)
            ninja.trek.Components.SpriteAnimationC sac = e.get(ninja.trek.Components.SpriteAnimationC.class);
            if (sac != null) comps.add(new ComponentWriter(TYPEID_SPRITEANIMC, (objOut) -> new SpriteAnimationCCodec().write(sac, objOut)));

            // Entity header
            gb.ensure(4 + 2);
            out.putInt(e.id);
            out.putShort((short) comps.size());

            // Components
            for (ComponentWriter cw : comps) {
                gb.ensure(2 + 2);
                out.putShort(cw.typeId);
                int sizePos = out.position();
                out.putShort((short) 0);
                int start = out.position();
                cw.write(out);
                int size = out.position() - start;
                out.putShort(sizePos, (short) size);
            }
        }

        Frame f = new Frame();
        f.frameIndex = frameIndex;
        f.rngState = 0L;
        f.data = gb.toByteBufferSlice();
        ring.addLast(f);
        while (ring.size() > MAX_KEYFRAMES) ring.removeFirst();
    }

    private static final class ComponentWriter {
        final short typeId;
        final WriterFn fn;
        ComponentWriter(short typeId, WriterFn fn) { this.typeId = typeId; this.fn = fn; }
        void write(ByteBuffer out) { fn.write(out); }
        interface WriterFn { void write(ByteBuffer out); }
    }

    public void restoreAndResimTo(int targetFrame) {
        // Find nearest keyframe <= target
        Frame chosen = null;
        for (Frame f : ring) {
            if (f.frameIndex <= targetFrame) chosen = f;
            else break;
        }
        if (chosen == null) return; // nothing captured yet

        restoreSnapshot(chosen.data.duplicate().order(ByteOrder.LITTLE_ENDIAN));

        // Resim from chosen.frameIndex+1 to targetFrame
        int toSim = targetFrame - chosen.frameIndex;
        float dt = 1f/120f;
        for (int i = 0; i < toSim; i++) {
            main.world.step(dt, 2, 2);
            Array<Entity> entities = main.getEntities();
            for (int j = 0; j < entities.size; j++) entities.get(j).update(dt, main);
        }
    }

    private void restoreSnapshot(ByteBuffer in) {
        int frameIndex = in.getInt();
        long rng = in.getLong();
        int entityCount = in.getInt();

        // Build desired id set
        Set<Integer> desired = new HashSet<>();
        int mark = in.position();
        for (int i = 0; i < entityCount; i++) {
            int id = in.getInt(); desired.add(id);
            short comps = in.getShort();
            for (int c = 0; c < comps; c++) {
                in.getShort(); // type
                short size = in.getShort();
                in.position(in.position() + size);
            }
        }
        in.position(mark);

        // Remove entities not desired
        Array<Entity> list = main.getEntities();
        for (int i = list.size - 1; i >= 0; i--) {
            Entity e = list.get(i);
            if (!desired.contains(e.id)) {
                list.removeIndex(i);
                e.onRemove(main);
                e.remove = false;
                com.badlogic.gdx.utils.Pools.free(e);
            }
        }

        // Read and apply
        for (int i = 0; i < entityCount; i++) {
            int id = in.getInt();
            short comps = in.getShort();

            Entity e = findEntityById(id);
            if (e == null) {
                Class<? extends Entity> clz = main.getEntityClassById(id);
                if (clz != null) e = main.createEntityWithId(clz, id);
            }
            if (e == null) continue; // cannot materialize
            // ensure components exist
            e.init();

            for (int c = 0; c < comps; c++) {
                short typeId = in.getShort();
                short size = in.getShort();
                ByteBuffer slice = in.slice(in.position(), size).order(in.order());
                in.position(in.position() + size);
                if (typeId == TYPEID_ACTIONLISTC) {
                    new ActionListCCodec(registry).read(e.get(ActionListC.class), slice);
                    // fix up action references that need entity lookups
                    fixupActions(e.get(ActionListC.class));
                    continue;
                }
                TimeCodec<?> codec = registry.get(typeId);
                if (codec == null && typeId != TYPEID_ACTIONLISTC && typeId != TYPEID_SPRITEANIMC) { in.position(in.position()); continue; }
                Class<?> target = codec.type();
                try {
                    if (typeId == TYPEID_ACTIONLISTC) {
                        ActionListC obj = e.get(ActionListC.class);
                        if (obj != null) new ActionListCCodec(registry).read(obj, slice);
                    } else if (typeId == TYPEID_SPRITEANIMC) {
                        ninja.trek.Components.SpriteAnimationC obj = e.get(ninja.trek.Components.SpriteAnimationC.class);
                        if (obj != null) new SpriteAnimationCCodec().read(obj, slice);
                    } else if (Entity.class.isAssignableFrom(target)) {
                        @SuppressWarnings("unchecked")
                        TimeCodec<Entity> ec = (TimeCodec<Entity>) codec;
                        ec.read(e, slice);
                    } else if (Component.class.isAssignableFrom(target)) {
                        @SuppressWarnings("unchecked")
                        TimeCodec<Component> cc = (TimeCodec<Component>) codec;
                        Component comp = getComponent(e, (Class<? extends Component>) target);
                        if (comp != null) cc.read(comp, slice);
                    }
                } catch (Throwable t) {
                    // ignore malformed component
                }
            }

            // apply physics to Box2D bodies
            PhysicsC pc = e.get(PhysicsC.class);
            if (pc != null && pc.body != null) {
                pc.body.setTransform(pc.posX, pc.posY, pc.angle);
                pc.body.setLinearVelocity(pc.velX, pc.velY);
                pc.body.setAngularVelocity(pc.angVel);
                if (pc.awake) pc.body.setAwake(true); else pc.body.setAwake(false);
            }
        }
    }

    private void fixupActions(ActionListC list) {
        if (list == null || list.actions == null || list.actions.actions == null || list.actions.actions.isEmpty()) return;
        Action a = list.actions.actions.getFirst();
        while (a.getNext() != null) {
            if (a instanceof CameraFollowAction) {
                CameraFollowAction cfa = (CameraFollowAction) a;
                if (cfa.target == null && cfa.targetId != 0) {
                    Entity target = findEntityById(cfa.targetId);
                    cfa.target = target;
                }
            }
            a = a.getNext();
        }
    }

    private Entity findEntityById(int id) {
        Array<Entity> list = main.getEntities();
        for (int i = 0; i < list.size; i++) if (list.get(i).id == id) return list.get(i);
        return null;
    }

    private Component getComponent(Entity e, Class<? extends Component> clz) {
        return e.get(clz);
    }

    // Manual codec for ActionListC with support for CameraFollowAction
    private static final class ActionListCCodec implements timecode.runtime.TimeCodec<ActionListC> {
        private final TimeCodecRegistry registry;
        ActionListCCodec(TimeCodecRegistry registry) { this.registry = registry; }
        @Override public short typeId() { return TYPEID_ACTIONLISTC; }
        @Override public Class<ActionListC> type() { return ActionListC.class; }
        @Override public void write(ActionListC obj, ByteBuffer out) {
            out.putFloat(obj.actions.currentTime);
            int count = obj.actions.size();
            out.putInt(count);
            if (obj.actions.actions.isEmpty()) return;
            Action a = obj.actions.actions.getFirst();
            while (a.getNext() != null) {
                TimeCodec<Action> codec = findActionCodec(a);
                if (codec != null) {
                    out.putShort(codec.typeId());
                    int sizePos = out.position(); out.putShort((short)0);
                    int start = out.position();
                    codec.write(a, out);
                    int sz = out.position() - start; out.putShort(sizePos, (short)sz);
                } else {
                    out.putShort((short)-1); out.putShort((short)0);
                }
                a = a.getNext();
            }
        }
        @Override public void read(ActionListC obj, ByteBuffer in) {
            obj.actions.clearWithDelayed();
            if (obj.actions.delayedActions != null) obj.actions.delayedActions.clear();
            obj.actions.currentTime = in.getFloat();
            int count = in.getInt();
            for (int i = 0; i < count; i++) {
                short type = in.getShort();
                short size = in.getShort();
                if (type == -1) { in.position(in.position() + size); continue; }
                TimeCodec<?> base = registry.get(type);
                if (base == null) { in.position(in.position() + size); continue; }
                @SuppressWarnings("unchecked")
                TimeCodec<Action> codec = (TimeCodec<Action>) base;
                Action instance = timecode.generated.GeneratedActionFactory.newInstance(type);
                if (instance != null) {
                    ByteBuffer slice = in.slice(in.position(), size).order(in.order());
                    codec.read(instance, slice);
                    in.position(in.position() + size);
                    obj.addToEnd(instance);
                } else {
                    in.position(in.position() + size);
                }
            }
        }
        private TimeCodec<Action> findActionCodec(Action a) {
            @SuppressWarnings("unchecked")
            TimeCodec<Action> c = (TimeCodec<Action>) registry.forClass((Class<Action>) a.getClass());
            return c;
        }
        
    }

    // Manual codec for SpriteAnimationC (minimal fields)
    private static final class SpriteAnimationCCodec implements timecode.runtime.TimeCodec<ninja.trek.Components.SpriteAnimationC> {
        @Override public short typeId() { return TYPEID_SPRITEANIMC; }
        @Override public Class<ninja.trek.Components.SpriteAnimationC> type() { return ninja.trek.Components.SpriteAnimationC.class; }
        @Override public void write(ninja.trek.Components.SpriteAnimationC obj, java.nio.ByteBuffer out) {
            out.putInt(obj.handle);
            out.putInt(obj.startFrame);
            out.putInt(obj.frameOffsetFrames);
            out.putInt(obj.renderLayer);
        }
        @Override public void read(ninja.trek.Components.SpriteAnimationC obj, java.nio.ByteBuffer in) {
            obj.handle = in.getInt();
            obj.startFrame = in.getInt();
            obj.frameOffsetFrames = in.getInt();
            obj.renderLayer = in.getInt();
        }
    }
}
