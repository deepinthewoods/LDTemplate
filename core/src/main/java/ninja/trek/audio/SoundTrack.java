package ninja.trek.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import games.rednblack.miniaudio.MASound;

import java.lang.reflect.Method;

/**
 * A group of looped stems that play in sync, with per-layer fades and a quantized SFX scheduler.
 */
public class SoundTrack {
    public final String name;
    private final AudioConductor engine;

    private final ObjectMap<String, MASound> stems = new ObjectMap<>();
    private String clockLayer = null;
    private float loopLengthSeconds = -1f; // read from clock stem

    private int stepsPerLoop = 16; // grid steps per loop (changeable)
    private int currentStep = -1;
    private boolean playing = false;

    private final Array<Fade> fades = new Array<>();
    private final Array<Scheduled> scheduled = new Array<>();

    static class Fade {
        String layer; float start, target, duration, elapsed;
        Fade set(String layer, float start, float target, float duration) {
            this.layer = layer; this.start = start; this.target = target; this.duration = Math.max(0.0001f, duration); this.elapsed = 0f; return this;
        }
    }
    static class Scheduled {
        String sfxName; int fireStep; boolean armed;
        Scheduled set(String sfx, int step) { sfxName = sfx; fireStep = step; armed = true; return this; }
    }

    SoundTrack(String name, AudioConductor engine) { this.name = name; this.engine = engine; }

    /** Add a stem layer from internal assets. The first added becomes the clock unless setClockLayer is called later. */
    public SoundTrack addStem(String layerName, String internalPath, float initialVolume) {
        MASound s = engine.mini.createSound(internalPath);
        stems.put(layerName, s);
        try { s.loop(); } catch (Throwable ignored) {}
        try { s.setVolume(Math.max(0f, Math.min(1f, initialVolume))); } catch (Throwable ignored) {}
        if (clockLayer == null) clockLayer = layerName;
        refreshLoopLength();
        return this;
    }

    /** Designate which layer defines the loop length and time cursor. */
    public void setClockLayer(String layer) { this.clockLayer = layer; refreshLoopLength(); }

    /** Start playback of all stems in sync from the beginning. */
    public void start() {
        if (stems.size == 0) return;
        // Reset all to 0, then play same frame to ensure sync
        for (ObjectMap.Entry<String, MASound> e : stems) { try { e.value.seekTo(0); } catch (Throwable ignored) {} }
        for (ObjectMap.Entry<String, MASound> e : stems) { try { e.value.play(); } catch (Throwable ignored) {} }
        playing = true; currentStep = -1; // force tick on first update
        refreshLoopLength();
    }

    /** Stop playback (not just mute). */
    public void stop() {
        for (ObjectMap.Entry<String, MASound> e : stems) { try { e.value.stop(); } catch (Throwable ignored) {} }
        playing = false; currentStep = -1;
    }

    /** Set linear fade for a stem to target volume over seconds. */
    public void fadeLayer(String layerName, float targetVolume, float seconds) {
        MASound s = stems.get(layerName);
        if (s == null) return;
        float cur = getVolumeSafe(s);
        Fade f = obtainFade();
        f.set(layerName, cur, Math.max(0f, Math.min(1f, targetVolume)), Math.max(0.0001f, seconds));
        fades.add(f);
    }

    public void setLayerVolume(String layerName, float volume) {
        MASound s = stems.get(layerName);
        if (s == null) return;
        try { s.setVolume(Math.max(0f, Math.min(1f, volume))); } catch (Throwable ignored) {}
    }

    /** Change grid steps (e.g., 8/16/32) at runtime. */
    public void setStepsPerLoop(int steps) { this.stepsPerLoop = Math.max(1, steps); }
    public int getStepsPerLoop() { return stepsPerLoop; }

    /** Quantize an SFX to the next grid boundary (or offset). */
    public void scheduleSfx(String sfxName, int stepsFromNow) {
        int step = nextStepIndex(stepsFromNow);
        Scheduled sc = obtainScheduled();
        sc.set(sfxName, step);
        scheduled.add(sc);
    }

    public void update(float dt) {
        if (!playing) return;
        // Update fades
        for (int i = fades.size - 1; i >= 0; i--) {
            Fade f = fades.get(i);
            MASound s = stems.get(f.layer);
            if (s == null) { fades.removeIndex(i); continue; }
            f.elapsed += dt;
            float t = Math.min(1f, f.elapsed / f.duration);
            float v = f.start + (f.target - f.start) * t;
            try { s.setVolume(v); } catch (Throwable ignored) {}
            if (t >= 1f) fades.removeIndex(i);
        }

        // Scheduler: check step transitions
        float loop = loopLengthSeconds;
        if (loop <= 0f) return;
        float cursor = getCursorSeconds();
        if (cursor < 0f) return;
        float pos = cursor % loop;
        float stepDur = loop / stepsPerLoop;
        int step = (int)Math.floor(pos / stepDur);
        if (step != currentStep) {
            currentStep = step;
            // fire armed events for this step
            for (int i = scheduled.size - 1; i >= 0; i--) {
                Scheduled sc = scheduled.get(i);
                if (!sc.armed) { scheduled.removeIndex(i); continue; }
                if (sc.fireStep == step) {
                    fireSfx(sc.sfxName);
                    scheduled.removeIndex(i);
                }
            }
        }
    }

    public void dispose() {
        for (ObjectMap.Entry<String, MASound> e : stems) {
            try { e.value.stop(); } catch (Throwable ignored) {}
            try { e.value.dispose(); } catch (Throwable ignored) {}
        }
        stems.clear(); fades.clear(); scheduled.clear();
    }

    private void fireSfx(String name) {
        MASound s = engine.obtainSfx(name);
        if (s == null) return;
        try { s.seekTo(0); } catch (Throwable ignored) {}
        try { s.play(); } catch (Throwable ignored) {}
        // schedule return to pool when finished in a naive way: let it play and stop elsewhere if needed
        // Optionally we could track end callbacks if exposed.
    }

    private Fade obtainFade() { return new Fade(); }
    private Scheduled obtainScheduled() { return new Scheduled(); }

    private int nextStepIndex(int stepsFromNow) {
        float loop = loopLengthSeconds;
        if (loop <= 0f) return 0;
        float cursor = getCursorSeconds();
        float pos = cursor % loop;
        float stepDur = loop / stepsPerLoop;
        int step = (int)Math.floor(pos / stepDur);
        return (step + Math.max(1, stepsFromNow)) % stepsPerLoop;
    }

    private void refreshLoopLength() {
        MASound clk = stems.get(clockLayer);
        if (clk == null) return;
        float len = getLengthSeconds(clk);
        if (len > 0f) loopLengthSeconds = len;
    }

    // ---- Reflection helpers to avoid tight coupling to specific API names ----

    private float getVolumeSafe(MASound s) {
        try {
            Method m = s.getClass().getMethod("getVolume");
            Object o = m.invoke(s);
            if (o instanceof Number) return ((Number)o).floatValue();
        } catch (Throwable ignored) {}
        return 1f;
    }

    private float getLengthSeconds(MASound s) {
        // try: getLength(), getLengthSeconds()
        try {
            Method m = s.getClass().getMethod("getLength");
            Object o = m.invoke(s);
            if (o instanceof Number) return ((Number)o).floatValue();
        } catch (Throwable ignored) {}
        try {
            Method m = s.getClass().getMethod("getLengthSeconds");
            Object o = m.invoke(s);
            if (o instanceof Number) return ((Number)o).floatValue();
        } catch (Throwable ignored) {}
        return -1f;
    }

    private float getCursorSeconds() {
        MASound clk = stems.get(clockLayer);
        if (clk == null) return -1f;
        // try: getCursor(), getCursorSeconds()
        try {
            Method m = clk.getClass().getMethod("getCursor");
            Object o = m.invoke(clk);
            if (o instanceof Number) return ((Number)o).floatValue();
        } catch (Throwable ignored) {}
        try {
            Method m = clk.getClass().getMethod("getCursorSeconds");
            Object o = m.invoke(clk);
            if (o instanceof Number) return ((Number)o).floatValue();
        } catch (Throwable ignored) {}
        return -1f;
    }
}

