package ninja.trek.Components;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a component reference field inside a Component as optional.
 * During Entity.init() auto-wiring, if no matching component is found
 * for a field annotated with this, the field is left as null instead
 * of throwing an exception.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OptionalComponent {
}

