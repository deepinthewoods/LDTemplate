package timecode.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

@AutoService(Processor.class)
public class TimecodeProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Types types;
    private javax.lang.model.type.TypeMirror actionBase;
    private final List<String> allRegistrations = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        TypeElement actionEl = elements.getTypeElement("ninja.trek.actionlist.Action");
        actionBase = actionEl != null ? actionEl.asType() : null;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList(
                "timecode.runtime.TimeSerializable",
                "timecode.runtime.TimeField",
                "timecode.runtime.TimeId"
        ));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> serializables = roundEnv.getElementsAnnotatedWith(
                elements.getTypeElement("timecode.runtime.TimeSerializable"));

        Map<Integer, String> typeIds = new HashMap<>();
        List<ActionType> actions = new ArrayList<>();

        for (Element e : serializables) {
            if (e.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement clz = (TypeElement) e;
            AnnotationMirror ann = getAnnotation(clz, "timecode.runtime.TimeSerializable");
            int typeId = getIntValue(ann, "typeId");
            if (typeIds.containsKey(typeId)) {
                String prev = typeIds.get(typeId);
                error("Duplicate @TimeSerializable typeId " + typeId + " on " + clz.getQualifiedName() + ", previously used by " + prev);
                continue;
            }
            typeIds.put(typeId, clz.getQualifiedName().toString());

            String pkg = elements.getPackageOf(clz).getQualifiedName().toString();
            String simpleName = clz.getSimpleName().toString();
            String codecName = simpleName + "Codec";
            String qualifiedCodec = pkg + "." + codecName;

            try {
                JavaFileObject jfo = filer.createSourceFile(qualifiedCodec, clz);
                try (Writer w = jfo.openWriter()) {
                    w.write(generateCodec(pkg, simpleName, clz.getQualifiedName().toString(), codecName, typeId, clz));
                }
            } catch (IOException ex) {
                error("Failed to write codec for " + clz.getQualifiedName() + ": " + ex.getMessage());
            }

            allRegistrations.add("r.register(new " + qualifiedCodec + "());");

            // Record action classes for factory generation
            if (actionBase != null && types.isAssignable(clz.asType(), actionBase)) {
                actions.add(new ActionType(typeId, clz.getQualifiedName().toString()));
            }
        }

        if (roundEnv.processingOver()) {
            try {
                if (!allRegistrations.isEmpty()) {
                    JavaFileObject jfo = filer.createSourceFile("timecode.generated.GeneratedTimeCodecs");
                    try (Writer w = jfo.openWriter()) {
                        w.write(generateRegistry(allRegistrations));
                    }
                }
                JavaFileObject jfo2 = filer.createSourceFile("timecode.generated.GeneratedActionFactory");
                try (Writer w = jfo2.openWriter()) {
                    w.write(generateActionFactory(actions));
                }
            } catch (IOException ex) {
                error("Failed to write registry: " + ex.getMessage());
            }
        }

        return false;
    }

    private String generateRegistry(List<String> registrations) {
        StringBuilder sb = new StringBuilder();
        sb.append("package timecode.generated;\n");
        sb.append("import timecode.runtime.TimeCodecRegistry;\n");
        sb.append("public final class GeneratedTimeCodecs {\n");
        sb.append("  public static void registerAll(TimeCodecRegistry r) {\n");
        for (String line : registrations) sb.append("    ").append(line).append("\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String generateCodec(String pkg, String simpleName, String qualifiedName, String codecName, int typeId, TypeElement clz) {
        // Collect fields annotated with @TimeField and also auto-include public primitive fields
        Map<String, VariableElement> byName = new HashMap<>();
        for (Element m : clz.getEnclosedElements()) {
            if (m.getKind() != ElementKind.FIELD) continue;
            VariableElement ve = (VariableElement) m;
            String fname = ve.getSimpleName().toString();
            AnnotationMirror tf = getAnnotation(ve, "timecode.runtime.TimeField");
            if (tf != null) {
                byName.put(fname, ve);
                continue;
            }
            // Auto-include if: primitive type and PUBLIC, not static/final/transient, and not @TimeIgnore
            Set<Modifier> mods = ve.getModifiers();
            if (!mods.contains(Modifier.PUBLIC)) continue;
            if (mods.contains(Modifier.STATIC) || mods.contains(Modifier.FINAL) || mods.contains(Modifier.TRANSIENT)) continue;
            if (getAnnotation(ve, "timecode.runtime.TimeIgnore") != null) continue;
            String kind = ve.asType().toString();
            if (isSupportedPrimitive(kind)) {
                byName.putIfAbsent(fname, ve);
            }
        }
        List<VariableElement> fields = new ArrayList<>(byName.values());
        fields.sort(Comparator.comparing(v -> v.getSimpleName().toString()));

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n");
        sb.append("public final class ").append(codecName).append(" implements timecode.runtime.TimeCodec<").append(qualifiedName).append("> {\n");
        sb.append("  public short typeId() { return (short)").append(typeId).append("; }\n");
        sb.append("  public Class<").append(qualifiedName).append("> type() { return ").append(qualifiedName).append(".class; }\n");
        sb.append("  public void write(").append(qualifiedName).append(" obj, java.nio.ByteBuffer out) {\n");
        for (VariableElement f : fields) {
            TypeMirror t = f.asType();
            String name = f.getSimpleName().toString();
            String kind = t.toString();
            if (kind.equals("int")) {
                sb.append("    out.putInt(obj.").append(name).append(");\n");
            } else if (kind.equals("short")) {
                sb.append("    out.putShort(obj.").append(name).append(");\n");
            } else if (kind.equals("long")) {
                sb.append("    out.putLong(obj.").append(name).append(");\n");
            } else if (kind.equals("boolean")) {
                sb.append("    out.put((byte)(obj.").append(name).append("?1:0));\n");
            } else if (kind.equals("float")) {
                sb.append("    out.putFloat(obj.").append(name).append(");\n");
            } else if (kind.equals("double")) {
                sb.append("    out.putDouble(obj.").append(name).append(");\n");
            } else {
                // Unsupported in this initial pass
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "Unsupported @TimeField type '" + kind + "' on " + qualifiedName + "." + name);
            }
        }
        sb.append("  }\n");
        sb.append("  public void read(").append(qualifiedName).append(" obj, java.nio.ByteBuffer in) {\n");
        for (VariableElement f : fields) {
            TypeMirror t = f.asType();
            String name = f.getSimpleName().toString();
            String kind = t.toString();
            if (kind.equals("int")) {
                sb.append("    obj.").append(name).append(" = in.getInt();\n");
            } else if (kind.equals("short")) {
                sb.append("    obj.").append(name).append(" = in.getShort();\n");
            } else if (kind.equals("long")) {
                sb.append("    obj.").append(name).append(" = in.getLong();\n");
            } else if (kind.equals("boolean")) {
                sb.append("    obj.").append(name).append(" = in.get()!=0;\n");
            } else if (kind.equals("float")) {
                sb.append("    obj.").append(name).append(" = in.getFloat();\n");
            } else if (kind.equals("double")) {
                sb.append("    obj.").append(name).append(" = in.getDouble();\n");
            } else {
                // error already reported in write
            }
        }
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private boolean isSupportedPrimitive(String kind) {
        return kind.equals("int") || kind.equals("short") || kind.equals("long") ||
               kind.equals("boolean") || kind.equals("float") || kind.equals("double");
    }

    private static final class ActionType {
        final int typeId; final String qname;
        ActionType(int typeId, String qname) { this.typeId = typeId; this.qname = qname; }
    }

    private String generateActionFactory(List<ActionType> actions) {
        StringBuilder sb = new StringBuilder();
        sb.append("package timecode.generated;\n");
        sb.append("import ninja.trek.actionlist.Action;\n");
        sb.append("public final class GeneratedActionFactory {\n");
        sb.append("  public static Action newInstance(short typeId) {\n");
        sb.append("    switch(typeId) {\n");
        for (ActionType a : actions) {
            sb.append("      case ").append(a.typeId).append(": return new ").append(a.qname).append("();\n");
        }
        sb.append("      default: return null;\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private AnnotationMirror getAnnotation(Element e, String annotationClass) {
        for (AnnotationMirror m : e.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(annotationClass)) return m;
        }
        return null;
    }

    private int getIntValue(AnnotationMirror ann, String name) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : ann.getElementValues().entrySet()) {
            if (e.getKey().getSimpleName().toString().equals(name)) {
                return (Integer) e.getValue().getValue();
            }
        }
        return 0;
    }

    private void error(String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg);
    }
}
