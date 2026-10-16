package com.arbergashi.charts.core.architecture;

import com.arbergashi.charts.Charts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassModel;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeDynamicInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LineNumber;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.AccessFlag;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * Reads the actual production output with the JDK class-file API and dependency analyzer.
 * Empty, malformed, unresolved, or partially analyzed input always fails the scan.
 */
public final class CompiledCore {
    private static final Pattern DEPENDENCY = Pattern.compile("^\\s+(\\S+)\\s+->\\s+(\\S+)\\s+(.+?)\\s*$");
    private static final Map<String, ModuleReference> SYSTEM_MODULES_BY_PACKAGE = systemModulesByPackage();

    private CompiledCore() {}

    /** Returns the code source of the core module, including custom Maven output directories. */
    public static Path productionLocation() {
        try {
            return Path.of(Charts.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot locate the compiled core module", ex);
        }
    }

    /** Returns every production class without importing tests or neighboring modules. */
    public static List<ClassModel> productionClasses() {
        return Production.CLASSES;
    }

    /** Parses and verifies every class in a directory, ordinary JAR, or individual class file. */
    public static List<ClassModel> classesAt(Path location) {
        List<ClassModel> classes = new ArrayList<>();
        ClassFile reader = classFileReader();
        try {
            if (Files.isDirectory(location)) {
                try (Stream<Path> files = Files.walk(location)) {
                    for (Path file : files.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                        classes.add(parse(reader, Files.readAllBytes(file), file.toString()));
                    }
                }
            } else if (location.toString().endsWith(".class")) {
                classes.add(parse(reader, Files.readAllBytes(location), location.toString()));
            } else {
                try (ZipFile archive = new ZipFile(location.toFile())) {
                    var entries = archive.stream().filter(entry -> entry.getName().endsWith(".class")).toList();
                    for (var entry : entries) {
                        if (entry.getName().startsWith("META-INF/versions/")) {
                            throw new IllegalStateException("Multi-release core archives require an explicit version scan: " + location);
                        }
                        try (var input = archive.getInputStream(entry)) {
                            classes.add(parse(reader, input.readAllBytes(), location + "!/" + entry.getName()));
                        }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Cannot analyze all class files under " + location, ex);
        }
        if (classes.isEmpty()) {
            throw new IllegalStateException("No class files found under " + location);
        }
        return List.copyOf(classes);
    }

    /** Reads one fixture without adding other test classes to the production scan. */
    public static ClassModel fixture(Class<?> type) {
        try (var input = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
            if (input == null) {
                throw new IllegalStateException("Missing fixture bytecode: " + type.getName());
            }
            return parse(classFileReader(), input.readAllBytes(), type.getName());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot read fixture " + type.getName(), ex);
        }
    }

    /**
     * Runs the JDK dependency analyzer and reconciles its output against the complete class inventory.
     * The test runtime classpath resolves provided dependencies such as the GraalVM annotations.
     */
    public static List<Dependency> dependencies(Path location) {
        Set<String> expected = new HashSet<>();
        for (ClassModel type : classesAt(location)) {
            if (!type.isModuleInfo()) {
                expected.add(name(type));
            }
        }
        if (expected.isEmpty()) {
            throw new IllegalStateException("No ordinary classes found under " + location);
        }
        ToolProvider jdeps = ToolProvider.findFirst("jdeps")
            .orElseThrow(() -> new IllegalStateException("Architecture tests require the JDK jdeps tool"));
        StringWriter output = new StringWriter();
        StringWriter errors = new StringWriter();
        int exitCode = jdeps.run(new PrintWriter(output), new PrintWriter(errors),
            "--class-path", System.getProperty("java.class.path"),
            "--multi-release", "base", "-verbose:class", "-filter:none", location.toString());
        if (exitCode != 0 || !errors.toString().isBlank()) {
            throw new IllegalStateException("jdeps failed for " + location + " (exit " + exitCode + "): "
                + errors + output);
        }
        List<Dependency> dependencies = new ArrayList<>();
        Set<String> analyzed = new HashSet<>();
        for (String line : output.toString().lines().toList()) {
            var matcher = DEPENDENCY.matcher(line);
            if (matcher.matches() && expected.contains(matcher.group(1))) {
                if (matcher.group(3).equals("not found")) {
                    throw new IllegalStateException("Unresolved dependency in architecture scan: " + line.trim());
                }
                analyzed.add(matcher.group(1));
                dependencies.add(new Dependency(matcher.group(1), matcher.group(2), matcher.group(3)));
            } else if (!line.isBlank() && Character.isWhitespace(line.charAt(0))) {
                throw new IllegalStateException("Unrecognized jdeps dependency output: " + line);
            }
        }
        expected.removeAll(analyzed);
        if (!expected.isEmpty()) {
            throw new IllegalStateException("Production classes not analyzed by jdeps: " + expected.stream().sorted().toList());
        }
        return List.copyOf(dependencies);
    }

    /** Returns the binary class name. */
    public static String name(ClassModel type) {
        return type.thisClass().asInternalName().replace('/', '.');
    }

    /** Returns whether the class has no enclosing class or method. */
    public static boolean isTopLevel(ClassModel type) {
        return type.findAttribute(Attributes.enclosingMethod()).isEmpty()
            && type.findAttribute(Attributes.innerClasses()).stream()
                .flatMap(attribute -> attribute.classes().stream())
                .noneMatch(inner -> inner.innerClass().equals(type.thisClass()));
    }

    /** Returns whether the class is a public top-level type. */
    public static boolean isPublicTopLevel(ClassModel type) {
        return type.flags().has(AccessFlag.PUBLIC) && isTopLevel(type);
    }

    /**
     * Lists direct constructor calls and constructor method references with their owning method.
     * Method references are included because they can allocate when a render callback executes.
     */
    public static List<ConstructorCall> constructorCalls(ClassModel type) {
        List<ConstructorCall> calls = new ArrayList<>();
        for (var method : type.methods()) {
            if (method.code().isEmpty()) {
                continue;
            }
            String methodName = method.methodName().stringValue();
            String descriptor = method.methodType().stringValue();
            int line = -1;
            for (var element : method.code().orElseThrow()) {
                if (element instanceof LineNumber number) {
                    line = number.line();
                } else if (element instanceof InvokeInstruction invoke && invoke.name().equalsString("<init>")) {
                    calls.add(new ConstructorCall(name(type), methodName, descriptor,
                        invoke.owner().asInternalName().replace('/', '.'), line));
                } else if (element instanceof InvokeDynamicInstruction dynamic) {
                    for (ConstantDesc argument : dynamic.bootstrapArgs()) {
                        addConstructorHandles(argument, type, methodName, descriptor, line, calls);
                    }
                } else if (element instanceof ConstantInstruction constant) {
                    addConstructorHandles(constant.constantValue(), type, methodName, descriptor, line, calls);
                }
            }
        }
        return List.copyOf(calls);
    }

    private static void addConstructorHandles(ConstantDesc constant, ClassModel type, String method,
                                              String descriptor, int line, List<ConstructorCall> calls) {
        if (constant instanceof DirectMethodHandleDesc handle && handle.methodName().equals("<init>")) {
            String owner = handle.owner().descriptorString();
            calls.add(new ConstructorCall(name(type), method, descriptor,
                owner.substring(1, owner.length() - 1).replace('/', '.'), line));
        } else if (constant instanceof DynamicConstantDesc<?> dynamic) {
            for (ConstantDesc argument : dynamic.bootstrapArgs()) {
                addConstructorHandles(argument, type, method, descriptor, line, calls);
            }
        }
    }

    private static ClassFile classFileReader() {
        // Read metadata without loading classes or resolving optional modules into the test JVM.
        ClassHierarchyResolver resolver = ClassHierarchyResolver.ofResourceParsing(CompiledCore::classResource).cached();
        return ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver));
    }

    private static InputStream classResource(ClassDesc type) {
        String descriptor = type.descriptorString();
        String resource = descriptor.substring(1, descriptor.length() - 1) + ".class";
        ModuleReference module = SYSTEM_MODULES_BY_PACKAGE.get(type.packageName());
        if (module == null) {
            return CompiledCore.class.getClassLoader().getResourceAsStream(resource);
        }
        // ModuleFinder includes installed modules absent from the boot layer, such as the Vector API.
        try (var reader = module.open(); var input = reader.open(resource).orElse(null)) {
            return input == null ? null : new ByteArrayInputStream(input.readAllBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read " + resource + " from " + module.descriptor().name(), ex);
        }
    }

    private static Map<String, ModuleReference> systemModulesByPackage() {
        Map<String, ModuleReference> modules = new HashMap<>();
        for (ModuleReference module : ModuleFinder.ofSystem().findAll()) {
            for (String packageName : module.descriptor().packages()) {
                if (modules.putIfAbsent(packageName, module) != null) {
                    throw new IllegalStateException("Ambiguous system module package: " + packageName);
                }
            }
        }
        return Map.copyOf(modules);
    }

    private static ClassModel parse(ClassFile reader, byte[] bytes, String source) {
        ClassModel type = reader.parse(bytes);
        List<VerifyError> errors = reader.verify(type);
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid bytecode in " + source + ": " + errors);
        }
        return type;
    }

    /** A resolved dependency reported by the JDK analyzer. */
    public record Dependency(String origin, String target, String module) {}

    /** A constructor invocation, including an available source line for actionable failures. */
    public record ConstructorCall(String owner, String method, String descriptor, String target, int line) {
        @Override
        public String toString() {
            return owner + "." + method + descriptor + ":" + line + " -> new " + target;
        }
    }

    private static final class Production {
        private static final List<ClassModel> CLASSES = classesAt(productionLocation());
    }
}
