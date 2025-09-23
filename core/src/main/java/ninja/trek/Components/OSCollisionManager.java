package ninja.trek.Components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;

/**
 * Spatial hash + narrowphase between AABB and Line shapes for OS physics.
 * Bucket size mirrors old system defaults (2 units).
 */
public class OSCollisionManager {
    private static final int GRID_SIZE = 2;
    private static final int PRIME1 = 641;
    private static final int PRIME2 = 37;

    private static final OSCollisionManager INSTANCE = new OSCollisionManager();
    public static OSCollisionManager get() { return INSTANCE; }

    // Buckets keyed by hash; stores colliders in that cell
    private final Map<Integer, Array<OSColliderC>> buckets = new HashMap<>();

    private final Array<OSColliderC> scratch = new Array<>();
    private final Vector2 minA = new Vector2(), maxA = new Vector2();
    private final Vector2 minB = new Vector2(), maxB = new Vector2();
    private final Vector2 p1 = new Vector2(), p2 = new Vector2();
    private final Vector2 p3 = new Vector2(), p4 = new Vector2();

    private OSCollisionManager() {}

    public void register(OSColliderC c) {
        placeInBucket(c);
    }

    public void unregister(OSColliderC c) {
        removeFromBucket(c);
    }

    public void updateAndCollide(OSColliderC c, float dt) {
        // Update bucket membership
        int key = computeKeyFor(c);
        if (key != c.bucketKey) {
            removeFromBucket(c);
            addToBucket(c, key);
        }
        // Gather neighbors to test pairs
        getNeighboringColliders(c, scratch);
        int selfHash = System.identityHashCode(c);
        for (int i = 0; i < scratch.size; i++) {
            OSColliderC other = scratch.get(i);
            if (other == c) continue;
            // Ensure each pair processed once per frame
            if (System.identityHashCode(other) <= selfHash) continue;
            collidePair(c, other, dt);
        }
        scratch.clear();
    }

    private void placeInBucket(OSColliderC c) {
        int key = computeKeyFor(c);
        addToBucket(c, key);
    }

    private void addToBucket(OSColliderC c, int key) {
        Array<OSColliderC> arr = buckets.get(key);
        if (arr == null) {
            arr = new Array<>(false, 8);
            buckets.put(key, arr);
        }
        arr.add(c);
        c.bucketKey = key;
    }

    private void removeFromBucket(OSColliderC c) {
        if (c.bucketKey == Integer.MIN_VALUE) return;
        Array<OSColliderC> arr = buckets.get(c.bucketKey);
        if (arr != null) {
            arr.removeValue(c, true);
            if (arr.size == 0) buckets.remove(c.bucketKey);
        }
        c.bucketKey = Integer.MIN_VALUE;
    }

    private int computeKeyFor(OSColliderC c) {
        c.getWorldAABB(minA, maxA);
        // Use min corner for bucketing (cheap, approximate)
        int x = fastFloor(minA.x / GRID_SIZE);
        int y = fastFloor(minA.y / GRID_SIZE);
        c.cellX = x; c.cellY = y;
        return hash(x, y);
    }

    private void getNeighboringColliders(OSColliderC c, Array<OSColliderC> out) {
        // 3x3 neighborhood around current cell
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int key = hash(c.cellX + dx, c.cellY + dy);
                Array<OSColliderC> arr = buckets.get(key);
                if (arr != null) out.addAll(arr);
            }
        }
    }

    private void collidePair(OSColliderC a, OSColliderC b, float dt) {
        // Broadphase AABB test
        a.getWorldAABB(minA, maxA);
        b.getWorldAABB(minB, maxB);
        if (!aabbOverlap(minA, maxA, minB, maxB)) return;

        if (a instanceof OSAABBColliderC && b instanceof OSAABBColliderC) {
            collideAabbAabb((OSAABBColliderC) a, (OSAABBColliderC) b);
            return;
        }

        // AABB-Line in either order
        if (a instanceof OSAABBColliderC && b instanceof OSLineColliderC) {
            collideAabbLine((OSAABBColliderC) a, (OSLineColliderC) b);
            return;
        }
        if (a instanceof OSLineColliderC && b instanceof OSAABBColliderC) {
            collideAabbLine((OSAABBColliderC) b, (OSLineColliderC) a);
        }
    }

    private boolean aabbOverlap(Vector2 aMin, Vector2 aMax, Vector2 bMin, Vector2 bMax) {
        return aMin.x <= bMax.x && aMax.x >= bMin.x &&
               aMin.y <= bMax.y && aMax.y >= bMin.y;
    }

    private void collideAabbAabb(OSAABBColliderC a, OSAABBColliderC b) {
        float ax = a.e.x, ay = a.e.y;
        float bx = b.e.x, by = b.e.y;
        float dx = bx - ax;
        float dy = by - ay;
        float overlapX = (a.halfWidth + b.halfWidth) - Math.abs(dx);
        float overlapY = (a.halfHeight + b.halfHeight) - Math.abs(dy);
        if (overlapX <= 0 || overlapY <= 0) return;

        // Choose axis of least penetration
        if (overlapX < overlapY) {
            float dir = Math.signum(dx == 0 ? 1f : dx);
            if (!a.trigger) a.e.x -= dir * overlapX;
            // Adjust velocity along axis if moving into collision
            OSPhysicsC pa = a.e.get(OSPhysicsC.class);
            if (pa != null) {
                if ((dir > 0 && pa.vel.x > 0) || (dir < 0 && pa.vel.x < 0)) pa.vel.x = 0f;
            }
        } else {
            float dir = Math.signum(dy == 0 ? 1f : dy);
            if (!a.trigger) a.e.y -= dir * overlapY;
            OSPhysicsC pa = a.e.get(OSPhysicsC.class);
            if (pa != null) {
                if ((dir > 0 && pa.vel.y > 0) || (dir < 0 && pa.vel.y < 0)) pa.vel.y = 0f;
                // Grounding: if we corrected downward (dir > 0 means b above a), set onGround when moving down
                if (dir > 0 && pa.wasOnGround == false) pa.onGround = true;
            }
        }
    }

    private void collideAabbLine(OSAABBColliderC box, OSLineColliderC line) {
        // Compute world endpoints
        line.getWorldPoints(p1, p2);
        float vx = p2.x - p1.x;
        float vy = p2.y - p1.y;
        float L = (float) Math.hypot(vx, vy);
        if (L == 0f) {
            // Degenerate segment: treat as point-in-rect
            box.getWorldAABB(minA, maxA);
            if (p1.x >= minA.x && p1.x <= maxA.x && p1.y >= minA.y && p1.y <= maxA.y) {
                // Push out vertically by smallest distance
                resolvePointVsAabb(box, p1.x, p1.y);
            }
            return;
        }
        float dirx = vx / L, diry = vy / L;
        // Normal (perp) pointing to one side
        float nx = -diry, ny = dirx;

        // AABB center and half extents
        float cx = box.e.x, cy = box.e.y;
        float hx = box.halfWidth, hy = box.halfHeight;

        // Project center onto line space
        float rx = cx - p1.x, ry = cy - p1.y;
        float t = rx * dirx + ry * diry; // along segment axis
        float d = rx * nx + ry * ny;     // signed distance to infinite line

        // Projection radii of AABB onto axes
        float rPerp = hx * Math.abs(nx) + hy * Math.abs(ny);
        float rPara = hx * Math.abs(dirx) + hy * Math.abs(diry);

        // Check interval overlap: |d| <= rPerp and t in [-rPara, L + rPara]
        if (Math.abs(d) <= rPerp) {
            if (t >= -rPara && t <= L + rPara) {
                // Penetration depth along normal
                float pen = rPerp - Math.abs(d);
                if (pen > 0f && !box.trigger) {
                    float sign = d >= 0f ? 1f : -1f;
                    box.e.x += sign * nx * pen;
                    box.e.y += sign * ny * pen;
                    OSPhysicsC p = box.e.get(OSPhysicsC.class);
                    if (p != null) {
                        float vn = p.vel.x * nx + p.vel.y * ny;
                        // If moving into the surface, kill normal component
                        if ((d >= 0f && vn < 0f) || (d < 0f && vn > 0f)) {
                            p.vel.x -= vn * nx;
                            p.vel.y -= vn * ny;
                        }
                        // Grounding if the line normal points up
                        if (ny > 0.5f) {
                            p.onGround = true;
                        }
                    }
                }
            }
        }
    }

    private void resolvePointVsAabb(OSAABBColliderC box, float px, float py) {
        float dxLeft = (box.e.x - box.halfWidth) - px;
        float dxRight = px - (box.e.x + box.halfWidth);
        float dyDown = (box.e.y - box.halfHeight) - py;
        float dyUp = py - (box.e.y + box.halfHeight);
        // Move out along smallest magnitude (choose vertical preference)
        float ax = Math.min(Math.abs(dxLeft), Math.abs(dxRight));
        float ay = Math.min(Math.abs(dyDown), Math.abs(dyUp));
        if (ay <= ax) {
            if (Math.abs(dyDown) < Math.abs(dyUp)) box.e.y = py + box.halfHeight; else box.e.y = py - box.halfHeight;
        } else {
            if (Math.abs(dxLeft) < Math.abs(dxRight)) box.e.x = px + box.halfWidth; else box.e.x = px - box.halfWidth;
        }
    }

    private int hash(int x, int y) {
        long k = (long) x * PRIME1 + (long) y * PRIME2;
        return (int) k;
    }

    private int fastFloor(float v) {
        int i = (int) v;
        return v < i ? i - 1 : i;
    }
}

