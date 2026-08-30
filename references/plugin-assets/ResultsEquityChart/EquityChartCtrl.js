angular.module('app.resultstabs.equitychart').controller('EquityChartCtrl', function ($rootScope, $scope, EquityChartService, SQEvents, $timeout, $window, SkinService, SQConstants, SQResultsData, $q, $element, L, BackendService) {
    console.log("EquityChart controller initialized");

    $scope.sampleTypes = SQConstants.getConstants().sampleTypes;

    $scope.isAlgoWizardCloud = SQResultsData.isAlgoWizardCloud();

    var chart = null;
    var zoomwidth = -1;
    var zoomminX = -1;
    var zoommaxX = -1;
    var lastWidth = -1;
    var lastXaxis = null;

    var strategySelected = false;

    $scope.drawdownTypes = {
        "Money": 10,
        "Pct": 20,
        "Pips": 30,
        "Off": 0
    }

    $scope.volumeTypes = {
        "Auto": '5',
        "On": '1',
        "Off": '0',
    }

    $scope.stagnationTypes = {
        "Full": $scope.sampleTypes.full,
        "IS": $scope.sampleTypes.in,
        "OOS": $scope.sampleTypes.out,
        "Off": 0,
    }

    $scope.pointsTypes = {
        "All": "all",
        "Grow": "grow",
        "Off": "off",
    }

    $scope.equityTypes = {
        "Daily": "daily",
        "MAE/MFE": "maemfe",
        "Off": "off",
    }

    $scope.isMTAnalyzer = isMTAnalyzer();

    $scope.resizeTimer;

    $scope.filter = {};
    $scope.filter.xaxis = 'trade';
    $scope.filter.sampleType = $scope.sampleTypes.full;
    $scope.filter.drawdown = '10';
    $scope.filter.volume = '5';
    $scope.filter.stagnation = 0;
    $scope.filter.maxNewHighDuration = 0;

    $scope.filter.crosshair = false;
	
    $scope.filter.yearmarker = false;
    $scope.filter.trendline = false;
    $scope.filter.points = 'off';
    $scope.filter.history = false;
    $scope.filter.equity = 'daily';

    $scope.filter.dailychart = false;
    $scope.filter.volatility = false;

    $scope.benchmarkSymbols = [{key: 'SPY_benchmark.D', name: L.tsq('SPY (S&P 500)')}];
    $scope.benchmarkNormalizations = [{key: 'off', name: L.tsq('off')}, 
                                      {key: 'drawdown', name: L.tsq('normalize by $ drawdown')},
                                      {key: 'drawdownPct', name: L.tsq('normalize by % drawdown')},
                                      {key: 'moneyManagement', name: L.tsq('normalize by money management')},  
                                      {key: 'exposure', name: L.tsq('normalize by exposure')}];
    
    $scope.filter.benchmarkActive = false;
    $scope.filter.benchmarkSymbol = $scope.benchmarkSymbols.length > 0 ? $scope.benchmarkSymbols[0].key : 'NA';
    $scope.filter.benchmarkNormalization = $scope.benchmarkNormalizations.length > 0 ? $scope.benchmarkNormalizations[0].key : 'NA';

    $scope.benchmarkTable = {};

    $scope.filter.isSP = false;
    $scope.portfolio = false;

    $scope.benchmarkSymbolLabel = function() {
        var item = getItem($scope.benchmarkSymbols, "key", $scope.filter.benchmarkSymbol);
        return item ? L.tsq(item.name) : $scope.filter.benchmarkSymbol;
    }

    $scope.benchmarkNormalizationLabel = function() {
        var item = getItem($scope.benchmarkNormalizations, "key", $scope.filter.benchmarkNormalization);
        return item ? L.tsq(item.name) : $scope.filter.benchmarkNormalization;
    }
    
    $scope.learMoreNormalization = function () {
        BackendService.sendRequest('/main/openlink', {
            url: 'https://strategyquant.com/benchmark-article'
        });
    }

    $scope.onFilterChange = function (strategyName, resultkey, direction, sampleType, wfStrategySelected) {
        $scope.strategyName = strategyName;
        $scope.filter.resultkey = resultkey;
        $scope.filter.direction = direction;
        $scope.filter.sampleType = sampleType;

        if($scope.filter.resultkey == 'Portfolio parts only'){
            $scope.filter.xaxis = 'time';
        }

        if (wfStrategySelected) {
            $scope.filter.xaxis = 'time';
        }

        strategySelected = true;

        refreshContent(false);

    }

    angular.element($window).bind('resize', function () {
        $timeout(function () {
            $timeout.cancel($scope.resizeTimer);
            $scope.resizeTimer = $timeout(function () {
                $scope.doneResizing();
            }, 300, false);
        }, 0, false);
    });

    $scope.doneResizing = function () {
        if (strategySelected) {
            strategySelected = false;

            return;
        }

        refreshContent(false, null, null, true);
    } 

    $scope.print = function (callback) {
        try { $scope.$digest(); } catch (err) { }

        //was changed xAxis type? in this case - reset zoom
        if (lastXaxis!=$scope.filter.xaxis){
            zoomwidth = -1;
            zoomminX = -1;
            zoommaxX = -1;       
        }

        $timeout(function () {
            refreshContent(false, null, callback);
        }, 0, false);
    }

    function reset() {
        chart.setData(null);
    }

    function refreshContent(strategyChanged, handlerContent, callback, onresize) {
        //console.error("equity chart refreshContent",strategyChanged, handlerContent);
		chart.setCursorCrossMode($scope.filter.crosshair);
		
        if (strategyChanged) {
            zoomwidth = -1;
            zoomminX = -1;
            zoommaxX = -1;
            lastWidth = -1;

            strategyWasChanged = true;
            updateSettings(handlerContent);

            var args = handlerContent ? {
                resultKey: $scope.filter.resultkey,
                direction: $scope.filter.direction,
                sampleType: $scope.filter.sampleType
            } : null;

            $scope.toolbar.refresh(args);
            return; //data will be fetched during next function call from onFilterChange method
        }

        if (!$scope.tab.active){
            return;
        }

        if (!callback) {
            chart.updateSizes();
        }

        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
            reset();
            return;
        }

        var currentWidth = getWidth();
        if (onresize && currentWidth==lastWidth){
            console.debug('Skipping reload equity chart due vertical resize.');
            return;
        }

        if($scope.filter.resultkey == null){
            return;
        }

        var params = [
            $scope.filter.resultkey,
            $scope.filter.direction,
            $scope.filter.sampleType,
            $scope.filter.xaxis,
            $scope.filter.drawdown,
            $scope.filter.volume,
            $scope.filter.stagnation,
            $scope.filter.maxNewHighDuration,
            $scope.filter.yearmarker,
            $scope.filter.trendline,
            $scope.filter.points,
            $scope.filter.history,
            $scope.filter.equity,
            currentWidth,
            getHeight(),
            //callback ? true : false,
            strategyWasChanged? false: true,
            zoomwidth,
            zoomminX,
            zoommaxX,
            $scope.filter.dailychart,
            $scope.filter.volatility,
            $scope.filter.benchmarkActive,
            $scope.filter.benchmarkSymbol,
            $scope.filter.benchmarkNormalization
        ];

        lastXaxis = $scope.filter.xaxis;
        SQResultsData.getData(handlerId, params).then(function (result) {
            if (!result.changed && !strategyWasChanged) return;

            strategyWasChanged = false;

            if (result.error) {
                reset();
                return;
            }

            lastWidth = currentWidth;
            if (callback) {
                callback(result.data);
            } else {
                render(result.data);
            }
        });
    }

    function updateSettings(handlerContent) {
        if (!handlerContent || !handlerContent.projectName) return;

        var attributes = handlerContent.data.attributes;
        $scope.filter.resultkey = attributes.resultKey;
        $scope.filter.direction = attributes.direction;
        $scope.filter.sampleType = attributes.sampleType;
        $scope.filter.xaxis = attributes.xaxis;
        $scope.filter.drawdown = attributes.drawdown;
        $scope.filter.volume = attributes.volume;
        $scope.filter.stagnation = attributes.stagnation;
        $scope.filter.maxNewHighDuration = attributes.maxNewHighDuration;
        $scope.filter.yearmarker = attributes.yearmarker;
        $scope.filter.trendline = attributes.trendline;
        $scope.filter.points = attributes.points;
        $scope.filter.history = attributes.history;
        $scope.filter.equity = attributes.equity;
        zoomwidth = attributes.zoomwidth;
        zoomminX = attributes.zoomminX;
        zoommaxX = attributes.zoommaxX;
        $scope.filter.dailychart = attributes.dailychart;
        $scope.filter.volatility = attributes.volatility;
        $scope.filter.benchmarkActive = attributes.benchmarkActive;
        $scope.filter.benchmarkSymbol = attributes.benchmarkSymbol;
        $scope.filter.benchmarkNormalization = attributes.benchmarkNormalization;
    }

    function fetchData(params) {
        var deferred = $q.defer();

        EquityChartService.print(params, function (result) {
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    function getWidth() {
        return chart.getChartWidth();
    }

    function getHeight() {
        return chart.getChartHeight();
    }

    var render = function (data) {
        //enable/disable X Axis Time
        $scope.portfolio = data.portfolio;
        $scope.filter.isSP = data.stockPick;
        
        chart.setData(data.chart);

        if($scope.benchmarkTable.print && data.chart && data.chart.benchmark) {
            $scope.benchmarkTable.print(data.chart.benchmark.data);
        }
    }

    function initChart() {
        var config = {
            canvasId: 'equitychart',
            logLevel: 'error',
            width: '100%',
            height: '100%',
            handleResize: false,
            tooltip: {
                show: true
            },
            dataLoader: function (ctx) {
                if ($(".dropdown-parameters-equity").hasClass("open")) {
                    $(".dropdown-parameters-equity").removeClass("open");
                    return;
                }

                zoomwidth = ctx.width;
                zoomminX = ctx.minX;
                zoommaxX = ctx.maxX;

                $timeout.cancel($scope.resizeTimer);
                $scope.resizeTimer = $timeout(function () {
                    $scope.print(function (data) {
                        ctx.dataLoadedHandler(data.chart);
                    });
                }, 300, false);

            }
        };

        chart = new SQ4.EquityChart(config);
        fixChartSizes();

        updateChartSkin();
    }

    function fixChartSizes(updateOnDelay){
        var wrapper = document.getElementById('equitychart').parentElement; 
        var observer = new ResizeObserver(function(entries) {
            var entry = entries[0];
            if (chart && entry.contentRect.width > 0 && entry.contentRect.height > 0) {
                console.log('Canvas size updated: ',entry.contentRect.height );
                observer.disconnect();

                if (updateOnDelay){
                    setTimeout(function(){chart.updateSizes();}, 200);                    
                }
                
                chart.updateSizes();
            }
        });
        observer.observe(wrapper);
    }

    $scope.refreshChart = function(){        
        chart.updateSizes();
        $scope.refreshing = true;
            $timeout(function() {
                $scope.refreshing = false;
            }, 1000);
    }

    function printKey(types, type) {
        for (var key in types) {
            if (types[key] == type) {
                return key;
            }
        }

        return null;
    }

    function updateChartSkin() {
        if (SkinService.selectedSkin.name == "Dark skin" && chart.switchToDark) {
            chart.switchToDark();
        }
        else if (chart.switchToLight) {
            chart.switchToLight();
        }
    }

    function init() {
        initChart();

        loadLastSettings();
    }

    function loadLastSettings() {
        var data = InitializationData().results.equityChart.lastSettings

        if (!data.equity) return; //no last settings saved

        $scope.filter.xaxis = data.xAxisType;
        $scope.filter.drawdown = data.drawdown;
        $scope.filter.volume = data.volume;
        $scope.filter.stagnation = parseInt(data.stagnation);
        $scope.filter.maxNewHighDuration = parseInt(data.maxNewHighDuration);

        $scope.filter.yearmarker = data.yearmarker;
        $scope.filter.trendline = data.trendline;
        $scope.filter.points = data.points;
        $scope.filter.equity = data.equity;

        $scope.filter.dailychart = data.dailychart;
        $scope.filter.volatility = data.volatility;

        $scope.filter.benchmarkActive = data.benchmarkActive;    
        $scope.filter.benchmarkSymbol = data.benchmarkSymbol;
        $scope.filter.benchmarkNormalization = data.benchmarkNormalization;

        try { $scope.$digest(); } catch (err) { }
    }

    var destroyConfigWatch = $scope.$watch("filter", function (newValue, oldValue) {
        if (newValue.benchmarkSymbol!=oldValue.benchmarkSymbol && newValue.benchmarkActive){
            $scope.print();
        }
    }, true);

    $scope.$on('$destroy', function () {
        if (!isMainApp() && !isAlgoWizard()) {
            window.parent.removeListener("EquityChart");
        }

        destroyConfigWatch();        
    });

    init();

    $scope.strategyName = null; //the name of the selected strategy
    $scope.toolbar = {};
    $scope.refreshing = false;

    var strategyWasChanged = false;

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['resultKey',
        'direction',
        'sampleType',
        'xaxis',
        'drawdown',
        'volume',
        'stagnation',
        'maxNewHighDuration',
        'yearmarker',
        'trendline',
        'points',
        'history',
        'equity',
        'width',
        'height',
        'zoom',
        'zoomwidth',
        'zoomminX',
        'zoommaxX',
        'dailychart',
        'volatility',
        'benchmarkActive',
        'benchmarkSymbol',
        'benchmarkNormalization'
    ]);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () { 
        watchersHandler.onActivated(false); 
        SQResultsData.resultsTabLoaded("EquityChartCtrl");
    }, 0, false);

    if (!isMainApp() && !isAlgoWizard()) {
        window.parent.addListener("EquityChart", [
            SQEvents.get("SKIN_CHANGED")
        ], updateChartSkin);
    }
    if (isAlgoWizard()) {
        SQEvents.addListener("EquityChart", [
            SQEvents.get("SKIN_CHANGED")
        ], updateChartSkin);
    }

    $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
        fixChartSizes(true);
    }

});