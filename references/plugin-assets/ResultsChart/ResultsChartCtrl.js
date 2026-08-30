angular.module('app.resultstabs.chart').controller('ResultsChartCtrl', function ($rootScope, $scope, $timeout, $q, SQEvents, ResultsChartService, SkinService, usSpinnerService, SQResultsData) {

    function init() {
        $timeout(function () {
            chart = new SQ4.StockChart(getChartConfig());
            chart.setCursorCrossMode(true);
            chart.setGridVisible(true);
            chart.updateSizes();

            updateChartSkin();
        }, 0, false);
    }

    function getChartConfig() {
        var config = {
            canvasId: 'canvasWrapper',
            logLevel: 'error',
            width: chartWidth,
            height: "100%",
            tooltip: {
                show: true
            },
            zoom: {
                external: true
            },
            dataLoader: function (ctx) {
                indexFrom = ctx.minX;
                indexTo = ctx.maxX;
				loadTrade = ctx.trade;
				zoomWidth = ctx.zoomWidth;
                
                refreshContent(false, null, ctx.dataLoadedHandler, true);
            },
            tooltipHandler: function(data){
                if(!loadingData){
                    loadingData = true;

                    setTimeout(function(){
                        loadingData = false;
                        refreshDataWindow(data);
                    }, 100);
                }
            },
			tradeSelectedHandler:function(selectedTrade){
				$scope.selectedTrade = selectedTrade;
				try { $scope.$digest(); } catch (err) { }
			}
        };
        return config;
    }

    function fetchData(params) {
        if (!params) return null;
        return ResultsChartService.loadChartData(params);
    }

    $scope.onSymbolFilterChange = function() {
        indexFrom = null;
        indexTo = null;

        refreshContent(false, null, null, false);
    }

    function refreshContent(strategyChanged, handlerContent, handler, ignorePreview) {
        $scope.noData = false;

        if (strategyChanged) {
            indicatorsLoaded = false;

            var filteredDataItems = [];
            var allItems = SQResultsData.getDataInfo().dataItems;
            for (var i = 0; i < allItems.length; i++) {
                var item = allItems[i];
                if (item !== 'Portfolio') {
                    filteredDataItems.push(item);
                }
            }
            $scope.dataItems = filteredDataItems;

			loadTrade = null;
            indexFrom = null;
            indexTo = null;

            $scope.selectedSymbol = null;

            dataDeferred = $q.defer();
        }
        else {
            if ($scope.tab.switchData) {
                goToTrade($scope.tab.switchData);
                $scope.tab.switchData = null;
            }
        }

        if (!$scope.tab.active) return;

        var preview = ignorePreview === true ? false : true;
        lastRequestId = Math.floor(Math.random() * 10000000)+''; 
        
		console.log('loading chart:'+indexFrom+', '+indexTo);
        SQResultsData.getData(handlerId, [indexFrom, indexTo, preview, lastRequestId, zoomWidth, loadTrade,$scope.selectedSymbol ]).then(function (result) {	
			zoomWidth = null;
            if (loadTrade!=null && !$scope.emptySymbols){
                chart.highlightTrade(loadTrade);
            }

			loadTrade = null;

            if (!$scope.noData && (!result.changed || !result.data)) return;

			result = result.data;
            if (result.error){
                $rootScope.showError(result.error);
                return;
            }

			if (typeof result.data.requestId!=='undefined' && lastRequestId!==result.data.requestId){
				return;
			}
			
			indexFrom = result.data.chart.area_from;
			indexTo = result.data.chart.area_to;
			
            resetChartData();

            $scope.loading = true;

            if (result.success) {				
                indicatorsLoaded = true;
                loadIndicators(result.data.chart);

                if (handler) {
                    handler(result.data.chart);
                }
                else {
                    chart.setData(result.data.chart, strategyChanged);
                    chart.setAllowedIndicators(getSelectedIndicators());
                    chart.updateSizes();
                }

                var symbolsFromServer = result.data.chart.stocks;
                $scope.symbols = symbolsFromServer;
                $scope.emptySymbols = !symbolsFromServer || symbolsFromServer.length==0;
                $scope.selectedSymbol = result.data.chart.currentStock;
            }
            else {
                $scope.noData = true;
                $scope.emptySymbols = true;
            }

            $scope.hideSpinner();
            $scope.loading = false;



            try { $scope.$digest(); } catch (err) { }

            dataDeferred.resolve();
        });
    }

    $scope.showGrid = true;

    $scope.onShowGridChange = function () {
        $scope.showGrid = !$scope.showGrid;

        chart.setGridVisible($scope.showGrid);
        chart.refresh();
    }
	
	$scope.switchTradeValueVisibility = function(){
		$scope.tradesCloseValueVisible = !$scope.tradesCloseValueVisible;
		chart.setTradesOpenCloseValueVisible($scope.tradesCloseValueVisible);
	}
	
	$scope.switchTradeTicketVisibility = function(){
		$scope.tradesCloseTicketVisible = !$scope.tradesCloseTicketVisible;
		chart.setTradesCloseTicketVisible($scope.tradesCloseTicketVisible);
	}

	$scope.switchTradePLVisibility = function(){
		$scope.tradesClosePLVisible = !$scope.tradesClosePLVisible;
		chart.setTradesClosePLVisible($scope.tradesClosePLVisible);
	}	

    $scope.onZoomIn = function(){
        chart.performZoomIn();
		updateFromTo();
    }

    $scope.onZoomOut = function(){
        chart.performZoomOut();
		updateFromTo();
    }

    $scope.onResetZoom = function(){
        chart.performZoomReset();
		updateFromTo();
    }
	
	$scope.onPreviousTrade = function(){
		dataDeferred.promise.then(function () {
			chart.zoomToPrevTrade();
		});
	}
	
	$scope.onNextTrade = function(){
		dataDeferred.promise.then(function () {
			chart.zoomToNextTrade();
		});
	}
	
	function updateFromTo(){
		var region = chart.getCurrentDataRegion();
		indexFrom = region.min;
		indexTo = region.max;	
	}

    function goToTrade(switchData) {
        dataDeferred.promise.then(function () {
            if (!$scope.emptySymbols){
                $scope.selectedSymbol = switchData.symbol;
                indexFrom = null;
                indexTo = null;
                loadTrade = switchData.tradeTicket;
                refreshContent(false, null, null, false);
            }else{
                chart.zoomToTrade(switchData.tradeTicket, switchData.openTime, switchData.closeTime);
            }
        });
    }

    function resetChartData() {
        $scope.chartData.length = 0;
		$scope.selectedTrade=null;

        var chart = {
            title: "NA",
            dateTime: "NA",
            indicators: [],
            trades: [],
            barData: {
                open: "NA",
                high: "NA",
                low: "NA",
                close: "NA"
            }
        };

        $scope.chartData.push(chart);
    }

    function refreshDataWindow(data) {
        if (!data || !data.groups || !data.groups.length) return;

        $scope.chartData.length = 0;

        for (var i = 0; i < data.groups.length; i++) {
            var group = data.groups[i];

            var chart = {
                title: group.title,
                dateTime: group.dateTime,
                indicators: [],
                trades: []
            };

            for (var a = 0; a < group.items.length; a++) {
                var item = group.items[a];

                if (item.kind == "chart") {
                    chart.barData = item.value;
                }
                else if (item.kind == "trade") {
                    chart.trades.push(item.value);
                }
                else if (item.kind == "indicator") {
                    if (item.line == 'Value') {
                        chart.indicators.push({
                            name: item.subtitle,
                            singleValue: true,
                            value: item.value
                        });
                    }
                    else {
                        if (!indicatorLabelExists(chart.indicators, item.subtitle)) {
                            chart.indicators.push({
                                name: item.subtitle,
                                isLabel: true
                            });
                        }

                        chart.indicators.push({
                            name: item.line,
                            value: item.value
                        });
                    }
                }
            }

            $scope.chartData.push(chart);
        }

        try { $scope.$digest(); } catch (err) { }
    }

    function indicatorLabelExists(indicators, label) {
        for (var i = 0; i < indicators.length; i++) {
            var indicator = indicators[i];

            if (indicator.name == label && indicator.isLabel) {
                return true;
            }
        }
        return false;
    }

    function loadIndicators(data) {
        $scope.indicators.length = 0;
        var separateIndysShown = 0;

        for (var i = 0; i < data.indicators.length; i++) {
            var indicator = data.indicators[i];

            var isAtr = indicator.title.startsWith('ATR(') && indicator.title.endsWith(',14)');
            $scope.indicators.push({
                id: indicator.id,
                name: indicator.title,
                show: !isAtr && (indicator.displayOn == 'chart' || separateIndysShown++ < separateIndicatorsToShow)
            });
        }
    }

    function getSelectedIndicators() {
        var selectedIndys = [];

        for (var i = 0; i < $scope.indicators.length; i++) {
            var indicator = $scope.indicators[i];
            if (indicator.show) selectedIndys.push(indicator.id);
        }

        return selectedIndys;
    }

    function updateChartSkin(){
        if(SkinService.selectedSkin.name == "Dark skin" && chart.switchToDark){
            chart.switchToDark();
        }
        else if(chart.switchToLight){
            chart.switchToLight();
        }
    }

    $scope.showSpinner = function () {
        usSpinnerService.spin($scope.spinnerKey);
    }

    $scope.hideSpinner = function () {
        usSpinnerService.stop($scope.spinnerKey);
    }

    $scope.onIndicatorSelect = function () {
        chart.setAllowedIndicators(getSelectedIndicators());
    }

    $scope.$on('$destroy', function () {
        if(!isMainApp() && !isAlgoWizard()){
            window.parent.removeListener("StockChart");
        }
    });

    //- Initialization --------------------------------------------------------------

    var chart;
    var chartWidth = "100%";
    var indexFrom, indexTo, loadTrade, zoomWidth=null;
    var indicatorsLoaded = false;
    var separateIndicatorsToShow = 1;
    var dataDeferred = $q.defer();
    var lastRequestId = null;
    var loadingData = false;

    $scope.selectedSymbol = null;
    $scope.emptySymbols = true;
    $scope.spinnerKey = "chartsSpinner";
    $scope.loading = false;
    $scope.dataItems = [];
    $scope.noData = true;
	
	$scope.tradesCloseTicketVisible=true;
	$scope.tradesCloseValueVisible=true;
	$scope.tradesClosePLVisible=true;

    $scope.chartData = [];
	$scope.selectedTrade = null;
    $scope.indicators = [];

    init();

    var handlerId = $scope.tab.title; 	//handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['indexFrom', 'indexTo', 'preview','requestId','zoomWidth','trade','stock']);
    
    if(!isMainApp() && !isAlgoWizard()){
        window.parent.addListener("StockChart", [ SQEvents.get("SKIN_CHANGED") ], updateChartSkin);
    }

});