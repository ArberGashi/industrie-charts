"use strict";

const $ = (id) => document.getElementById(id);
const parameters = new URLSearchParams(window.location.search);
const state = { examples: [], selected: null, theme: "dark", request: null, imageUrl: null, lastUrl: null, renderId: 0 };
let startPromise = null;
const glyphs = {
    line: "M2 14 6 9 10 11 14 4 18 7",
    area: "M2 16V12L6 6 10 10 14 4 18 7V16Z",
    step: "M2 15H6V10H10V5H14V8H18",
    bar: "M3 16V10M8 16V5M13 16V8M18 16V2",
    scatter: "M3 13h.1M7 7h.1M10 12h.1M12 3h.1M16 8h.1M18 4h.1",
    candlestick: "M5 2V18M15 1V17M2 6H8V12H2ZM12 5H18V10H12Z",
    donut: "M10 2A8 8 0 1 1 2 10H6A4 4 0 1 0 10 6ZM2 8A8 8 0 0 1 8 2V6A4 4 0 0 0 6 8Z",
    radar: "M10 2 18 8 15 17 5 17 2 8ZM10 2V10L18 8M10 10 15 17M10 10 5 17M10 10 2 8M10 5 15 9 12 14 6 15 5 9Z",
    heatmap: "M2 2H18V18H2ZM2 7H18M2 12H18M7 2V18M12 2V18M8 8H11V11H8Z",
    boxplot: "M5 2V6M5 13V18M2 6H8V13H2ZM2 9H8M15 1V4M15 11V17M12 4H18V11H12ZM12 7H18",
    kde: "M2 17C4 17 3 3 7 3S9 16 12 16 13 7 15 7 17 16 18 17"
};

try {
    const stored = parameters.get("theme") || localStorage.getItem("charts-workbench-theme");
    if (stored === "light" || stored === "dark") state.theme = stored;
} catch (_) {
    // The workbench remains usable when browser storage is disabled.
}
applyTheme();

function applyTheme() {
    document.documentElement.dataset.theme = state.theme;
    const next = state.theme === "dark" ? "light" : "dark";
    $("theme-toggle").textContent = `${next === "light" ? "Light" : "Dark"} theme`;
    $("theme-toggle").setAttribute("aria-label", `Switch to ${next} theme`);
}

function drawCatalog() {
    const focusedExample = document.activeElement?.dataset.example;
    const query = $("search").value.trim().toLowerCase();
    const shown = state.examples.filter((example) => `${example.name} ${example.category} ${example.dataset}`.toLowerCase().includes(query));
    const catalog = $("catalog");
    catalog.replaceChildren();
    for (const category of [...new Set(shown.map((example) => example.category))]) {
        const group = document.createElement("div");
        group.className = "catalog-group";
        const heading = document.createElement("h2");
        heading.textContent = category;
        group.append(heading);
        for (const example of shown.filter((item) => item.category === category)) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "example-button";
            button.dataset.example = example.id;
            button.setAttribute("aria-current", String(state.selected?.id === example.id));
            const icon = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            icon.setAttribute("viewBox", "0 0 20 20");
            icon.setAttribute("class", "chart-glyph");
            icon.setAttribute("aria-hidden", "true");
            const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
            path.setAttribute("d", glyphs[example.id] || glyphs.line);
            path.setAttribute("fill", "none");
            path.setAttribute("stroke", "currentColor");
            path.setAttribute("stroke-width", example.id === "scatter" ? "3" : "1.5");
            path.setAttribute("stroke-linecap", "round");
            path.setAttribute("stroke-linejoin", "round");
            icon.append(path);
            const label = document.createElement("span");
            label.textContent = example.name;
            const count = document.createElement("span");
            count.className = "point-count";
            count.textContent = String(example.defaultSamples);
            count.title = `${example.defaultSamples} ${(example.sampleLabel || "samples").toLowerCase()}`;
            button.append(icon, label, count);
            button.addEventListener("click", () => selectExample(example));
            group.append(button);
        }
        catalog.append(group);
    }
    $("catalog-empty").hidden = shown.length !== 0;
    $("example-count").textContent = String(shown.length);
    if (focusedExample) {
        [...catalog.querySelectorAll("button")].find((button) => button.dataset.example === focusedExample)?.focus();
    }
}

function selectExample(example) {
    state.selected = example;
    $("samples").value = String(example.defaultSamples);
    $("sample-label").textContent = example.sampleLabel || "Samples";
    $("inspector-sample-label").textContent = example.sampleLabel || "Samples";
    const radial = example.presentation === "donut" || example.presentation === "radar";
    $("inspector-x-title").textContent = radial ? "Dimensions" : "Horizontal axis";
    $("inspector-y-title").textContent = radial ? "Values" : "Vertical axis";
    $("series-symbol").hidden = radial || example.presentation === "heatmap" || example.presentation === "boxplot";
    $("tab-title").textContent = `${example.name}.chart`;
    $("chart-title").textContent = example.dataset;
    $("chart-description").textContent = example.description;
    $("series-name").textContent = example.dataset;
    $("inspector-renderer").textContent = example.renderer;
    $("inspector-x").textContent = example.xLabel;
    $("inspector-y").textContent = example.yLabel;
    $("chart-caption").textContent = `${example.name} chart. ${example.description} ${coordinateDescription(example)}`;
    drawCatalog();
    renderChart();
}

function coordinateDescription(example) {
    if (example.presentation === "donut") return "Labeled sectors show category shares; the center shows the total.";
    if (example.presentation === "radar") return "Six labeled radial axes share a score scale from 0 to 100.";
    const axes = `Horizontal axis: ${example.xLabel}. Vertical axis: ${example.yLabel}.`;
    return example.presentation === "heatmap" ? `${axes} The color scale shows response values.` : axes;
}

function outputSize() {
    if ($("size").value !== "fit") {
        const [width, height] = $("size").value.split("x").map(Number);
        return { width, height, scale: 1 };
    }
    const logicalWidth = Math.max(320, Math.min(1280, Math.floor($("chart-stage").clientWidth)));
    const logicalHeight = Math.min(620, Math.max(240, Math.floor($("chart-stage").clientHeight)));
    const scale = window.devicePixelRatio > 1 ? 2 : 1;
    return { width: logicalWidth * scale, height: logicalHeight * scale, scale };
}

async function renderChart() {
    if (!state.selected) return;
    state.request?.abort();
    const controller = new AbortController();
    state.request = controller;
    const renderId = ++state.renderId;
    const selected = state.selected;
    const size = outputSize();
    const samples = $("samples").value;
    const query = new URLSearchParams({ ...size, samples, theme: state.theme });
    const url = `api/charts/${selected.id}.png?${query}`;
    $("inspector-points").textContent = Number(samples).toLocaleString("en");
    $("chart-stage").dataset.state = "loading";
    $("chart-stage").setAttribute("aria-busy", "true");
    $("chart-message").textContent = "Rendering chart…";
    $("chart-message").hidden = false;
    $("render-status").textContent = "Rendering";
    $("footer-status").textContent = "Rendering";
    $("download").classList.add("disabled");
    $("download").setAttribute("aria-disabled", "true");
    $("download").removeAttribute("href");
    $("copy-url").disabled = true;
    const start = performance.now();
    let pendingImageUrl = null;
    try {
        const response = await fetch(url, { signal: controller.signal });
        if (!response.ok) {
            let detail = response.status === 429 ? "The renderer is busy. Select Render to retry." : `The chart could not be rendered (HTTP ${response.status}).`;
            try {
                const problem = await response.json();
                if (problem.detail) detail = problem.detail;
            } catch (_) {
                // Non-JSON error responses retain the actionable HTTP fallback.
            }
            throw new Error(detail);
        }
        const blob = await response.blob();
        pendingImageUrl = URL.createObjectURL(blob);
        const image = new Image();
        image.src = pendingImageUrl;
        await image.decode();
        if (renderId !== state.renderId) return;
        $("chart-image").src = pendingImageUrl;
        $("chart-image").alt = `${selected.name} chart of ${selected.dataset.toLowerCase()}. ${selected.sampleLabel || "Samples"}: ${samples}. ${coordinateDescription(selected)}`;
        $("chart-image").hidden = false;
        if (state.imageUrl) URL.revokeObjectURL(state.imageUrl);
        state.imageUrl = pendingImageUrl;
        pendingImageUrl = null;
        state.lastUrl = new URL(url, window.location.href).href;
        $("chart-stage").dataset.state = "ready";
        $("chart-message").hidden = true;
        $("image-size").textContent = `${image.naturalWidth} × ${image.naturalHeight}`;
        $("response-size").textContent = `${(blob.size / 1024).toFixed(1)} KB`;
        $("render-time").textContent = `${Math.round(performance.now() - start)} ms`;
        $("render-status").textContent = "Rendered successfully";
        $("footer-status").textContent = "Ready";
        $("request-url").textContent = `GET /${url}`;
        $("copy-url").disabled = false;
        $("download").href = `${url}&download=true`;
        $("download").classList.remove("disabled");
        $("download").setAttribute("aria-disabled", "false");
        const location = new URL(window.location.href);
        location.searchParams.set("example", selected.id);
        location.searchParams.set("theme", state.theme);
        window.history.replaceState(null, "", location);
    } catch (error) {
        if (error.name !== "AbortError" && renderId === state.renderId) showError(error.message);
    } finally {
        if (pendingImageUrl) URL.revokeObjectURL(pendingImageUrl);
        if (renderId === state.renderId) $("chart-stage").setAttribute("aria-busy", "false");
    }
}

function showError(message) {
    $("chart-stage").dataset.state = "error";
    $("chart-image").hidden = true;
    $("chart-message").textContent = `${message} Select Render to try again.`;
    $("chart-message").hidden = false;
    $("render-status").textContent = "Render failed";
    $("footer-status").textContent = "Connection or render error";
    $("chart-stage").setAttribute("aria-busy", "false");
}

function start() {
    if (state.selected) return Promise.resolve();
    if (startPromise) return startPromise;
    $("render").disabled = true;
    $("samples").disabled = true;
    $("size").disabled = true;
    $("chart-stage").dataset.state = "loading";
    $("chart-stage").setAttribute("aria-busy", "true");
    $("chart-message").textContent = "Loading chart examples…";
    $("chart-message").hidden = false;
    $("render-status").textContent = "Loading examples";
    $("footer-status").textContent = "Connecting";
    startPromise = loadCatalog().finally(() => {
        startPromise = null;
        $("render").disabled = false;
        $("samples").disabled = !state.selected;
        $("size").disabled = !state.selected;
    });
    return startPromise;
}

async function loadCatalog() {
    try {
        const response = await fetch("api/examples");
        if (!response.ok) throw new Error(`Could not load the examples (HTTP ${response.status}).`);
        const catalog = await response.json();
        if (!Array.isArray(catalog.examples) || catalog.examples.length === 0) {
            throw new Error("The server returned no chart examples.");
        }
        state.examples = catalog.examples;
        $("connection").replaceChildren();
        const dot = document.createElement("span");
        dot.className = "status-dot";
        $("connection").append(dot, document.createTextNode("Local server"));
        $("runtime-info").textContent = `Spring Boot ${catalog.springBoot} · Java ${catalog.java} · ${catalog.runtime}`;
        selectExample(state.examples.find((example) => example.id === parameters.get("example")) || state.examples[0]);
    } catch (error) {
        showError(error.message);
    }
}

$("search").addEventListener("input", drawCatalog);
$("samples").addEventListener("change", renderChart);
$("size").addEventListener("change", renderChart);
$("render").addEventListener("click", () => state.selected ? renderChart() : start());
$("download").addEventListener("click", (event) => {
    if ($("download").getAttribute("aria-disabled") === "true") event.preventDefault();
});
$("theme-toggle").addEventListener("click", () => {
    state.theme = state.theme === "dark" ? "light" : "dark";
    applyTheme();
    try { localStorage.setItem("charts-workbench-theme", state.theme); } catch (_) { /* Storage is optional. */ }
    renderChart();
});
$("copy-url").addEventListener("click", async () => {
    try {
        await navigator.clipboard.writeText(state.lastUrl);
        $("copy-url").textContent = "Copied";
    } catch (_) {
        $("copy-url").textContent = "Select URL to copy";
        const selection = window.getSelection();
        const range = document.createRange();
        range.selectNodeContents($("request-url"));
        selection.removeAllRanges();
        selection.addRange(range);
    }
    setTimeout(() => { $("copy-url").textContent = "Copy URL"; }, 2000);
});
let previousWidth = $("chart-stage").clientWidth;
let previousHeight = $("chart-stage").clientHeight;
let resizeTimer;
new ResizeObserver(() => {
    const width = $("chart-stage").clientWidth;
    const height = $("chart-stage").clientHeight;
    if (Math.abs(width - previousWidth) < 3 && Math.abs(height - previousHeight) < 3) return;
    previousWidth = width;
    previousHeight = height;
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => { if ($("size").value === "fit") renderChart(); }, 200);
}).observe($("chart-stage"));
window.addEventListener("pagehide", () => {
    state.request?.abort();
    if (state.imageUrl) URL.revokeObjectURL(state.imageUrl);
});
start();
