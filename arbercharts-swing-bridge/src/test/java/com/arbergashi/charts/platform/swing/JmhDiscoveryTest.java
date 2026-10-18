package com.arbergashi.charts.platform.swing;

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.BenchmarkListEntry;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JmhDiscoveryTest {
    @Test
    void generatedBenchmarkListContainsEverySwingBenchmark() throws Exception {
        Set<String> expected = Stream.of(RenderHintsJmhBenchmark.class, ThemeSwitchJmhBenchmark.class)
                .flatMap(type -> Arrays.stream(type.getDeclaredMethods())
                        .filter(method -> method.isAnnotationPresent(Benchmark.class))
                        .map(method -> type.getName() + "." + method.getName()))
                .collect(Collectors.toSet());
        assertFalse(expected.isEmpty());

        try (InputStream input = getClass().getResourceAsStream("/META-INF/BenchmarkList")) {
            assertNotNull(input, "JMH annotation processing must generate the runnable benchmark index");
            Set<String> generated = BenchmarkList.readBenchmarkList(input).stream()
                    .map(BenchmarkListEntry::getUsername)
                    .collect(Collectors.toSet());
            assertEquals(expected, generated);
        }
    }
}
