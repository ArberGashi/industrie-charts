/**
 * ArberCharts Visual Verifier Client
 * @version 2.0.0
 * @license MIT
 */
(function() {
    'use strict';

    var SIZE_PRESETS = {
        sm: { width: 640, height: 384 },
        md: { width: 800, height: 480 },
        lg: { width: 1200, height: 720 }
    };

    var cardState = new WeakMap();
    var animationTimers = new WeakMap();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    function init() {
        applyInitialTheme();
        initCardState();
        bindHeaderActions();
        bindCategoryNavigation();
        bindCardControls();
        loadAllRenderers(false);
    }

    function applyInitialTheme() {
        var saved = localStorage.getItem('vv-theme');
        var theme = (saved === 'dark' || saved === 'light') ? saved : 'light';
        setGlobalTheme(theme);
    }

    function setGlobalTheme(theme) {
        document.body.setAttribute('data-theme', theme);
        document.body.classList.toggle('light', theme === 'light');
        localStorage.setItem('vv-theme', theme);
    }

    function initCardState() {
        var cards = document.querySelectorAll('.renderer-card-modern');
        var globalTheme = document.body.getAttribute('data-theme') || 'light';

        cards.forEach(function(card) {
            var canvas = card.querySelector('.preview-canvas');
            var size = detectSizeKey(canvas);
            cardState.set(card, { theme: globalTheme, size: size });
            syncCardControlState(card, globalTheme, size);
        });
    }

    function detectSizeKey(canvas) {
        var width = parseInt(canvas && canvas.dataset.width, 10);
        if (width >= SIZE_PRESETS.lg.width) {
            return 'lg';
        }
        if (width <= SIZE_PRESETS.sm.width) {
            return 'sm';
        }
        return 'md';
    }

    function syncCardControlState(card, theme, size) {
        card.querySelectorAll('.theme-switch button').forEach(function(button) {
            button.classList.toggle('active', button.dataset.theme === theme);
        });
        card.querySelectorAll('.size-controls button').forEach(function(button) {
            button.classList.toggle('active', button.dataset.size === size);
        });
    }

    function bindHeaderActions() {
        var refreshAll = document.getElementById('refreshAll');
        if (refreshAll) {
            refreshAll.addEventListener('click', function() {
                loadAllRenderers(true);
                notify('Refresh started for all renderers', 'info');
            });
        }

        var themeToggle = document.getElementById('themeToggle');
        if (themeToggle) {
            themeToggle.addEventListener('click', function() {
                var current = document.body.getAttribute('data-theme') || 'light';
                var next = current === 'light' ? 'dark' : 'light';
                setGlobalTheme(next);

                document.querySelectorAll('.renderer-card-modern').forEach(function(card) {
                    var state = cardState.get(card);
                    if (state) {
                        state.theme = next;
                        syncCardControlState(card, state.theme, state.size);
                    }
                });

                loadAllRenderers(true);
            });
        }
    }

    function bindCategoryNavigation() {
        var buttons = document.querySelectorAll('.category-nav-btn');
        var sections = document.querySelectorAll('.category-section');

        buttons.forEach(function(btn) {
            btn.addEventListener('click', function() {
                var category = this.dataset.category;

                buttons.forEach(function(button) {
                    button.classList.remove('active');
                });
                this.classList.add('active');

                sections.forEach(function(section) {
                    var visible = category === 'all' || section.dataset.category === category;
                    section.style.display = visible ? 'block' : 'none';
                });

                if (category !== 'all') {
                    var target = document.querySelector('.category-section[data-category="' + cssEscape(category) + '"]');
                    if (target) {
                        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }
                }
            });
        });
    }

    function bindCardControls() {
        document.addEventListener('click', function(event) {
            var themeButton = event.target.closest('.theme-switch button');
            if (themeButton) {
                onThemeSwitch(themeButton);
                return;
            }

            var sizeButton = event.target.closest('.size-controls button');
            if (sizeButton) {
                onSizeSwitch(sizeButton);
                return;
            }

            var actionButton = event.target.closest('.icon-btn[data-action]');
            if (actionButton) {
                onCardAction(actionButton);
                return;
            }

            var demoButton = event.target.closest('[data-action="demo-animations"]');
            if (demoButton) {
                startAnimationDemo();
            }
        });
    }

    function onThemeSwitch(button) {
        var card = button.closest('.renderer-card-modern');
        if (!card) {
            return;
        }
        var state = cardState.get(card);
        if (!state) {
            return;
        }

        state.theme = button.dataset.theme === 'dark' ? 'dark' : 'light';
        syncCardControlState(card, state.theme, state.size);

        var canvas = card.querySelector('.preview-canvas');
        if (canvas) {
            loadRenderer(canvas, false);
        }
    }

    function onSizeSwitch(button) {
        var card = button.closest('.renderer-card-modern');
        if (!card) {
            return;
        }
        var state = cardState.get(card);
        if (!state) {
            return;
        }

        var nextSize = button.dataset.size;
        if (!SIZE_PRESETS[nextSize]) {
            return;
        }

        state.size = nextSize;
        syncCardControlState(card, state.theme, state.size);

        var canvas = card.querySelector('.preview-canvas');
        if (canvas) {
            canvas.dataset.width = String(SIZE_PRESETS[nextSize].width);
            canvas.dataset.height = String(SIZE_PRESETS[nextSize].height);
            loadRenderer(canvas, true);
        }
    }

    function onCardAction(button) {
        var card = button.closest('.renderer-card-modern');
        var canvas = card && card.querySelector('.preview-canvas');
        if (!card || !canvas) {
            return;
        }

        var action = button.dataset.action;
        if (action === 'fullscreen') {
            openFullscreen(card, canvas);
            return;
        }

        if (action === 'benchmark') {
            runBenchmark(card, canvas.dataset.rendererClass);
            return;
        }

        if (action === 'animate') {
            toggleAnimation(card, canvas);
        }
    }

    function loadAllRenderers(forceRefresh) {
        var canvases = document.querySelectorAll('.preview-canvas[data-renderer-class]');
        if (canvases.length === 0) {
            return;
        }

        canvases.forEach(function(canvas, index) {
            setTimeout(function() {
                loadRenderer(canvas, forceRefresh);
            }, index * 70);
        });
    }

    async function loadRenderer(canvas, forceRefresh) {
        var card = canvas.closest('.renderer-card-modern');
        if (!card) {
            return;
        }

        var state = cardState.get(card) || {
            theme: document.body.getAttribute('data-theme') || 'light',
            size: detectSizeKey(canvas)
        };

        var rendererClass = canvas.dataset.rendererClass;
        var width = parseInt(canvas.dataset.width, 10) || SIZE_PRESETS.md.width;
        var height = parseInt(canvas.dataset.height, 10) || SIZE_PRESETS.md.height;
        var theme = state.theme;

        if (!rendererClass) {
            showError(card, 'Missing renderer class');
            return;
        }

        card.classList.add('loading');
        card.classList.remove('error', 'loaded');

        var url = '/api/renderer?className=' + encodeURIComponent(rendererClass) +
            '&width=' + width +
            '&height=' + height +
            '&theme=' + theme +
            (forceRefresh ? '&_t=' + Date.now() : '');

        var startedAt = performance.now();

        try {
            var response = await fetch(url, {
                cache: forceRefresh ? 'no-store' : 'default'
            });

            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }

            var blob = await response.blob();
            var bitmap = await createBitmap(blob);
            drawBitmapToCanvas(canvas, bitmap, width, height);
            if (typeof bitmap.close === 'function') {
                bitmap.close();
            }

            card.classList.remove('loading', 'error');
            card.classList.add('loaded');

            updateRenderStats(card, response.headers.get('X-Render-Time-Ms'), performance.now() - startedAt);
        } catch (error) {
            console.error('Render failed for ' + rendererClass + ':', error);
            showError(card, 'Render failed');
        }
    }

    async function createBitmap(blob) {
        if (window.createImageBitmap) {
            return window.createImageBitmap(blob);
        }

        return new Promise(function(resolve, reject) {
            var image = new Image();
            var objectUrl = URL.createObjectURL(blob);

            image.onload = function() {
                URL.revokeObjectURL(objectUrl);
                resolve(image);
            };

            image.onerror = function() {
                URL.revokeObjectURL(objectUrl);
                reject(new Error('Image decode failed'));
            };

            image.src = objectUrl;
        });
    }

    function drawBitmapToCanvas(canvas, bitmap, width, height) {
        var dpr = window.devicePixelRatio || 1;
        canvas.width = Math.round(width * dpr);
        canvas.height = Math.round(height * dpr);

        var ctx = canvas.getContext('2d');
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, width, height);
        ctx.drawImage(bitmap, 0, 0, width, height);
    }

    function updateRenderStats(card, headerMs, fallbackMs) {
        var renderMs = parseFloat(headerMs);
        if (!Number.isFinite(renderMs)) {
            renderMs = fallbackMs;
        }

        var renderValue = card.querySelector('.render-time');
        if (renderValue) {
            renderValue.textContent = renderMs.toFixed(1) + ' ms';
        }

        var fps = Math.max(1, Math.min(240, Math.round(1000 / Math.max(1, renderMs))));
        var fpsValue = card.querySelector('.fps-counter');
        if (fpsValue) {
            fpsValue.textContent = String(fps);
        }
    }

    function showError(card, message) {
        card.classList.remove('loading', 'loaded');
        card.classList.add('error');
        var errorMessage = card.querySelector('.error-message');
        if (errorMessage) {
            errorMessage.textContent = message;
        }
        var renderValue = card.querySelector('.render-time');
        if (renderValue) {
            renderValue.textContent = '--';
        }
    }

    function toggleAnimation(card, canvas) {
        var existing = animationTimers.get(card);
        if (existing) {
            clearInterval(existing);
            animationTimers.delete(card);
            card.classList.remove('animating');
            notify('Animation stopped', 'info');
            return;
        }

        card.classList.add('animating');
        loadRenderer(canvas, true);

        var intervalId = setInterval(function() {
            loadRenderer(canvas, true);
        }, 1000);

        animationTimers.set(card, intervalId);
        notify('Animation started', 'success');
    }

    function startAnimationDemo() {
        var cards = document.querySelectorAll('.renderer-card-modern');
        var started = 0;

        cards.forEach(function(card) {
            var canvas = card.querySelector('.preview-canvas');
            var rendererClass = canvas && canvas.dataset.rendererClass;
            if (!canvas || !rendererClass) {
                return;
            }

            var simpleName = rendererClass.split('.').pop();
            var isAnimated = Array.isArray(window.ANIMATED_RENDERERS) && window.ANIMATED_RENDERERS.indexOf(simpleName) !== -1;
            if (!isAnimated || animationTimers.has(card)) {
                return;
            }

            toggleAnimation(card, canvas);
            started += 1;
        });

        if (started === 0) {
            notify('No animation-capable renderers found', 'info');
        } else {
            notify('Started animation demo for ' + started + ' renderers', 'success');
        }
    }

    async function runBenchmark(card, rendererClass) {
        if (!rendererClass) {
            return;
        }

        try {
            var response = await fetch('/api/benchmark/' + encodeURIComponent(rendererClass) + '?iterations=100&warmup=20');
            if (!response.ok) {
                throw new Error('HTTP ' + response.status);
            }
            var result = await response.json();
            var message = 'Avg ' + result.avgTimeMs.toFixed(2) + ' ms, p99 ' + result.p99Ms.toFixed(2) + ' ms, throughput ' + result.throughput.toFixed(1) + '/s';
            notify(message, 'success');

            var renderValue = card.querySelector('.render-time');
            if (renderValue) {
                renderValue.textContent = result.avgTimeMs.toFixed(1) + ' ms';
            }
            var fpsValue = card.querySelector('.fps-counter');
            if (fpsValue) {
                fpsValue.textContent = String(Math.max(1, Math.round(result.throughput)));
            }
        } catch (error) {
            console.error('Benchmark failed for ' + rendererClass + ':', error);
            notify('Benchmark failed', 'error');
        }
    }

    function openFullscreen(card, canvas) {
        var rendererClass = canvas.dataset.rendererClass || 'Renderer';
        var title = rendererClass.split('.').pop();
        var dataUrl;

        try {
            dataUrl = canvas.toDataURL('image/png');
        } catch (error) {
            console.error('Fullscreen export failed:', error);
            notify('Fullscreen export failed', 'error');
            return;
        }

        var win = window.open('', '_blank', 'noopener,noreferrer');
        if (!win) {
            notify('Popup blocked by browser', 'error');
            return;
        }

        win.document.write('<!doctype html><html><head><meta charset="utf-8"><title>' + title + '</title>' +
            '<style>body{margin:0;background:#0f172a;display:flex;align-items:center;justify-content:center;min-height:100vh}' +
            'img{max-width:98vw;max-height:98vh;object-fit:contain;border-radius:12px;box-shadow:0 12px 40px rgba(0,0,0,.45)}' +
            '</style></head><body><img alt="' + title + '" src="' + dataUrl + '"></body></html>');
        win.document.close();
    }

    function notify(message, type) {
        var toast = document.createElement('div');
        toast.className = 'notification notification-' + (type || 'info');
        toast.textContent = message;
        document.body.appendChild(toast);

        requestAnimationFrame(function() {
            toast.classList.add('show');
        });

        setTimeout(function() {
            toast.classList.remove('show');
            setTimeout(function() {
                toast.remove();
            }, 250);
        }, 2500);
    }

    function cssEscape(value) {
        if (window.CSS && typeof window.CSS.escape === 'function') {
            return window.CSS.escape(value);
        }
        return String(value).replace(/"/g, '\\"');
    }
})();
