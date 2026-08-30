angular.module('app.resultstabs.tradeanalysis').controller('TradeAnalysisCtrl', function($rootScope, $scope, TradeAnalysisService, SQEvents, $timeout, $q, SQResultsData, $element, L) {
    console.log("Trade analysis controller initialized");

    $scope.filter = {};
    $scope.filter.period = 1;
    $scope.filter.year = 'All';

    $scope.instances = [];
    $scope.types = [];

    $scope.instanceFixedChart = {};

    var annualStatsGrid;

    function init() {
        var data = InitializationData().results;

        loadToArray($scope.types, data.tradeAnalysis.types);

        var charts = data.tradeAnalysis.lastSettings;

        for (var i = 0; i < 12; i++) {
            var instance = {
                number: i,
                onTypeSelect: print,
                selectedType: charts[i]
            };

            $scope.instances.push(instance);
        }

        $scope.instanceFixedChart = {
            'number': 10,
        };

        initAnnualStatsGrid();
    }

    $scope.onFilterChange = function(strategyName, resultKey, direction, sampleType) {
        $scope.filter.resultKey = resultKey;
        $scope.filter.direction = direction;
        $scope.filter.sampleType = sampleType;

        refreshContent();
    }

    function reset() {
        for (var i = 0; i < $scope.instances.length; i++) {
            $scope.instances[i].reset();
        }

        $scope.instanceFixedChart.reset();

        annualStatsGrid.removeAllRows();
    }

    function refreshContent(strategyChanged, handlerContent) {
        if (strategyChanged) {
            updateSettings(handlerContent);

            var args = handlerContent ? {
                resultKey: $scope.filter.resultkey,
                direction: $scope.filter.direction,
                sampleType: $scope.filter.sampleType,
            } : null;

            $scope.toolbar.refresh(args);
            return; //data will be fetched during next function call from onFilterChange method
        }

        if (!$scope.tab.active) return;

        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
            reset();
            return;
        }

        SQResultsData.getData(handlerId, [$scope.filter.resultKey, $scope.filter.direction, $scope.filter.sampleType, $scope.filter.period, $scope.filter.year]).then(function(result) {
            print();
        });
    }

    function updateSettings(handlerContent) {
        if (!handlerContent || !handlerContent.projectName) {
            $scope.filter.year = 'All';
            return;
        }

        var attributes = handlerContent.data.attributes;
        $scope.filter.resultkey = attributes.resultKey;
        $scope.filter.direction = attributes.direction;
        $scope.filter.sampleType = attributes.sampleType;
        $scope.filter.period = attributes.period;
        $scope.filter.year = attributes.year;
    }

    function fetchData(params) {
        var deferred = $q.defer();

        $timeout(function() { deferred.resolve({}); }, 0, false);

        return deferred.promise;
    }

    $scope.print = function(callback) {
        refreshContent(false, null);
    }

    function print(dontReloadStats) {
        var dataInfo = SQResultsData.getDataInfo();

        var data = {};
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        angular.extend(data, $scope.filter);

        let charts = [];
        for (let i = 0; i < $scope.instances.length; i++) {
            charts.push($scope.instances[i].getSelectedType());
        }

        data.charts = charts.toString();

        TradeAnalysisService.print(data, function(res) {
            let resCharts = res.charts;

            for (let i = 0; i < resCharts.length; i++) {
                $scope.instances[i].print(resCharts[i]);
            }

            $scope.instanceFixedChart.print(res.fixedChart);
        });

        //print annual stats        
        if (!dontReloadStats) {
            annualStatsGrid.removeAllRows();

            if (!data.strategy) {
                return;
            }

            if (window.parent && window.top.nodeID) {
                if (window.top.nodeID == "null") {
                    return;
                }
                data.nodeID = window.top.nodeID;
            }

            var url = "tradeanalysis/getAnnualStats?" + objectToURL(data);
            
            annualStatsGrid.loadFromUrl(url, afterGridLoaded);
        }
    }

    function afterGridLoaded() {
        $timeout(function() {
            if (annualStatsGrid.getNumberOfRows() > 0) {
                if ($scope.filter.year == 'All') {
                    annualStatsGrid.selectRow(0);

                } else {
                    for (var i = 0; i < annualStatsGrid.getNumberOfRows(); i++) {
                        var year = annualStatsGrid.getCellValue(i, 0);

                        if (year == $scope.filter.year) {
                            annualStatsGrid.selectRow(i);
                            break;
                        }
                    }
                }
            }
        }, 100, false);
    }

    function initAnnualStatsGrid() {
        $timeout(function() {
            annualStatsGrid = new sqGrid("annualStatsGrid");

            var columns = [
                { title: L.tsq('Period'), type: "text", sort: 'text' },
                { title: L.tsq('Net Profit'), type: "text", sort: 'text' },
                { title: L.tsq('Profit Factor'), type: "text", sort: 'text' },
                { title: L.tsq('# of trades'), type: "text", sort: 'text' },
                { title: L.tsq('% Wins'), type: "text", sort: 'text' }
            ];

            var widths = ['*', 100, 100, 100, 100];

            annualStatsGrid.setColumns(columns, !!annualStatsGrid);
            annualStatsGrid.setWidths(widths, !!annualStatsGrid);
            annualStatsGrid.setEmptyGridText(L.tsq('No annual stats available.'));
            annualStatsGrid.disableSorting();

            annualStatsGrid.onCellClick = function(rowIndex, cellIndex) {
                $scope.filter.year = annualStatsGrid.getCellValue(rowIndex, 0);
                print(true);
            };

            $('#annualStatsGrid').addClass('gridbox_compressed');
        }, 0, false);
    }

    $scope.toolbar = {};

    init();

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['resultKey', 'direction', 'sampleType', 'period', 'year']);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function() {
        watchersHandler.onActivated(false);
        SQResultsData.resultsTabLoaded("TradeAnalysisCtrl");
    }, 0, false);

    $scope.tab.tabChanged = function() {
        watchersHandler.onActivated($scope.tab.active);
    }
});