package com.arbergashi.charts.core.architecture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static java.lang.constant.ConstantDescs.CD_String;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiledCoreTest {
    @Test
    void verifier_reads_installed_modules_without_resolving_them(@TempDir Path directory) throws Exception {
        Path output = directory.resolve("unresolved-module-probe.log");
        Path java = Path.of(System.getProperty("java.home"), "bin", "java");
        if (!Files.isExecutable(java)) {
            java = java.resolveSibling("java.exe");
        }
        Process process = new ProcessBuilder(java.toString(), "--limit-modules=java.base", "-cp",
            System.getProperty("java.class.path"), UnresolvedModuleProbe.class.getName(),
            CompiledCore.productionLocation().toString())
            .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Bytecode verification probe timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    @Test
    void verifier_still_rejects_incompatible_optional_module_types(@TempDir Path directory) throws Exception {
        Path file = invalidReturnFixture(directory, "InvalidVectorReturn", ClassDesc.of("jdk.incubator.vector.DoubleVector"));
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> CompiledCore.classesAt(file));
        assertTrue(error.getMessage().contains("Invalid bytecode"), error.getMessage());
        assertTrue(error.getMessage().contains("Bad return type"), error.getMessage());
    }

    @Test
    void verifier_still_rejects_unresolved_hierarchies(@TempDir Path directory) throws Exception {
        Path file = invalidReturnFixture(directory, "UnresolvedReturn", ClassDesc.of("missing.architecture.MissingDependency"));
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> CompiledCore.classesAt(file));
        assertTrue(error.getMessage().contains("Could not resolve class MissingDependency"), error.getMessage());
    }

    private static Path invalidReturnFixture(Path directory, String name, ClassDesc argument) throws Exception {
        byte[] bytes = ClassFile.of(ClassFile.StackMapsOption.DROP_STACK_MAPS)
            .build(ClassDesc.of("architecture.fixtures." + name), builder -> builder
                .withMethodBody("invalidReturn", MethodTypeDesc.of(CD_String, argument),
                    ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC, code -> code.aload(0).areturn()));
        return Files.write(directory.resolve(name + ".class"), bytes);
    }

    /** Runs with only java.base resolved, regardless of the parent test JVM's Vector API options. */
    public static final class UnresolvedModuleProbe {
        private UnresolvedModuleProbe() {}

        public static void main(String[] arguments) {
            String moduleName = "jdk.incubator.vector";
            if (ModuleLayer.boot().findModule(moduleName).isPresent()) {
                throw new AssertionError("Vector API must remain unresolved in the verification probe");
            }
            if (ModuleFinder.ofSystem().find(moduleName).isEmpty()) {
                throw new AssertionError("The installed JDK must contain the Vector API module");
            }
            var classes = CompiledCore.classesAt(Path.of(arguments[0]));
            if (classes.stream().noneMatch(type -> CompiledCore.name(type)
                .equals("com.arbergashi.charts.engine.spatial.SpatialSimdKernels"))) {
                throw new AssertionError("The optional SIMD implementation was not verified");
            }
            if (ModuleLayer.boot().findModule(moduleName).isPresent()) {
                throw new AssertionError("Bytecode verification resolved the Vector API module");
            }
        }
    }
}
