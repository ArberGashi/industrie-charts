package com.arbergashi.charts.core;

import com.arbergashi.charts.core.architecture.CompiledCore;
import com.arbergashi.charts.render.ChartRenderer;
import com.arbergashi.charts.render.testing.AllocationFixtures;
import org.junit.jupiter.api.Test;

import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.constant.ClassDesc;
import java.lang.reflect.AccessFlag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Enforces allocation and package boundaries using the JDK class-file API.
 * Desktop allocations are forbidden throughout renderers and the engine. Reusable
 * StringBuilder instances may be created during construction or static initialization,
 * but render methods and their helpers must not create them on each invocation.
 *
 * <p>These structural checks complement runtime allocation tests. They do not claim
 * to prove that arbitrary code, collection calls, or array operations allocate nothing.</p>
 *
 * @since 2.0.0
 */
class ZeroGcArchitectureTest {
    private static final Set<String> DESKTOP_ALLOCATIONS = Set.of(
        "java.awt.BasicStroke", "java.awt.Color", "java.awt.geom.Point2D",
        "java.awt.geom.Point2D$Double", "java.awt.geom.Point2D$Float"
    );

    @Test
    void renderers_and_engine_must_not_allocate_desktop_objects() {
        List<ClassModel> types = CompiledCore.productionClasses().stream()
            .filter(type -> isRenderType(type) || CompiledCore.name(type).startsWith("com.arbergashi.charts.engine."))
            .toList();
        assertFalse(types.isEmpty(), "No production render or engine classes analyzed");
        List<CompiledCore.ConstructorCall> violations = types.stream()
            .flatMap(type -> CompiledCore.constructorCalls(type).stream())
            .filter(call -> DESKTOP_ALLOCATIONS.contains(call.target())).toList();
        assertTrue(violations.isEmpty(), "Desktop allocation in render/engine code: " + violations);
    }

    @Test
    void render_methods_and_helpers_must_reuse_string_builders() {
        List<CompiledCore.ConstructorCall> violations = CompiledCore.productionClasses().stream()
            .filter(ZeroGcArchitectureTest::isRenderType)
            .flatMap(type -> stringBuilderViolations(type).stream()).toList();
        assertTrue(violations.isEmpty(), "Per-call StringBuilder allocation in render code: " + violations);
    }

    @Test
    void chart_renderer_implementations_must_be_in_the_render_package() {
        Map<String, ClassModel> types = productionTypes();
        List<ClassModel> implementations = types.values().stream()
            .filter(type -> !type.flags().has(AccessFlag.INTERFACE))
            .filter(CompiledCore::isTopLevel)
            .filter(type -> implementsChartRenderer(type, types)).toList();
        assertFalse(implementations.isEmpty(), "No production ChartRenderer implementations analyzed");
        List<String> violations = implementations.stream().filter(type -> !isRenderType(type))
            .map(CompiledCore::name).sorted().toList();
        assertTrue(violations.isEmpty(), "ChartRenderer implementations outside the render package: " + violations);
    }

    @Test
    void allocation_scan_distinguishes_initialization_from_render_callbacks() {
        ClassModel setup = CompiledCore.fixture(AllocationFixtures.CachedBuilders.class);
        List<CompiledCore.ConstructorCall> initialization = CompiledCore.constructorCalls(setup).stream()
            .filter(call -> call.target().equals("java.lang.StringBuilder")).toList();
        assertEquals(2, initialization.size(), "Both instance and static initialization must be analyzed");
        assertEquals(List.of(), stringBuilderViolations(setup));

        ClassModel render = CompiledCore.fixture(AllocationFixtures.PerCallBuilders.class);
        Set<String> violatingMethods = stringBuilderViolations(render).stream()
            .map(CompiledCore.ConstructorCall::method).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("render", "drawData", "helper", "constructorReference"), violatingMethods);

        ClassModel desktop = CompiledCore.fixture(AllocationFixtures.DesktopObjects.class);
        Set<String> targets = CompiledCore.constructorCalls(desktop).stream()
            .map(CompiledCore.ConstructorCall::target).filter(DESKTOP_ALLOCATIONS::contains)
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("java.awt.BasicStroke", "java.awt.Color", "java.awt.geom.Point2D$Double"), targets);
    }

    @Test
    void renderer_package_rule_follows_the_interface_hierarchy_instead_of_names() {
        Map<String, ClassModel> types = productionTypes();
        ClassModel coordinator = types.get("com.arbergashi.charts.engine.concurrent.VirtualThreadRenderer");
        assertFalse(implementsChartRenderer(coordinator, types));

        ClassModel misplaced = ClassFile.of().parse(ClassFile.of().build(
            ClassDesc.of("com.arbergashi.charts.engine.MisplacedPlot"), builder ->
                builder.withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_ABSTRACT)
                    .withInterfaceSymbols(ClassDesc.of(ChartRenderer.class.getName()))));
        assertTrue(implementsChartRenderer(misplaced, types));
        assertFalse(isRenderType(misplaced));
    }

    private static List<CompiledCore.ConstructorCall> stringBuilderViolations(ClassModel type) {
        return CompiledCore.constructorCalls(type).stream()
            .filter(call -> call.target().equals("java.lang.StringBuilder"))
            .filter(call -> !call.method().equals("<init>") && !call.method().equals("<clinit>"))
            .toList();
    }

    private static boolean isRenderType(ClassModel type) {
        return CompiledCore.name(type).startsWith("com.arbergashi.charts.render.");
    }

    private static Map<String, ClassModel> productionTypes() {
        Map<String, ClassModel> types = new HashMap<>();
        for (ClassModel type : CompiledCore.productionClasses()) {
            types.put(CompiledCore.name(type), type);
        }
        return types;
    }

    private static boolean implementsChartRenderer(ClassModel type, Map<String, ClassModel> types) {
        if (type == null) {
            throw new IllegalStateException("Missing class while checking the renderer hierarchy");
        }
        if (CompiledCore.name(type).equals(ChartRenderer.class.getName())) {
            return true;
        }
        for (var implemented : type.interfaces()) {
            String name = implemented.asInternalName().replace('/', '.');
            if (name.equals(ChartRenderer.class.getName())
                || types.containsKey(name) && implementsChartRenderer(types.get(name), types)) {
                return true;
            }
        }
        return type.superclass().map(parent -> types.get(parent.asInternalName().replace('/', '.')))
            .map(parent -> implementsChartRenderer(parent, types)).orElse(false);
    }
}
