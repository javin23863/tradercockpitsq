angular.module('app.data').controller('ReviewChartCtrl', function ($scope, $rootScope, $timeout, DMDataService) {
    console.log("ReviewChart controller initialized");

    $scope.loading = false;

    var chart;
    var chartWidth = "100%";
    var indexFrom, indexTo;
    var symbolInfo;
    $scope.tickData = false;

    function initBarData() {
        $scope.barData = {
            dateTime: "NA",
            open: "NA",
            high: "NA",
            low: "NA",
            close: "NA"
        };
    }

    function init() {
        $timeout(function(){
            chart = new SQ4.StockChart(getChartConfig());
            chart.setCursorCrossMode(true);
            chart.updateSizes();
        });
    }

    function getChartConfig() {
        var config = {
            canvasId: 'canvasWrapper',
            logLevel: 'error',
            width: chartWidth,
            height: "100%",
            tooltip : {
                show: true
            },
            dataLoader : function(ctx){
                indexFrom = ctx.minX;
                indexTo = ctx.maxX;

                printChart(ctx.dataLoadedHandler, false);
            },
            tooltipHandler: refreshDataWindow
        };
        return config;
    }

    function refreshDataWindow(data) {
        if(!data || !data.groups || !data.groups.length) return;
        
        var group = data.groups[0];
        var value = group.items[0].value;
        
        $scope.barData.dateTime = group.dateTime;
        $scope.barData.open = value.open;
        $scope.barData.high = value.high;
        $scope.barData.low = value.low;
        $scope.barData.close = value.close;
        
        try { $scope.$digest(); } catch(err){}
    }

    $scope.tab.print = function(symbolInfoParam) {
        symbolInfo = symbolInfoParam;

		indexFrom = -1;
        indexTo = -1;
        
        $scope.tickData = (symbolInfo.timeframe=='TICK');

        initBarData();

        if($scope.tickData) {
            return;
        } 

        printChart(undefined, true);
    } 
	
	$scope.tab.updateChart = function(){
		chart.updateSizes();	
	}

    function printChart(handler, loadPreview) {
        var params = {
            symbol: symbolInfo.symbol,
            timeframe: symbolInfo.timeframe,
            session: symbolInfo.session,
            indexFrom: indexFrom, 
            indexTo: indexTo,
			loadPreview: loadPreview
        }

        DMDataService.loadChartData(params, function(data) {
            if (handler) {
                handler(data.chart);
            } else{
                chart.setData(data.chart);
                chart.updateSizes();
            }
        });
    }

    init();
});