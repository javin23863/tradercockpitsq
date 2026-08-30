angular.module('app.resultstabs.resultsplugins').controller('ProfileChartCtrl', function($scope, $http, $timeout, SQResultsData, ProfileChartService, SkinService) {
    $scope.chartPaths = [];
    $scope.baseUrl = '';
    $scope.selectedPath = null;
    $scope.chartUrl = null;
    $scope.loading = false;
    $scope.svgLoading = false;

    var resizeObserver = null;
    var iframeLoadHandler = null;
    var loadedContextKey = null;

    function getChartIframe() {
        return document.querySelector('#results-profile-chart-container iframe');
    }

    // Disable iframe pointer-events during splitter drag so sqSplitter mousemove is not blocked.
    function onResizerMouseDown(e) {
        if (e.target && e.target.classList.contains('resizer')) {
            var iframe = getChartIframe();
            if (iframe) iframe.style.pointerEvents = 'none';
        }
    }
    function onResizerMouseUp() {
        var iframe = getChartIframe();
        if (iframe) iframe.style.pointerEvents = '';
    }
    document.addEventListener('mousedown', onResizerMouseDown, true);
    document.addEventListener('mouseup', onResizerMouseUp, true);

    $scope.$on('$destroy', function() {
        document.removeEventListener('mousedown', onResizerMouseDown, true);
        document.removeEventListener('mouseup', onResizerMouseUp, true);
        if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null; }
    });

    var CHART_PADDING = 20;

    // Generate all iframe CSS — layout, skin background and SVG element overrides.
    function buildSkinCss() {
        var isDark = SkinService.selectedSkin && SkinService.selectedSkin.name === "Dark skin";
        var bgColor = isDark ? "#1a1a2e" : "#f0f2f5";
        return "html,body{margin:0;padding:0;background:" + bgColor + ";overflow-x:auto;overflow-y:hidden;width:100%;height:100%;box-sizing:border-box;}" +
            // Match EquityChart-like panel behavior: SVG height tracks panel height.
            // Dynamic autoscale transform is disabled, so this is a stable visual scale.
            "svg{height:100%;width:auto;max-width:none;display:block;}" +
            "#toolbar,#replayToolbar,#annoGroup,#colorPalette{display:none!important;}" +
            (isDark ? "" :
                "#bgRect{fill:#f0f2f5!important}" +
                "#plotRect{fill:#ffffff!important}" +
                "#yAxisBg rect{fill:#f0f2f5!important}" +
                "#yAxisG text{fill:#1a1a2e!important}" +
                "#yAxisG line{stroke:#666!important}" +
                ".thTitle{fill:#1a1a2e!important}" +
                ".thBrand{fill:#555!important}" +
                ".thPrice{fill:#555!important}" +
                ".thGrid{stroke:#ccd!important}" +
                ".thDate{fill:#555!important}" +
                ".thVol{fill:#333!important}" +
                ".thStat{fill:#444!important}" +
                ".thSesDiv{stroke:#bbb!important}"
            );
    }

    // Build the HTML page that hosts the SVG — skin CSS is baked in to avoid flash on load.
    function buildHtmlWrapper(svgContent) {
        return '<!DOCTYPE html><html><head><meta charset="UTF-8">' +
               '<style>' + buildSkinCss() + '</style>' +
               '</head><body>' + svgContent + '</body></html>';
    }

    // Update skin CSS in-place when the skin changes (no iframe reload needed).
    function applyIframeStyles() {
        var iframe = getChartIframe();
        if (!iframe || !iframe.contentDocument || !iframe.contentDocument.head) return;
        var doc = iframe.contentDocument;
        var style = doc.getElementById('sqx-profile-style');
        if (!style) {
            style = doc.createElement('style');
            style.id = 'sqx-profile-style';
            doc.head.appendChild(style);
        }
        style.textContent = buildSkinCss();
    }

    // Keep behavior deterministic in embedded panel:
    // follow EquityChart approach (container-driven resize) and avoid
    // SVG self-autoscale transforms that can drift content out of viewport.
    function resizeSvg() {
        var iframe = getChartIframe();
        if (!iframe || !iframe.contentWindow) return;
        var win = iframe.contentWindow;
        renderStaticYAxis(iframe);
        if (typeof win._stickyScroll === 'function') win._stickyScroll();
    }

    function disableSvgAutoScale(iframe) {
        if (!iframe || !iframe.contentWindow || !iframe.contentDocument) return;
        var win = iframe.contentWindow;
        var doc = iframe.contentDocument;

        if (typeof win._autoScale === 'function') {
            win.removeEventListener('scroll', win._autoScale);
            win.removeEventListener('resize', win._autoScale);
            win._autoScale = function() {};
        }

        var cg = doc.getElementById('chartG');
        if (cg) cg.setAttribute('transform', 'matrix(1,0,0,1,0,0)');

        var stats = doc.querySelectorAll('.sStats');
        for (var i = 0; i < stats.length; i++) stats[i].removeAttribute('transform');

        var vpTexts = doc.querySelectorAll('#chartG text[data-oy]');
        for (var j = 0; j < vpTexts.length; j++) vpTexts[j].removeAttribute('transform');

        var tpoTexts = doc.querySelectorAll('#tpoTextG text[data-by]');
        for (var k = 0; k < tpoTexts.length; k++) {
            var t = tpoTexts[k];
            var by = parseFloat(t.dataset.by) || 0;
            var off = parseFloat(t.dataset.off) || 0;
            t.setAttribute('y', by + off);
        }

        renderStaticYAxis(iframe);
    }

    function renderStaticYAxis(iframe) {
        if (!iframe || !iframe.contentWindow || !iframe.contentDocument) return;
        var win = iframe.contentWindow;
        var doc = iframe.contentDocument;
        var ya = doc.getElementById('yAxisG');
        if (!ya) return;

        if (typeof win._mt !== 'number' || typeof win._ph !== 'number' ||
            typeof win._gL !== 'number' || typeof win._gR !== 'number' ||
            typeof win._mL !== 'number' || win._gR <= 0) {
            return;
        }

        var ns = 'http://www.w3.org/2000/svg';
        var nT = 10;
        var yFill = win._yFill || '#555';
        var yStroke = win._yStroke || '#888';

        ya.innerHTML = '';
        for (var i = 0; i <= nT; i++) {
            var p = win._gL + (win._gR * i / nT);
            var yy = win._mt + win._ph - (win._ph * i / nT);

            var ln = doc.createElementNS(ns, 'line');
            ln.setAttribute('x1', win._mL);
            ln.setAttribute('x2', win._mL + 6);
            ln.setAttribute('y1', yy);
            ln.setAttribute('y2', yy);
            ln.setAttribute('stroke', yStroke);
            ya.appendChild(ln);

            var tx = doc.createElementNS(ns, 'text');
            tx.setAttribute('x', win._mL - 3);
            tx.setAttribute('y', yy + 3);
            tx.setAttribute('font-family', 'Monospace');
            tx.setAttribute('font-size', '9');
            tx.setAttribute('fill', yFill);
            tx.setAttribute('text-anchor', 'end');
            tx.textContent = p.toFixed(5);
            ya.appendChild(tx);
        }
    }

    function onIframeLoaded() {
        $timeout(function() { $scope.svgLoading = false; }, 0);
        applyIframeStyles();
        var iframe = getChartIframe();
        if (iframe && iframe.contentDocument && iframe.contentWindow) {
            var svg = iframe.contentDocument.querySelector('svg');
            if (svg) {
                svg.setAttribute('preserveAspectRatio', 'xMinYMin meet');
            }
            disableSvgAutoScale(iframe);
            iframe.contentWindow.scrollTo(iframe.contentWindow.scrollX || 0, 0);
        }
        resizeSvg();
    }

    // Fetch SVG from server, wrap it in HTML, and set as iframe srcdoc.
    function loadChartIntoIframe(url) {
        $scope.svgLoading = true;
        $http.get(url, { transformResponse: angular.identity }).then(function(response) {
            var iframe = getChartIframe();
            if (!iframe) { $scope.svgLoading = false; return; }
            iframe.srcdoc = buildHtmlWrapper(response.data);
            // svgLoading cleared in onIframeLoaded after iframe fires its load event
        }, function() {
            $scope.svgLoading = false;
        });
    }

    // After Angular renders the iframe element: set up ResizeObserver, load listener, then fetch SVG.
    function setupIframe() {
        $timeout(function() {
            var iframe = getChartIframe();
            if (!iframe) return;
            if (resizeObserver) resizeObserver.disconnect();
            if (typeof ResizeObserver !== 'undefined') {
                resizeObserver = new ResizeObserver(function() { resizeSvg(); });
                resizeObserver.observe(iframe);
            }
            if (iframeLoadHandler) iframe.removeEventListener('load', iframeLoadHandler);
            iframeLoadHandler = onIframeLoaded;
            iframe.addEventListener('load', iframeLoadHandler);
            loadChartIntoIframe($scope.chartUrl);
        }, 0);
    }

    function mergeParams() {
        var dataInfo = SQResultsData.getDataInfo();
        var params = {
            projectName: dataInfo.projectName,
            databankName: dataInfo.databankName,
            strategyName: dataInfo.strategyName
        };
        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) {
            params.backtestID = dataInfo.backtestID;
        }
        if (dataInfo.isAlgoWizardCloud) {
            params.nodeID = dataInfo.nodeID;
            params.isAlgoWizardCloud = dataInfo.isAlgoWizardCloud;
        }
        return params;
    }

    function loadPaths() {
        if (!$scope.tab || !$scope.tab.active) return;
        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
            $scope.chartPaths = [];
            $scope.selectedPath = null;
            $scope.chartUrl = null;
            loadedContextKey = null;
            return;
        }
        var params = mergeParams();
        var contextKey = JSON.stringify(params);
        if (loadedContextKey !== null && loadedContextKey !== contextKey) {
            // Strategy context changed: drop previous path before loading new list.
            $scope.selectedPath = null;
            $scope.chartUrl = null;
        }
        if (loadedContextKey === contextKey) {
            return;
        }

        $scope.loading = true;
        loadedContextKey = contextKey;

        ProfileChartService.getPaths(params, function(response) {
            if (contextKey !== loadedContextKey) return;
            $scope.loading = false;
            if (response && response.success && response.profileChartPaths) {
                var newPaths = response.profileChartPaths || [];
                $scope.chartPaths = newPaths;
                $scope.baseUrl = response.baseUrl || '';
                if (newPaths.length > 0) {
                    // Keep it deterministic: for every new strategy context pick first path.
                    $scope.selectedPath = newPaths[0];
                    $scope.updateChartUrl($scope.selectedPath);
                } else {
                    $scope.selectedPath = null;
                    $scope.chartUrl = null;
                }
            } else {
                $scope.chartPaths = [];
                $scope.selectedPath = null;
                $scope.chartUrl = null;
            }
            try { $scope.$digest(); } catch (err) { }
        });
    }

    function refreshContent(strategyChanged, handlerContent) {
        if (strategyChanged) {
            // Clear stale selection immediately when strategy context changes.
            loadedContextKey = null;
            $scope.chartPaths = [];
            $scope.selectedPath = null;
            $scope.chartUrl = null;
        }
        loadPaths();
    }

    $scope.updateChartUrl = function(pathOverride) {
        if (pathOverride) {
            $scope.selectedPath = pathOverride;
        }

        if ($scope.chartPaths && $scope.chartPaths.length > 0 &&
            $scope.selectedPath && $scope.chartPaths.indexOf($scope.selectedPath) < 0) {
            $scope.selectedPath = $scope.chartPaths[0];
        }

        if ($scope.baseUrl && $scope.selectedPath) {
            $scope.chartUrl = $scope.baseUrl + encodeURIComponent($scope.selectedPath) + '?_ts=' + Date.now();
            setupIframe();
        } else {
            $scope.chartUrl = null;
        }
    };

    // Re-inject theme CSS whenever the skin changes, without reloading the iframe.
    $scope.$watch(function() {
        return SkinService.selectedSkin && SkinService.selectedSkin.name;
    }, function(newVal, oldVal) {
        if (newVal !== oldVal) applyIframeStyles();
    });

    $scope.tab = $scope.tab || {};

    var handlerId = 'ProfileChartCtrl';
    SQResultsData.addHandler(handlerId, null, refreshContent, []);

    $scope.tab.tabChanged = function() {
        if ($scope.tab.active) {
            loadPaths();
        }
    };

    if ($scope.tab.active) {
        loadPaths();
    }
});
