package ninja.trek.Entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import ninja.trek.Components.Component;
import ninja.trek.Main;

public class Entity {
    public float x, y;
    public Array<Component> components = new Array<Component>();
    private boolean hasInit = false;
    public boolean remove = false;
    public Entity(){

    }
    public void init(){
        if (hasInit) return;
        hasInit = true;

        // Phase 1: Ensure all Component fields exist and collect them
        Field[] fields = ClassReflection.getFields(this.getClass());
        for (Field f : fields){
            try {
                // Ignore static fields
                if (f.isStatic()) continue;

                // Only consider fields that are Components
                if (!Component.class.isAssignableFrom(f.getType())) continue;

                Object o = f.get(this);
                if (o == null) {
                    // If the field is null, create a new instance via no-arg constructor
                    Constructor constructor = ClassReflection.getConstructor(f.getType());
                    o = constructor.newInstance();
                    f.set(this, o);
                    Gdx.app.log("entity", "Created component for field: " + f.getName());
                }

                Component c = (Component)o;
                components.add(c);
                c.e = this;
                Gdx.app.log("entity", "add " + c.getClass());
            } catch (ReflectionException e) {
                // Per requirements: throw if constructor missing or reflection fails
                throw new RuntimeException(e);
            }
        }

        // Phase 2: Wire component-to-component references inside each component
        for (int i = 0; i < components.size; i++){
            Component comp = components.get(i);
            try {
                // Combine declared (non-public) and public fields
                Field[] declared = ClassReflection.getDeclaredFields(comp.getClass());
                Field[] publics = ClassReflection.getFields(comp.getClass());

                // Process declared fields
                for (Field cf : declared){
                    if (cf.isStatic()) continue;
                    Class<?> fType = cf.getType();
                    if (!Component.class.isAssignableFrom(fType)) continue;

                    Object current = cf.get(comp);
                    if (current != null) continue;

                    Component match = null;
                    int matchCount = 0;
                    for (int j = 0; j < components.size; j++){
                        Component other = components.get(j);
                        if (other == comp) continue;
                        if (fType.isAssignableFrom(other.getClass())){
                            match = other;
                            matchCount++;
                            if (matchCount > 1) break;
                        }
                    }
                    if (matchCount > 1){
                        throw new RuntimeException("Multiple component matches for field '" + cf.getName() + "' of " + comp.getClass().getName() + " (type " + fType.getName() + ")");
                    }
                    if (matchCount == 0){
                        if (isOptional(cf)) {
                            // Leave null and continue
                            Gdx.app.log("entity", "optional missing: " + comp.getClass().getSimpleName() + "." + cf.getName());
                            continue;
                        }
                        throw new RuntimeException("No component match for field '" + cf.getName() + "' of " + comp.getClass().getName() + " (type " + fType.getName() + ")");
                    }
                    if (matchCount == 1 && match != null){
                        if (!cf.isAccessible()) cf.setAccessible(true);
                        cf.set(comp, match);
                        Gdx.app.log("entity", "wired " + comp.getClass().getSimpleName() + "." + cf.getName() + " -> " + match.getClass().getSimpleName());
                    }
                }

                // Process public (including inherited public) fields
                for (Field cf : publics){
                    if (cf.isStatic()) continue;
                    Class<?> fType = cf.getType();
                    if (!Component.class.isAssignableFrom(fType)) continue;

                    Object current = cf.get(comp);
                    if (current != null) continue;

                    Component match = null;
                    int matchCount = 0;
                    for (int j = 0; j < components.size; j++){
                        Component other = components.get(j);
                        if (other == comp) continue;
                        if (fType.isAssignableFrom(other.getClass())){
                            match = other;
                            matchCount++;
                            if (matchCount > 1) break;
                        }
                    }
                    if (matchCount > 1){
                        throw new RuntimeException("Multiple component matches for field '" + cf.getName() + "' of " + comp.getClass().getName() + " (type " + fType.getName() + ")");
                    }
                    if (matchCount == 0){
                        if (isOptional(cf)) {
                            // Leave null and continue
                            Gdx.app.log("entity", "optional missing: " + comp.getClass().getSimpleName() + "." + cf.getName());
                            continue;
                        }
                        throw new RuntimeException("No component match for field '" + cf.getName() + "' of " + comp.getClass().getName() + " (type " + fType.getName() + ")");
                    }
                    if (matchCount == 1 && match != null){
                        if (!cf.isAccessible()) cf.setAccessible(true);
                        cf.set(comp, match);
                        Gdx.app.log("entity", "wired " + comp.getClass().getSimpleName() + "." + cf.getName() + " -> " + match.getClass().getSimpleName());
                    }
                }
            } catch (ReflectionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isOptional(Field field) {
        try {
            Annotation[] annotations = field.getDeclaredAnnotations();
            if (annotations == null) return false;
            for (Annotation a : annotations) {
                Class<?> at = a.getAnnotationType();
                if (at != null && "ninja.trek.Components.OptionalComponent".equals(at.getName())) {
                    return true;
                }
            }
        } catch (Throwable t) {
            // Be conservative if reflection API differs; treat as not optional
        }
        return false;
    }
    public <CL extends Component> CL get(Class<CL> cl){
        for (int i = 0; i < components.size; i++){
            Component c = components.get(i);
            if (cl.isAssignableFrom(c.getClass())) return (CL)c;
        }
        return null;
    }
    public void update(float dt, Main main){

        for (int i = 0; i < components.size; i++){
            Component c = components.get(i);
            c.update(dt, main);
        }
    }

    public void updateRender(float dt, Main main){

        for (int i = 0; i < components.size; i++){
            Component c = components.get(i);
            c.updateRender(dt, main);
        }
    }

    public void preRender(float dt, Main main){
        for (int i = 0; i < components.size; i++){
            Component c = components.get(i);
            c.preRender(dt, main);
        }
    }

    public void onAdded(Main main){
        for (int i = 0; i < components.size; i++){
            Component c = components.get(i);
            c.onAdded(main);
        }
    }

    public void onRemove(Main main){
        for (int i = 0; i < components.size; i++){
            Component c = components.get(i);
            c.onRemove(main);
        }
    }

}
