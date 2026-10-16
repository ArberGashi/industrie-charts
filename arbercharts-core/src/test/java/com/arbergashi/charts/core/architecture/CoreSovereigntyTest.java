package com.arbergashi.charts.core.architecture;

import com.arbergashi.charts.render.testing.DesktopDependencyFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreSovereigntyTest {
    private static final Set<String> DESKTOP_PACKAGES = ModuleFinder.ofSystem()
        .find("java.desktop").orElseThrow().descriptor().packages();

    private static final List<String> PLATFORM_PACKAGES = List.of(
        "com.formdev", "javafx", "org.eclipse.swt", "org.jetbrains.skia", "androidx.compose",
        "org.springframework", "io.micrometer", "javax.servlet", "jakarta.servlet",
        "com.arbergashi.charts.bridge", "com.arbergashi.charts.platform.swing",
        "com.arbergashi.charts.platform.ui", "com.arbergashi.charts.platform.export",
        "com.arbergashi.charts.platform.legend", "com.arbergashi.charts.platform.spatial",
        "com.arbergashi.charts.engine.allocation"
    );

    private static final Set<String> PUBLIC_RENDER_ALLOWLIST = Set.of(
        "com.arbergashi.charts.render.LayerBand",
        "com.arbergashi.charts.render.legend.LegendChartContext",
        "com.arbergashi.charts.render.TooltipContentProvider",
        // Established public entry point for registry initialization.
        "com.arbergashi.charts.render.RendererBootstrap",
        // Shared public contracts consumed by UI bridges and custom renderers.
        "com.arbergashi.charts.render.TooltipContext",
        "com.arbergashi.charts.render.TooltipValueWriter",
        "com.arbergashi.charts.render.grid.GridBatchConfig",
        // Shared coordinate contracts keep renderer and grid geometry identical across families.
        "com.arbergashi.charts.render.grid.PolarGeometry",
        "com.arbergashi.charts.render.grid.TernaryGeometry",
        // Public transforms used when integrating specialized plots and legend layouts.
        "com.arbergashi.charts.render.specialized.SmithChartTransform",
        "com.arbergashi.charts.render.legend.LegendLayoutTransformer"
    );

    @Test
    void entire_core_module_must_be_platform_independent() {
        List<CompiledCore.Dependency> dependencies = CompiledCore.dependencies(CompiledCore.productionLocation());
        List<CompiledCore.Dependency> violations = dependencies.stream()
            .filter(CoreSovereigntyTest::isPlatformDependency).toList();
        assertTrue(violations.isEmpty(), "Core depends on desktop or server integrations: " + violations);
    }

    @Test
    void module_rule_rejects_desktop_dependencies_outside_the_core_package(@TempDir Path directory) throws Exception {
        for (Class<?> fixture : List.of(
            DesktopDependencyFixtures.AwtReference.class,
            DesktopDependencyFixtures.ImageIoReference.class,
            DesktopDependencyFixtures.SoundReference.class,
            DesktopDependencyFixtures.GenericReference.class,
            DesktopDependencyFixtures.AnnotationReference.class
        )) {
            Path file = directory.resolve(fixture.getSimpleName() + ".class");
            try (var input = fixture.getResourceAsStream("/" + fixture.getName().replace('.', '/') + ".class")) {
                Files.copy(java.util.Objects.requireNonNull(input), file);
            }
            List<CompiledCore.Dependency> dependencies = CompiledCore.dependencies(file);
            assertTrue(dependencies.stream().allMatch(dependency -> dependency.origin().equals(fixture.getName())));
            assertTrue(dependencies.stream().anyMatch(CoreSovereigntyTest::isPlatformDependency),
                "Architecture rule did not reject " + fixture.getName());
        }
    }

    @Test
    void module_rule_rejects_framework_and_bridge_packages() {
        for (String packageName : PLATFORM_PACKAGES) {
            assertTrue(isPlatformDependency(new CompiledCore.Dependency(
                "com.arbergashi.charts.model.Model", packageName + ".Integration", "test-fixture")), packageName);
        }
        assertTrue(isPlatformDependency(new CompiledCore.Dependency(
            "com.arbergashi.charts.api.Audit", "com.arbergashi.charts.api.ViewportAuditExtractor", "test-fixture")));
        assertFalse(isPlatformDependency(new CompiledCore.Dependency(
            "com.arbergashi.charts.Charts", "com.arbergashi.charts.model.ChartModel", "classes")));
    }

    @Test
    void architecture_scan_rejects_empty_or_malformed_output(@TempDir Path directory) throws Exception {
        assertThrows(IllegalStateException.class, () -> CompiledCore.classesAt(directory));
        Files.write(directory.resolve("Broken.class"), new byte[] {0, 1, 2, 3});
        assertThrows(IllegalStateException.class, () -> CompiledCore.classesAt(directory));
    }

    @Test
    void render_public_types_must_be_whitelisted() {
        List<ClassModel> renderTypes = CompiledCore.productionClasses().stream()
            .filter(type -> CompiledCore.name(type).startsWith("com.arbergashi.charts.render."))
            .filter(CompiledCore::isPublicTopLevel).toList();
        assertFalse(renderTypes.isEmpty(), "No public production renderer types analyzed");
        List<String> violations = renderTypes.stream().map(CompiledCore::name)
            .filter(name -> !isAllowedPublicRenderType(name)).toList();
        assertTrue(violations.isEmpty(), "Public render types not whitelisted: " + violations);
    }

    @Test
    void public_type_rule_detects_final_classes() {
        String name = "com.arbergashi.charts.render.testing.UnapprovedHelper";
        ClassModel fixture = ClassFile.of().parse(ClassFile.of().build(ClassDesc.of(name), builder ->
            builder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL)));
        assertTrue(CompiledCore.isPublicTopLevel(fixture));
        assertFalse(isAllowedPublicRenderType(CompiledCore.name(fixture)));
    }

    @Test
    void core_pom_must_not_declare_platform_dependencies() throws Exception {
        NodeList dependencies = documentFactory().newDocumentBuilder().parse(resolvePom().toFile())
            .getElementsByTagName("dependency");
        assertTrue(dependencies.getLength() > 0, "No Maven dependencies found for scan");
        assertEquals(List.of(), platformDeclarations(dependencies),
            "Core POM declares desktop or server dependencies, including optional/provided dependencies");
    }

    @Test
    void pom_rule_includes_compile_runtime_provided_and_optional_dependencies() throws Exception {
        for (String options : List.of("", "<scope>compile</scope>", "<scope>runtime</scope>",
            "<scope>provided</scope>", "<optional>true</optional>")) {
            for (String group : List.of("com.formdev", "org.openjfx", "org.springframework", "io.micrometer", "com.arbergashi")) {
                String xml = "<project><dependencies><dependency><groupId>" + group
                    + "</groupId><artifactId>integration</artifactId>" + options
                    + "</dependency></dependencies></project>";
                NodeList dependencies = documentFactory().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml))).getElementsByTagName("dependency");
                assertEquals(List.of(group + ":integration"), platformDeclarations(dependencies), group + options);
            }
        }
    }

    private static boolean isPlatformDependency(CompiledCore.Dependency dependency) {
        String target = dependency.target();
        int separator = target.lastIndexOf('.');
        String packageName = separator < 0 ? "" : target.substring(0, separator);
        return dependency.module().equals("java.desktop") || DESKTOP_PACKAGES.contains(packageName)
            || PLATFORM_PACKAGES.stream().anyMatch(prefix -> packageName.equals(prefix) || packageName.startsWith(prefix + "."))
            || target.equals("com.arbergashi.charts.api.ViewportAuditExtractor");
    }

    private static boolean isAllowedPublicRenderType(String name) {
        return name.endsWith("Renderer") || name.endsWith("Layer") || PUBLIC_RENDER_ALLOWLIST.contains(name);
    }

    private static List<String> platformDeclarations(NodeList dependencies) {
        List<String> violations = new ArrayList<>();
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dependency = (Element) dependencies.item(i);
            if ("test".equals(elementText(dependency, "scope"))) {
                continue;
            }
            String group = elementText(dependency, "groupId");
            String artifact = elementText(dependency, "artifactId");
            boolean platformDependency = group.equals("com.arbergashi")
                || group.startsWith("com.formdev") || group.startsWith("org.openjfx")
                || group.startsWith("org.eclipse.swt") || artifact.startsWith("org.eclipse.swt")
                || group.startsWith("org.jetbrains.skia") || group.startsWith("androidx.compose")
                || group.startsWith("org.springframework") || group.startsWith("io.micrometer")
                || group.startsWith("javax.servlet") || group.startsWith("jakarta.servlet");
            if (platformDependency) {
                violations.add(group + ":" + artifact);
            }
        }
        return violations;
    }

    private static String elementText(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private static DocumentBuilderFactory documentFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static Path resolvePom() {
        Path base = Path.of(System.getProperty("user.dir"));
        Path modulePom = base.resolve("arbercharts-core/pom.xml");
        return Files.exists(modulePom) ? modulePom : base.resolve("pom.xml");
    }
}
