package com.arbergashi.charts.visualverifier.controller;

import com.arbergashi.charts.visualverifier.dto.ComparisonResult;
import com.arbergashi.charts.visualverifier.service.VisualRegressionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for visual regression testing.
 *
 * <p>Provides endpoints for comparing rendered charts against baseline snapshots,
 * updating baselines, and running full regression test suites.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/regression/compare/{renderer}} - Compare against baseline</li>
 *   <li>{@code POST /api/regression/baseline/{renderer}} - Update baseline</li>
 *   <li>{@code GET /api/regression/run-all} - Run all regression tests</li>
 *   <li>{@code GET /api/regression/diff/{renderer}} - Get diff image</li>
 * </ul>
 *
 * @author Arber Gashi
 * @version 2.0.0
 * @since 2026-01-01
 */
@RestController
@RequestMapping("/api/regression")
public class RegressionController {

    private final VisualRegressionService regressionService;

    public RegressionController(VisualRegressionService regressionService) {
        this.regressionService = regressionService;
    }

    /**
     * Compares a renderer against its baseline snapshot.
     *
     * @param renderer fully qualified renderer class name
     * @param width    chart width (default 800)
     * @param height   chart height (default 600)
     * @param theme    theme name (default light)
     * @return comparison result with match percentage
     */
    @GetMapping("/compare/{renderer}")
    public ComparisonResult compare(
            @PathVariable String renderer,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "light") String theme
    ) {
        return regressionService.compare(renderer, width, height, theme);
    }

    /**
     * Updates the baseline snapshot for a renderer.
     *
     * @param renderer fully qualified renderer class name
     * @param width    chart width (default 800)
     * @param height   chart height (default 600)
     * @param theme    theme name (default light)
     * @return success status
     */
    @PostMapping("/baseline/{renderer}")
    public Map<String, Object> updateBaseline(
            @PathVariable String renderer,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "light") String theme
    ) {
        boolean success = regressionService.updateBaseline(renderer, width, height, theme);
        return Map.of(
                "renderer", renderer,
                "success", success,
                "message", success ? "Baseline updated" : "Failed to update baseline"
        );
    }

    /**
     * Runs visual regression tests on all renderers.
     *
     * @return list of comparison results with summary
     */
    @GetMapping("/run-all")
    public Map<String, Object> runAll() {
        List<ComparisonResult> results = regressionService.runAllTests();

        long passed = results.stream().filter(ComparisonResult::matches).count();
        long failed = results.stream().filter(r -> !r.matches() && r.status() == ComparisonResult.Status.MISMATCH).count();
        long noBaseline = results.stream().filter(r -> r.status() == ComparisonResult.Status.NO_BASELINE).count();
        long errors = results.stream().filter(r -> r.status() == ComparisonResult.Status.ERROR).count();

        return Map.of(
                "summary", Map.of(
                        "total", results.size(),
                        "passed", passed,
                        "failed", failed,
                        "noBaseline", noBaseline,
                        "errors", errors
                ),
                "results", results
        );
    }

    /**
     * Gets the diff image for a failed comparison.
     *
     * @param renderer fully qualified renderer class name
     * @param width    chart width (default 800)
     * @param height   chart height (default 600)
     * @param theme    theme name (default light)
     * @return diff image as PNG or 404 if no diff
     */
    @GetMapping(value = "/diff/{renderer}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getDiff(
            @PathVariable String renderer,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "light") String theme
    ) {
        ComparisonResult result = regressionService.compare(renderer, width, height, theme);

        if (result.diffImage() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("X-Match-Percentage", String.format("%.2f", result.matchPercentage() * 100))
                .header("X-Diff-Pixels", String.valueOf(result.diffPixels()))
                .body(result.diffImage());
    }
}
