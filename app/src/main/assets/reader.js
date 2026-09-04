// ─── Global State ───
let highlights = [];
let currentTheme = 0;
let currentFontSize = 16;
let currentColumns = 1;
let isSpeaking = false;
let scrollTimeout = null;

// ─── Initialization (Called from Kotlin in Phase 2) ───
function initReader(text, themeIndex, fontSize, columnsCount, scrollY, highlightsJson) {
    try {
        var content = document.getElementById('content');
        // textContent preserves exact offsets (innerText corrupts them)
        content.textContent = text;

        setTheme(themeIndex);
        setFontSize(fontSize);
        setColumns(columnsCount);

        try {
            highlights = JSON.parse(highlightsJson || "[]");
            applyAllHighlights();
        } catch (e) { highlights = []; }

        updateProgressBar();

        if (scrollY > 0) {
            setTimeout(function() { window.scrollTo(0, scrollY); }, 150);
        }

        // This interface is provided by Kotlin in Phase 2
        if (window.Android && typeof window.Android.onReady === 'function') {
            window.Android.onReady();
        }
    } catch (e) {
        console.error('initReader error:', e);
    }
}

// ─── Theme ───
function setTheme(index) {
    document.body.classList.remove('theme-light', 'theme-sepia', 'theme-night');
    var names = ['light', 'sepia', 'night'];
    document.body.classList.add('theme-' + names[index]);
    currentTheme = index;
}

function setFontSize(size) {
    document.documentElement.style.setProperty('--font-size', size + 'px');
    currentFontSize = size;
}

function setColumns(count) {
    document.documentElement.style.setProperty('--columns', count);
    currentColumns = count;
}

// ─── Highlights ───
function applyAllHighlights() {
    document.querySelectorAll('.highlight').forEach(function(el) {
        var parent = el.parentNode;
        parent.replaceChild(document.createTextNode(el.innerText), el);
        parent.normalize();
    });

    var sorted = highlights.slice().sort(function(a, b) { return b.startOffset - a.startOffset; });
    sorted.forEach(function(h) { applyOneHighlight(h); });
}

function applyOneHighlight(h) {
    var content = document.getElementById('content');
    if (!content) return;

    var currentOffset = 0;
    var walker = document.createTreeWalker(content, NodeFilter.SHOW_TEXT, null);
    var node = walker.nextNode();
    var startNode = null, endNode = null;
    var startNodeOffset = 0, endNodeOffset =
