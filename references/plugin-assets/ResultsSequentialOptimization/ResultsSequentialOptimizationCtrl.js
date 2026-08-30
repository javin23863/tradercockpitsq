angular.module('app.resultstabs.sequentialoptimization').controller('ResultsSequentialOptimizationCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, SQConstants, SQResultsData, $element, L, BackendService) {
    console.log("Results Sequential optimization controller initialized");

    function init() {   
        initGrid();
    }

    function initGrid(){
        grid = new sqGrid("sequentialOptimizationParametersGrid");

        var columns = [
            { title: L.tsq('Parameter'), type: "text", sort: 'text' },
            { title: L.tsq('Orig. value'), type: "number", sort: 'number' },
            { title: L.tsq('Optim. value'), type: "number", sort: 'number' },
            { title: L.tsq('Found stable area'), type: "number", sort: 'number' }
        ];

        var widths = [ '*', 100, 100, 140];

        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No params available.'));
        grid.disableSorting();

        grid.headerRedraw();

        $('#sequentialOptimizationParametersGrid').addClass('gridbox_compressed');
    }

    $scope.applyOptimizedValues = function(){
        var dataInfo = SQResultsData.getDataInfo();
        let data = {
            projectName: dataInfo.projectName,
            databankName: dataInfo.databankName,
            strategyName: dataInfo.strategyName
        };
        
        BackendService.sendRequest("sequentialoptimization/applyParams", data, function(result){
            if(result.success){
                $rootScope.showSuccess(result.success);
            }
        });
    }

    function print(data){
        //console.info("Chain optimization print", data);

        grid.removeAllRows(true, true, true);
        $scope.instances.length = 0;

        if(data.data.results){
            $scope.parametersApplied = data.data.parametersApplied;
            $scope.passed = data.data.passed;

            for(let i=0; i<data.data.results.length; i++){
                let result = data.data.results[i];

                let lastCol = result.stableAreaFound ? "<label style='color:green; font-weight:550;'>yes</label>" : "<label style='color:red; font-weight:550;'>no</label>" 
                grid.addRow([result.parameterName, result.originalValue, result.optimizedValue, lastCol], true);

                $scope.instances.push({ 
                    data : result.chart, 
                    parameterName: result.parameterName, 
                    stableAreaFound : result.stableAreaFound  
                });
            }
        }

        grid.debouncedBodyRedrawWithResort();
    }

    function refreshContent(strategyChanged, handlerContent) {
        if (!$scope.tab.active) return;

        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
            return;
        }

        SQResultsData.getData(handlerId, []).then(function (result) {
            print(result);
            $timeout(function () {
                window.dispatchEvent(new Event('resize'));
            }, 200, false);
        });        
    }

    function fetchData(params) {
        let deferred = $q.defer();
        BackendService.getPromise('sequentialoptimization/print', params, isMTAnalyzer() ? 'GET' : 'POST', true).then(function(result){
            deferred.resolve(result.data);
        });

        return deferred.promise;
    }

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var grid;

    $scope.instances = [];
    $scope.parametersApplied = false;
    $scope.passed = false;

    init();

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
    }, 0, false);

    $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
    }

});