package com.arbergashi.charts.springdemo;

import org.springframework.boot.SpringBootVersion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** HTTP endpoints for the bounded example catalog and server-rendered PNG images. */
@RestController
final class WorkbenchController {
    private final DemoChartService charts;

    WorkbenchController(DemoChartService charts) {
        this.charts = charts;
    }

    @GetMapping("/api/examples")
    Map<String, Object> catalog() {
        return Map.of("examples", DemoExamples.catalog(), "springBoot", SpringBootVersion.getVersion(),
                "java", Runtime.version().feature(), "runtime", System.getProperty("java.vendor"),
                "dataSource", "Synthetic demo data");
    }

    @GetMapping(value = "/api/charts/{id}.png", produces = MediaType.IMAGE_PNG_VALUE)
    ResponseEntity<byte[]> chart(@PathVariable String id,
                                 @RequestParam(defaultValue = "960") int width,
                                 @RequestParam(defaultValue = "540") int height,
                                 @RequestParam(defaultValue = "256") int samples,
                                 @RequestParam(defaultValue = "dark") String theme,
                                 @RequestParam(defaultValue = "1") int scale,
                                 @RequestParam(defaultValue = "false") boolean download) {
        DemoExamples.Example example = DemoExamples.find(id);
        long start = System.nanoTime();
        byte[] image = charts.render(example, width, height, samples, theme, scale);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (download ? "attachment" : "inline") + "; filename=\"charts-" + id + "-" + theme + ".png\"")
                .header("Server-Timing", "render;dur=" + (System.nanoTime() - start) / 1_000_000L)
                .body(image);
    }
}
