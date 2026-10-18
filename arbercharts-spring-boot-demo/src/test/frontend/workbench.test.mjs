import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";
import vm from "node:vm";

const source = await readFile(new URL("../../main/resources/static/workbench.js", import.meta.url), "utf8");
const html = await readFile(new URL("../../main/resources/static/index.html", import.meta.url), "utf8");
const elementIds = new Set([...html.matchAll(/\bid="([^"]+)"/g)].map((match) => match[1]));
const examples = ["line", "scatter"].map((id) => ({
    id, name: id, category: "Examples", dataset: `${id} dataset`, description: `${id} description`,
    xLabel: "X", yLabel: "Y", defaultSamples: 256, renderer: `${id}Renderer`
}));
examples.push(
    { id: "donut", name: "Donut", category: "Composition", dataset: "Energy mix", description: "Category shares.",
        xLabel: "Energy source", yLabel: "Share (%)", defaultSamples: 256, sampleLabel: "Observations", presentation: "donut", renderer: "DonutRenderer" },
    { id: "radar", name: "Radar", category: "Comparison", dataset: "System profile", description: "Six normalized scores.",
        xLabel: "Six metrics", yLabel: "Score (0-100)", defaultSamples: 96, sampleLabel: "Samples / metric", presentation: "radar", renderer: "RadarRenderer" },
    { id: "heatmap", name: "Heatmap", category: "Scientific", dataset: "Response field", description: "A sampled grid.",
        xLabel: "Position X (mm)", yLabel: "Position Y (mm)", defaultSamples: 1024, sampleLabel: "Cell budget", presentation: "heatmap", renderer: "HeatmapRenderer" }
);
const catalog = { examples, springBoot: "test", java: 25, runtime: "test" };

// These tests exercise the actual script's asynchronous UI state without a DOM package.
// Browser layout, keyboard navigation and real PNG decoding are checked separately.
function workbench({ automaticImageDecoding = true } = {}) {
    const elements = new Map();
    let document;
    class Element {
        constructor(tag = "div") {
            this.tag = tag;
            this.dataset = {};
            this.children = [];
            this.attributes = new Map();
            this.events = new Map();
            this.value = "";
            this.textContent = "";
            this.disabled = false;
            this.hidden = false;
            this.clientWidth = 640;
            this.clientHeight = 360;
            this.classList = new Set();
            this.classList.remove = this.classList.delete.bind(this.classList);
        }
        append(...children) { this.children.push(...children); }
        replaceChildren(...children) { this.children = children; }
        setAttribute(name, value) { this.attributes.set(name, String(value)); }
        getAttribute(name) { return this.attributes.get(name) ?? null; }
        removeAttribute(name) { this.attributes.delete(name); }
        set href(value) { this.setAttribute("href", value); }
        get href() { return this.getAttribute("href"); }
        addEventListener(name, listener) { this.events.set(name, listener); }
        focus() { document.activeElement = this; }
        querySelectorAll(tag) {
            return this.children.flatMap((child) => [
                ...(child.tag === tag ? [child] : []), ...child.querySelectorAll(tag)
            ]);
        }
        fire(name) {
            const event = { defaultPrevented: false, preventDefault() { this.defaultPrevented = true; } };
            this.events.get(name)?.(event);
            return event;
        }
    }
    const element = (id) => {
        // A browser returns null for an unknown ID. Inventing missing elements
        // here would hide a startup failure caused by HTML/script drift.
        if (!elementIds.has(id)) return null;
        if (!elements.has(id)) elements.set(id, new Element());
        return elements.get(id);
    };
    document = {
        documentElement: new Element("html"), activeElement: null, getElementById: element,
        createElement: (tag) => new Element(tag), createElementNS: (_, tag) => new Element(tag),
        createTextNode: (value) => Object.assign(new Element("text"), { textContent: value })
    };
    element("size").value = "fit";
    element("samples").value = "256";
    element("download").setAttribute("aria-disabled", "true");
    const requests = [];
    const imageDecodes = [];
    const imageDecoding = { automatic: automaticImageDecoding };
    const activeImageUrls = new Set();
    const createdImageUrls = [];
    const revokedImageUrls = [];
    const windowEvents = new Map();
    let imageSequence = 0;
    class ImageUrl extends URL {
        static createObjectURL() {
            const url = `blob:test-${++imageSequence}`;
            createdImageUrls.push(url);
            activeImageUrls.add(url);
            return url;
        }
        static revokeObjectURL(url) {
            revokedImageUrls.push(url);
            activeImageUrls.delete(url);
        }
    }
    const window = {
        location: { search: "", href: "http://127.0.0.1:8080/" }, devicePixelRatio: 1,
        history: { replaceState() {} },
        addEventListener(name, listener) { windowEvents.set(name, listener); }
    };
    const context = vm.createContext({
        document, window, URL: ImageUrl, URLSearchParams, AbortController, performance, setTimeout, clearTimeout,
        localStorage: { getItem() { return null; }, setItem() {} },
        ResizeObserver: class { observe() {} },
        Image: class {
            naturalWidth = 640;
            naturalHeight = 360;
            decode() {
                return new Promise((resolve, reject) => {
                    imageDecodes.push({ url: this.src, resolve, reject });
                    if (imageDecoding.automatic) resolve();
                });
            }
        },
        fetch(url, options = {}) {
            // Keep completion controllable even after cancellation to exercise
            // late network/decoder responses as well as observing the real signal.
            return new Promise((resolve, reject) => requests.push({ url, signal: options.signal, resolve, reject }));
        }
    });
    vm.runInContext(source, context, { filename: "workbench.js" });
    return {
        context, element, requests, imageDecodes, imageDecoding,
        activeImageUrls, createdImageUrls, revokedImageUrls,
        fireWindow(name) { windowEvents.get(name)?.(); }
    };
}

function respond(request, json = null, status = 200) {
    request.resolve({ ok: status >= 200 && status < 300, status, async json() { return json; },
        async blob() { return { size: 1024 }; } });
}

async function settle() {
    await new Promise((resolve) => setImmediate(resolve));
}

async function ready() {
    const app = workbench();
    respond(app.requests[0], catalog);
    await settle();
    respond(app.requests[1]);
    await settle();
    assert.equal(app.element("chart-stage").dataset.state, "ready");
    return app;
}

function choose(app, id) {
    const button = app.element("catalog").querySelectorAll("button").find((item) => item.dataset.example === id);
    assert.ok(button, `The catalog must contain ${id}.`);
    button.fire("click");
}

test("download loses its target immediately and stays unavailable after a failed render", async () => {
    const app = await ready();
    assert.match(app.element("download").href, /charts\/line\.png/);
    app.element("download").focus();
    choose(app, "scatter");
    assert.equal(app.element("download").href, null);
    assert.equal(app.element("download").getAttribute("aria-disabled"), "true");
    assert.equal(app.element("download").fire("click").defaultPrevented, true);
    respond(app.requests[2], { detail: "Temporary render failure." }, 503);
    await settle();
    assert.equal(app.element("chart-stage").dataset.state, "error");
    assert.equal(app.element("download").href, null);
    assert.equal(app.element("download").fire("click").defaultPrevented, true);
});

test("a successful retry enables only the newly selected chart download", async () => {
    const app = await ready();
    choose(app, "scatter");
    respond(app.requests[2], null, 503);
    await settle();
    app.element("render").fire("click");
    respond(app.requests[3]);
    await settle();
    assert.match(app.element("download").href, /charts\/scatter\.png/);
    assert.equal(app.element("download").getAttribute("aria-disabled"), "false");
    assert.equal(app.element("download").fire("click").defaultPrevented, false);
});

test("repeated initialization shares one catalog request and cannot reset a later selection", async () => {
    const app = workbench();
    const first = app.context.start();
    const second = app.context.start();
    assert.equal(first, second);
    assert.equal(app.requests.length, 1);
    assert.equal(app.element("render").disabled, true);
    assert.equal(app.element("samples").disabled, true);
    respond(app.requests[0], catalog);
    await first;
    respond(app.requests[1]);
    await settle();
    choose(app, "scatter");
    respond(app.requests[2]);
    await settle();
    await app.context.start();
    assert.equal(app.requests.filter((request) => request.url === "api/examples").length, 1);
    assert.equal(app.element("chart-title").textContent, "scatter dataset");
    assert.equal(app.element("chart-stage").dataset.state, "ready");
});

test("failed initialization restores the retry action and deduplicates the retry", async () => {
    const app = workbench();
    app.requests[0].reject(new Error("Catalog connection failed."));
    await settle();
    assert.equal(app.element("render").disabled, false);
    assert.equal(app.element("samples").disabled, true);
    assert.match(app.element("chart-message").textContent, /Catalog connection failed/);
    app.element("render").fire("click");
    const retry = app.context.start();
    assert.equal(app.requests.length, 2);
    assert.equal(app.element("render").disabled, true);
    assert.equal(app.element("chart-stage").dataset.state, "loading");
    respond(app.requests[1], catalog);
    await retry;
    respond(app.requests[2]);
    await settle();
    assert.equal(app.element("render").disabled, false);
    assert.equal(app.element("samples").disabled, false);
    assert.equal(app.element("chart-stage").dataset.state, "ready");
});

test("an empty catalog has an actionable error and allows retry", async () => {
    const app = workbench();
    respond(app.requests[0], { examples: [] });
    await settle();
    assert.match(app.element("chart-message").textContent, /no chart examples/);
    assert.equal(app.element("render").disabled, false);
    assert.equal(app.element("download").href, null);
});

test("an obsolete image response cannot reactivate its download", async () => {
    const app = await ready();
    choose(app, "scatter");
    choose(app, "line");
    respond(app.requests[2]);
    await settle();
    assert.equal(app.element("download").href, null);
    respond(app.requests[3]);
    await settle();
    assert.match(app.element("download").href, /charts\/line\.png/);
});

test("the actual page controls initialize and its theme button requests the new theme", async () => {
    const app = await ready();
    assert.equal(app.element("copy-url-missing"), null, "Missing HTML controls must not be fabricated.");
    assert.ok(app.element("copy-url"));
    app.element("theme-toggle").fire("click");
    assert.match(app.requests[2].url, /theme=light/);
    assert.equal(app.element("download").href, null);
    respond(app.requests[2]);
    await settle();
    assert.match(app.element("download").href, /theme=light/);
    assert.equal(app.element("theme-toggle").getAttribute("aria-label"), "Switch to dark theme");
});

test("an image remains unavailable until the browser has decoded it successfully", async () => {
    const app = workbench({ automaticImageDecoding: false });
    respond(app.requests[0], catalog);
    await settle();
    respond(app.requests[1]);
    await settle();
    assert.equal(app.imageDecodes.length, 1);
    assert.equal(app.element("chart-stage").dataset.state, "loading");
    assert.equal(app.element("chart-stage").getAttribute("aria-busy"), "true");
    assert.equal(app.element("download").href, null);
    assert.equal(app.element("copy-url").disabled, true);
    app.imageDecodes[0].resolve();
    await settle();
    assert.equal(app.element("chart-stage").dataset.state, "ready");
    assert.equal(app.element("chart-stage").getAttribute("aria-busy"), "false");
    assert.equal(app.element("chart-image").src, app.imageDecodes[0].url);
    assert.match(app.element("download").href, /charts\/line\.png/);
});

test("a corrupt image releases its URL and keeps export unavailable until a decoded retry", async () => {
    const app = workbench({ automaticImageDecoding: false });
    respond(app.requests[0], catalog);
    await settle();
    respond(app.requests[1]);
    await settle();
    const failedUrl = app.imageDecodes[0].url;
    app.imageDecodes[0].reject(new Error("The PNG could not be decoded."));
    await settle();
    assert.equal(app.element("chart-stage").dataset.state, "error");
    assert.match(app.element("chart-message").textContent, /PNG could not be decoded/);
    assert.equal(app.element("chart-image").hidden, true);
    assert.equal(app.element("download").href, null);
    assert.equal(app.element("copy-url").disabled, true);
    assert.deepEqual(app.revokedImageUrls, [failedUrl]);
    assert.equal(app.activeImageUrls.size, 0);
    app.element("render").fire("click");
    respond(app.requests[2]);
    await settle();
    assert.equal(app.element("download").href, null);
    app.imageDecodes[1].resolve();
    await settle();
    assert.equal(app.element("chart-stage").dataset.state, "ready");
    assert.equal(app.element("chart-image").src, app.imageDecodes[1].url);
    assert.equal(app.activeImageUrls.size, 1);
    assert.match(app.element("download").href, /charts\/line\.png/);
});

test("replacing a decoded chart and leaving the page release the owned image URLs", async () => {
    const app = await ready();
    const previous = app.element("chart-image").src;
    app.imageDecoding.automatic = false;
    choose(app, "scatter");
    respond(app.requests[2]);
    await settle();
    const replacement = app.imageDecodes[1].url;
    assert.deepEqual([...app.activeImageUrls], [previous, replacement]);
    assert.deepEqual(app.revokedImageUrls, [], "The displayed image must remain valid while its replacement decodes.");
    app.imageDecodes[1].resolve();
    await settle();
    assert.equal(app.element("chart-image").src, replacement);
    assert.deepEqual(app.revokedImageUrls, [previous]);
    assert.deepEqual([...app.activeImageUrls], [replacement]);
    app.fireWindow("pagehide");
    assert.deepEqual(app.revokedImageUrls, [previous, replacement]);
    assert.equal(app.activeImageUrls.size, 0);
});

test("an obsolete decode cannot replace a newer chart and releases its unused URL", async () => {
    const app = await ready();
    const previous = app.element("chart-image").src;
    app.imageDecoding.automatic = false;
    choose(app, "scatter");
    respond(app.requests[2]);
    await settle();
    const obsolete = app.imageDecodes[1];
    choose(app, "line");
    respond(app.requests[3]);
    await settle();
    const current = app.imageDecodes[2];
    current.resolve();
    await settle();
    obsolete.resolve();
    await settle();
    assert.equal(app.element("chart-image").src, current.url);
    assert.match(app.element("download").href, /charts\/line\.png/);
    assert.deepEqual(app.revokedImageUrls, [previous, obsolete.url]);
    assert.deepEqual([...app.activeImageUrls], [current.url]);
});

test("superseding a render and leaving the page abort the outstanding requests", async () => {
    const app = await ready();
    choose(app, "scatter");
    const previous = app.requests[2];
    assert.ok(previous.signal instanceof AbortSignal);
    assert.equal(previous.signal.aborted, false);
    let previousAborts = 0;
    previous.signal.addEventListener("abort", () => previousAborts++);
    choose(app, "line");
    const current = app.requests[3];
    assert.equal(previous.signal.aborted, true);
    assert.equal(previousAborts, 1);
    assert.equal(current.signal.aborted, false);
    app.fireWindow("pagehide");
    assert.equal(current.signal.aborted, true);
    previous.reject(Object.assign(new Error("Obsolete request cancelled."), { name: "AbortError" }));
    current.reject(Object.assign(new Error("Page closed."), { name: "AbortError" }));
    await settle();
    assert.notEqual(app.element("chart-stage").dataset.state, "error");
});


test("radial examples describe their actual coordinates and restore Cartesian labels when switching", async () => {
    const app = await ready();
    choose(app, "donut");
    assert.equal(app.element("sample-label").textContent, "Observations");
    assert.equal(app.element("inspector-x-title").textContent, "Dimensions");
    assert.equal(app.element("series-symbol").hidden, true);
    respond(app.requests[2]);
    await settle();
    assert.match(app.element("chart-image").alt, /Labeled sectors/);
    assert.doesNotMatch(app.element("chart-caption").textContent, /Horizontal axis|Both axes/);
    choose(app, "radar");
    assert.equal(app.element("sample-label").textContent, "Samples / metric");
    respond(app.requests[3]);
    await settle();
    assert.match(app.element("chart-image").alt, /radial axes.*0 to 100/);
    choose(app, "scatter");
    assert.equal(app.element("inspector-x-title").textContent, "Horizontal axis");
    assert.equal(app.element("series-symbol").hidden, false);
    assert.equal(app.element("sample-label").textContent, "Samples");
});

test("heatmap exposes a cell budget and a response color scale instead of a point-count claim", async () => {
    const app = await ready();
    choose(app, "heatmap");
    assert.equal(app.element("sample-label").textContent, "Cell budget");
    assert.equal(app.element("inspector-sample-label").textContent, "Cell budget");
    assert.equal(app.element("samples").value, "1024");
    assert.equal(app.element("series-symbol").hidden, true);
    respond(app.requests[2]);
    await settle();
    assert.match(app.element("chart-image").alt, /Cell budget: 1024/);
    assert.match(app.element("chart-caption").textContent, /color scale shows response values/);
});
