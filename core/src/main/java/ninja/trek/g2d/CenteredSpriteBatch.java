package ninja.trek.g2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

/**
 * A sprite batch that caches all submitted quads and only issues GL calls in end().
 * It is designed to render a single bound Texture; set via setTexture().
 * Positions are center-based. Exposes per-corner color draw.
 */
public class CenteredSpriteBatch {
    // Vertex layout mirrors libGDX SpriteBatch: 2 pos + 1 packed color + 2 uv = 5 floats per vertex
    public static final int VERTEX_SIZE = 2 + 1 + 2;
    public static final int SPRITE_SIZE = 4 * VERTEX_SIZE; // 20 floats

    private final float[] vertices;
    private int idx = 0; // floats in use

    private final Mesh mesh;
    private Texture texture;

    private final Matrix4 projectionMatrix = new Matrix4();
    private final Matrix4 transformMatrix = new Matrix4();
    private final Matrix4 combinedMatrix = new Matrix4();

    private ShaderProgram shader;
    private boolean drawing = false;

    private boolean blendingDisabled = false;
    private int blendSrcFunc = GL20.GL_SRC_ALPHA;
    private int blendDstFunc = GL20.GL_ONE_MINUS_SRC_ALPHA;
    private int blendSrcFuncAlpha = GL20.GL_SRC_ALPHA;
    private int blendDstFuncAlpha = GL20.GL_ONE_MINUS_SRC_ALPHA;

    public int renderCalls = 0; // per-begin
    public int totalRenderCalls = 0; // lifetime
    public int maxSpritesInBatch = 0;
    public int droppedSprites = 0; // number of quads ignored due to capacity

    public CenteredSpriteBatch() {
        this(1000, createDefaultShader());
    }

    public CenteredSpriteBatch(int size) {
        this(size, createDefaultShader());
    }

    public CenteredSpriteBatch(int size, ShaderProgram shader) {
        if (size < 1) throw new IllegalArgumentException("size must be > 0");
        this.vertices = new float[size * SPRITE_SIZE];
        this.shader = shader;

        // Build mesh with indices for quads
        mesh = new Mesh(false, size * 4, size * 6,
                new VertexAttribute(Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
                new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        short[] indices = new short[size * 6];
        short j = 0;
        for (int i = 0; i < indices.length; i += 6, j += 4) {
            indices[i] = j;
            indices[i + 1] = (short) (j + 1);
            indices[i + 2] = (short) (j + 2);
            indices[i + 3] = (short) (j + 2);
            indices[i + 4] = (short) (j + 3);
            indices[i + 5] = j;
        }
        mesh.setIndices(indices);
    }

    public static ShaderProgram createDefaultShader() {
        String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
                "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
                "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "  v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
                "  v_color.a = v_color.a * (255.0/254.0);\n" +
                "  v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
                "  gl_Position = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
                "}";
        String fragmentShader = "#ifdef GL_ES\n" +
                "precision mediump float;\n" +
                "#endif\n" +
                "varying vec4 v_color;\n" +
                "varying vec2 v_texCoords;\n" +
                "uniform sampler2D u_texture;\n" +
                "void main() {\n" +
                "  gl_FragColor = v_color * texture2D(u_texture, v_texCoords);\n" +
                "}";
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled()) throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

    // Lifecycle
    public void start() { begin(); }

    public void begin() {
        if (drawing) throw new IllegalStateException("end() must be called before begin().");
        if (texture == null) throw new IllegalStateException("Texture not set. Call setTexture() before begin().");
        drawing = true;
        renderCalls = 0;
        droppedSprites = 0;
        Gdx.gl.glDepthMask(false);
        shader.bind();
        setupMatrices();
    }

    public void end() {
        if (!drawing) throw new IllegalStateException("begin() must be called before end().");
        if (idx > 0) flush();
        drawing = false;
        Gdx.gl.glDepthMask(true);
        if (!blendingDisabled) Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // State setters
    public void setTexture(Texture texture) { this.texture = texture; }

    public void setShader(ShaderProgram shader) { this.shader = shader; }

    public ShaderProgram getShader() { return shader; }

    public void setProjectionMatrix(Matrix4 projection) {
        if (drawing) flush();
        this.projectionMatrix.set(projection);
        if (drawing) setupMatrices();
    }

    public void setTransformMatrix(Matrix4 transform) {
        if (drawing) flush();
        this.transformMatrix.set(transform);
        if (drawing) setupMatrices();
    }

    public Matrix4 getProjectionMatrix() { return projectionMatrix; }
    public Matrix4 getTransformMatrix() { return transformMatrix; }

    public void disableBlending() { if (!blendingDisabled) { flush(); blendingDisabled = true; } }
    public void enableBlending() { if (blendingDisabled) { flush(); blendingDisabled = false; } }
    public void setBlendFunction(int src, int dst) { setBlendFunctionSeparate(src, dst, src, dst); }
    public void setBlendFunctionSeparate(int srcColor, int dstColor, int srcAlpha, int dstAlpha) {
        if (blendSrcFunc == srcColor && blendDstFunc == dstColor && blendSrcFuncAlpha == srcAlpha && blendDstFuncAlpha == dstAlpha) return;
        flush();
        blendSrcFunc = srcColor; blendDstFunc = dstColor; blendSrcFuncAlpha = srcAlpha; blendDstFuncAlpha = dstAlpha;
    }

    private void setupMatrices() {
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        shader.setUniformMatrix("u_projTrans", combinedMatrix);
        shader.setUniformi("u_texture", 0);
    }

    private void flush() {
        if (idx == 0) return;
        renderCalls++;
        totalRenderCalls++;
        int spritesInBatch = idx / SPRITE_SIZE;
        if (spritesInBatch > maxSpritesInBatch) maxSpritesInBatch = spritesInBatch;
        int count = spritesInBatch * 6;

        texture.bind();
        mesh.setVertices(vertices, 0, idx);

        if (blendingDisabled) {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
        }

        mesh.render(shader, GL20.GL_TRIANGLES, 0, count);
        idx = 0;
    }

    // Drawing API (center-based)
    public void draw(TextureRegion region, float centerX, float centerY, float width, float height, float rotationDeg, Color color) {
        draw(region, centerX, centerY, width, height, rotationDeg, color, color, color, color);
    }
    public void draw(TextureRegion region, float centerX, float centerY, float width, float height, float rotationDeg) {
        draw(region, centerX, centerY, width, height, rotationDeg, Color.WHITE);
    }

    public void draw(TextureRegion region, float centerX, float centerY, float width, float height, float rotationDeg,
                     Color c00, Color c10, Color c11, Color c01) {
        drawInternal(region, centerX, centerY, width, height, rotationDeg, c00, c10, c11, c01, false);
    }

    public void draw(TextureRegion region, float centerX, float centerY, float width, float height, float rotationDeg,
                     Color c00, Color c10, Color c11, Color c01, boolean rotateUV90CW) {
        drawInternal(region, centerX, centerY, width, height, rotationDeg, c00, c10, c11, c01, rotateUV90CW);
    }

    private void drawInternal(TextureRegion region, float centerX, float centerY, float width, float height, float rotationDeg,
                              Color c00, Color c10, Color c11, Color c01, boolean rotateUV90CW) {
        if (!drawing) throw new IllegalStateException("begin() must be called before draw().");
        // Enforce single texture: ignore if wrong texture
        if (region == null || region.getTexture() != this.texture) return;
        // Capacity check – never flush early
        if (idx + SPRITE_SIZE > vertices.length) { droppedSprites++; return; }

        final float hw = width * 0.5f;
        final float hh = height * 0.5f;

        float cos = 1, sin = 0;
        if (rotationDeg != 0f) {
            float rad = (float)Math.toRadians(rotationDeg);
            cos = (float)Math.cos(rad);
            sin = (float)Math.sin(rad);
        }

        // local corners around origin (center)
        float lx0 = -hw, ly0 = -hh; // bottom-left (0,0)
        float lx1 =  hw, ly1 = -hh; // bottom-right (1,0)
        float lx2 =  hw, ly2 =  hh; // top-right (1,1)
        float lx3 = -hw, ly3 =  hh; // top-left (0,1)

        // rotate and translate
        float x0 = centerX + (lx0 * cos - ly0 * sin);
        float y0 = centerY + (lx0 * sin + ly0 * cos);
        float x1 = centerX + (lx1 * cos - ly1 * sin);
        float y1 = centerY + (lx1 * sin + ly1 * cos);
        float x2 = centerX + (lx2 * cos - ly2 * sin);
        float y2 = centerY + (lx2 * sin + ly2 * cos);
        float x3 = centerX + (lx3 * cos - ly3 * sin);
        float y3 = centerY + (lx3 * sin + ly3 * cos);

        float u = region.getU();
        float v = region.getV();
        float u2 = region.getU2();
        float v2 = region.getV2();

        float c0 = c00.toFloatBits();
        float c1 = c10.toFloatBits();
        float c2 = c11.toFloatBits();
        float c3 = c01.toFloatBits();

        int i = idx;
        float[] verts = vertices;
        // Vertex order: bottom-left, bottom-right, top-right, top-left
        if (!rotateUV90CW) {
            // Default mapping
            verts[i] = x0; verts[i+1] = y0; verts[i+2] = c0; verts[i+3] = u;   verts[i+4] = v2; // BL -> (u, v2)
            verts[i+5] = x1; verts[i+6] = y1; verts[i+7] = c1; verts[i+8] = u2;  verts[i+9] = v2; // BR -> (u2, v2)
            verts[i+10]= x2; verts[i+11]= y2; verts[i+12]= c2; verts[i+13]= u2;  verts[i+14]= v;  // TR -> (u2, v)
            verts[i+15]= x3; verts[i+16]= y3; verts[i+17]= c3; verts[i+18]= u;   verts[i+19]= v;  // TL -> (u, v)
        } else {
            // Rotate UVs 90 degrees clockwise to counter CCW-packed regions
            // BL -> TL, BR -> BL, TR -> BR, TL -> TR
            verts[i] = x0; verts[i+1] = y0; verts[i+2] = c0; verts[i+3] = u;   verts[i+4] = v;   // BL samples TL
            verts[i+5] = x1; verts[i+6] = y1; verts[i+7] = c1; verts[i+8] = u;   verts[i+9] = v2;  // BR samples BL
            verts[i+10]= x2; verts[i+11]= y2; verts[i+12]= c2; verts[i+13]= u2;  verts[i+14]= v2;  // TR samples BR
            verts[i+15]= x3; verts[i+16]= y3; verts[i+17]= c3; verts[i+18]= u2;  verts[i+19]= v;   // TL samples TR
        }
        idx = i + SPRITE_SIZE;
    }

    public void dispose() {
        mesh.dispose();
        if (shader != null) shader.dispose();
    }

    public boolean isDrawing() { return drawing; }
}
