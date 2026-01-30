package com.arbergashi.charts.bridge.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Loads the embedded native library or uses ARBER_NATIVE_PATH if provided.
 */
public final class NativeLibraryLoader {
    private static final String ENV_PATH = "ARBER_NATIVE_PATH";
    private static final String RESOURCE_ROOT = "/native";

    private NativeLibraryLoader() {
    }

    public static void load() {
        String override = System.getenv(ENV_PATH);
        if (override != null && !override.isBlank()) {
            Path candidate = Path.of(override).resolve(libraryFileName());
            System.load(candidate.toAbsolutePath().toString());
            return;
        }

        String resourcePath = RESOURCE_ROOT + "/" + osToken() + "/" + archToken() + "/" + libraryFileName();
        try (InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Native library not found on classpath: " + resourcePath);
            }
            Path temp = Files.createTempFile("arbercharts-core-", libraryFileName());
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            System.load(temp.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load embedded native library", e);
        }
    }

    private static String libraryFileName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return "libarbercharts-core.dylib";
        }
        if (os.contains("win")) {
            return "arbercharts-core.dll";
        }
        return "libarbercharts-core.so";
    }

    private static String osToken() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "macos";
        if (os.contains("win")) return "windows";
        return "linux";
    }

    private static String archToken() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) return "aarch64";
        if (arch.contains("x86_64") || arch.contains("amd64")) return "x86_64";
        return arch;
    }
}
