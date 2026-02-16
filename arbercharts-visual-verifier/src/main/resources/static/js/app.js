/**
 * ArberCharts Visual Verifier - Simple & Working Client
 * @version 2.0.0
 * @license MIT
 */

(function() {
    'use strict';

    // Wait for DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    function init() {
        console.log('✓ Visual Verifier initializing...');
        loadAllRenderers();
        bindCategoryNavigation();
        console.log('✓ Visual Verifier ready');
    }

    /**
     * Load all renderer images
     */
    function loadAllRenderers() {
        var canvases = document.querySelectorAll('canvas[data-renderer-class]');
        console.log('Found ' + canvases.length + ' renderers to load...');

        if (canvases.length === 0) {
            console.warn('No canvas elements found! Check HTML template.');
            return;
        }

        canvases.forEach(function(canvas, index) {
            // Delay loading to prevent overwhelming the server
            setTimeout(function() {
                loadRenderer(canvas);
            }, index * 100);
        });
    }

    /**
     * Load a single renderer into canvas
     */
    function loadRenderer(canvas) {
        var card = canvas.closest('.renderer-card-modern');
        var rendererClass = canvas.dataset.rendererClass;
        var width = parseInt(canvas.dataset.width) || 800;
        var height = parseInt(canvas.dataset.height) || 480;
        var theme = 'light';

        if (!rendererClass) {
            console.warn('Missing renderer class on canvas');
            return;
        }

        // Show loading state
        if (card) {
            card.classList.add('loading');
            card.classList.remove('error', 'loaded');
        }

        // Build API URL
        var url = '/api/renderer?className=' + encodeURIComponent(rendererClass) +
                  '&width=' + width +
                  '&height=' + height +
                  '&theme=' + theme;

        // Create image element
        var img = new Image();
        img.crossOrigin = 'anonymous';

        img.onload = function() {
            try {
                // Set canvas dimensions
                canvas.width = width;
                canvas.height = height;

                // Draw image to canvas
                var ctx = canvas.getContext('2d');
                ctx.clearRect(0, 0, width, height);
                ctx.drawImage(img, 0, 0, width, height);

                // Success state
                if (card) {
                    card.classList.remove('loading');
                    card.classList.add('loaded');
                }

                var simpleName = rendererClass.split('.').pop();
                console.log('✓ Loaded: ' + simpleName);
            } catch (error) {
                console.error('Failed to draw ' + rendererClass + ':', error);
                showError(card, 'Draw failed');
            }
        };

        img.onerror = function() {
            console.error('Failed to load: ' + rendererClass);
            showError(card, 'Load failed');
        };

        // Start loading
        img.src = url;
    }

    /**
     * Show error state
     */
    function showError(card, message) {
        if (!card) return;
        card.classList.remove('loading');
        card.classList.add('error');
        var errorMsg = card.querySelector('.error-message');
        if (errorMsg) {
            errorMsg.textContent = message;
        }
    }

    /**
     * Category navigation
     */
    function bindCategoryNavigation() {
        var buttons = document.querySelectorAll('.category-nav-btn');

        buttons.forEach(function(btn) {
            btn.addEventListener('click', function() {
                var category = this.dataset.category;

                // Update active button
                buttons.forEach(function(b) {
                    b.classList.remove('active');
                });
                this.classList.add('active');

                // Filter sections
                var sections = document.querySelectorAll('.category-section');
                if (category === 'all') {
                    sections.forEach(function(section) {
                        section.style.display = 'block';
                    });
                } else {
                    sections.forEach(function(section) {
                        if (section.dataset.category === category) {
                            section.style.display = 'block';
                            section.scrollIntoView({ behavior: 'smooth', block: 'start' });
                        } else {
                            section.style.display = 'none';
                        }
                    });
                }
            });
        });
    }

})();

