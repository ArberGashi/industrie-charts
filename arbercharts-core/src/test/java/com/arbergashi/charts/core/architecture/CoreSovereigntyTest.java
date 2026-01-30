package com.arbergashi.charts.core.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreSovereigntyTest {

    private static final Set<String> PUBLIC_RENDER_ALLOWLIST = Set.of(
        "BaseRenderer",
        "ChartRenderer",
        "GridLayer",
        "LayerBand",
        "LegendChartContext",
        "SpatialChunkRenderer",
        "TooltipContentProvider"
    );

    @Test
    void core_must_be_awt_free() {
        JavaClasses imported = importMainClasses();

        ArchRule rule = noClasses()
            .that().resideInAPackage("com.arbergashi.charts.core..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "java.awt..",
                "javax.swing..",
                "org.jetbrains.skia..",
                "androidx.compose.."
            )
            .because("The core must be 100% headless and platform-independent.");

        rule.allowEmptyShould(true).check(imported);
    }

    @Test
    void render_internal_types_should_not_be_public() {
        JavaClasses imported = importMainClasses();

        ArchCondition<JavaClass> beNonPublicUnlessRendererOrWhitelisted = new ArchCondition<>("be non-public unless whitelisted") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String simpleName = item.getSimpleName();
                boolean allowed = simpleName.endsWith("Renderer")
                    || simpleName.endsWith("Layer")
                    || PUBLIC_RENDER_ALLOWLIST.contains(simpleName);

                boolean isPublic = item.getModifiers().contains(JavaModifier.PUBLIC);
                if (isPublic && !allowed) {
                    String message = String.format(
                        "Type %s is public in render package but is not whitelisted.",
                        item.getName()
                    );
                    events.add(SimpleConditionEvent.violated(item, message));
                }
            }
        };

        ArchRule rule = classes()
            .that().resideInAPackage("com.arbergashi.charts.render..")
            .and().areTopLevelClasses()
            .should(beNonPublicUnlessRendererOrWhitelisted);

        rule.allowEmptyShould(true).check(imported);
    }

    @Test
    void core_sources_must_not_import_awt() {
        List<Path> javaFiles = sourceFiles(resolvePath("src/main/java"));
        Assertions.assertFalse(javaFiles.isEmpty(), "No core source files found for scan.");
        for (Path path : javaFiles) {
            String text = read(path);
            Assertions.assertFalse(text.contains("import java.awt"),
                "AWT import found in core source: " + path);
            Assertions.assertFalse(text.contains("import javax.swing"),
                "Swing import found in core source: " + path);
        }
    }

    @Test
    void render_public_types_must_be_whitelisted() {
        List<Path> javaFiles = sourceFiles(resolvePath("src/main/java/com/arbergashi/charts/render"));
        Assertions.assertFalse(javaFiles.isEmpty(), "No render source files found for scan.");
        for (Path path : javaFiles) {
            String text = read(path);
            String simpleName = classNameFromPath(path);
            if (simpleName == null) {
                continue;
            }
            boolean isPublic = text.contains("public class " + simpleName)
                || text.contains("public interface " + simpleName)
                || text.contains("public enum " + simpleName);
            if (!isPublic) {
                continue;
            }
            boolean allowed = simpleName.endsWith("Renderer")
                || simpleName.endsWith("Layer")
                || PUBLIC_RENDER_ALLOWLIST.contains(simpleName);
            Assertions.assertTrue(allowed, "Public render type not whitelisted: " + simpleName);
        }
    }

    private static JavaClasses importMainClasses() {
        Path root = resolvePath("target/classes");
        List<Path> classFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(classFiles::add);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to scan compiled classes in " + root, ex);
        }
        Assertions.assertFalse(classFiles.isEmpty(), "No class files found under " + root);
        return new ClassFileImporter().importPaths(classFiles);
    }

    private static Path resolvePath(String relative) {
        Path base = Path.of(System.getProperty("user.dir"));
        Path direct = base.resolve(relative);
        if (Files.exists(direct)) {
            return direct;
        }
        Path moduleRoot = base.resolve("arbercharts-core");
        Path modulePath = moduleRoot.resolve(relative);
        if (Files.exists(modulePath)) {
            return modulePath;
        }
        if (base.getFileName() != null && base.getFileName().toString().equals("arbercharts-core")) {
            return base.resolve(relative);
        }
        return direct;
    }

    private static List<Path> sourceFiles(Path root) {
        List<Path> files = new ArrayList<>();
        if (!Files.exists(root)) {
            return files;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            stream
                .filter(path -> path.toString().endsWith(".java"))
                .forEach(files::add);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to scan sources under " + root, ex);
        }
        return files;
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read " + path, ex);
        }
    }

    private static String classNameFromPath(Path path) {
        String fileName = path.getFileName().toString();
        if (!fileName.endsWith(".java")) {
            return null;
        }
        return fileName.substring(0, fileName.length() - ".java".length());
    }
}
