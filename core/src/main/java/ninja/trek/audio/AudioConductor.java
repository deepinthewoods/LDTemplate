package ninja.trek.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import games.rednblack.miniaudio.MASound;
import games.rednblack.miniaudio.MiniAudio;

/**
 * Central audio engine wrapper for layered loop tracks and quantized SFX.
 * Create once, register tracks and SFX, call update(dt) each frame, dispose at shutdown.
 */
public class AudioConductor {
    public final MiniAudio mini;

    private final ObjectMap<String, SoundTrack> tracks = new ObjectMap<>();
    private final ObjectMap<String, SfxPool> sfxPools = new ObjectMap<>();

    public AudioConductor() {
        mini = new MiniAudio();
    }

    /** Android-only helper. Safe to pass null or call on non-Android. */
    public void setupAndroid(Object assets) {
        try {
            // Call MiniAudio.setupAndroid via reflection to avoid android classes in core API
            java.lang.reflect.Method m = mini.getClass().getMethod("setupAndroid", Class.forName("android.content.res.AssetManager"));
            m.invoke(mini, assets);
        } catch (Throwable ignored) {}
    }

    public SoundTrack createTrack(String name) {
        SoundTrack t = new SoundTrack(name, this);
        tracks.put(name, t);
        return t;
    }

    public SoundTrack getTrack(String name) { return tracks.get(name); }

    public void removeTrack(String name) {
        SoundTrack t = tracks.remove(name);
        if (t != null) t.dispose();
    }

    /** Registers an SFX file and pre-allocates instances. */
    public void registerSfx(String name, String internalPath, int poolSize) {
        SfxPool pool = sfxPools.get(name);
        if (pool == null) {
            pool = new SfxPool(internalPath, poolSize);
            sfxPools.put(name, pool);
        } else {
            pool.ensureSize(poolSize);
        }
    }

    MASound obtainSfx(String name) {
        SfxPool pool = sfxPools.get(name);
        if (pool == null) return null;
        return pool.obtain();
    }

    void freeSfx(String name, MASound snd) {
        SfxPool pool = sfxPools.get(name);
        if (pool == null) { if (snd != null) snd.stop(); return; }
        pool.free(snd);
    }

    public void update(float dt) {
        for (ObjectMap.Entry<String, SoundTrack> e : tracks) e.value.update(dt);
    }

    public void dispose() {
        for (ObjectMap.Entry<String, SoundTrack> e : tracks) e.value.dispose();
        tracks.clear();
        for (ObjectMap.Entry<String, SfxPool> e : sfxPools) e.value.dispose();
        sfxPools.clear();
        mini.dispose();
    }

    private final class SfxPool {
        private final String path;
        private final Pool<MASound> pool;
        private int created = 0;
        private int target;

        SfxPool(String path, int size) {
            this.path = path;
            this.target = Math.max(1, size);
            this.pool = new Pool<MASound>(1, 64) {
                @Override protected MASound newObject() {
                    created++;
                    return mini.createSound(path);
                }
            };
            ensureSize(target);
        }

        void ensureSize(int size) {
            target = Math.max(1, size);
            while (created < target) pool.free(pool.newObject()); // prewarm
        }

        MASound obtain() {
            MASound s = pool.obtain();
            try { s.seekTo(0); } catch (Throwable ignored) {}
            return s;
        }

        void free(MASound s) {
            if (s == null) return;
            try { s.stop(); } catch (Throwable ignored) {}
            pool.free(s);
        }

        void dispose() {
            // drain all
            for (int i = 0; i < pool.getFree(); i++) {
                MASound s = pool.obtain();
                try { s.stop(); } catch (Throwable ignored) {}
                try { s.dispose(); } catch (Throwable ignored) {}
            }
        }
    }
}
