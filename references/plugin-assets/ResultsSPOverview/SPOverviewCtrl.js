angular.module('app.resultstabs.spoverview').controller('SPOverviewCtrl', function ($scope, SPOverviewService, SQEvents, $timeout, $window, SkinService, SQConstants, SQResultsData, $q, $element, L, BackendService) {
    console.log("SP Overview controller initialized");

    function init() {
        $timeout(function () {
            $scope.stats.init();
            $scope.treeMap.init();
            $scope.chartTop10Profit.init("#008000");
            $scope.chartTop10Loss.init("#E8383C");
            $scope.chartTop10Volume.init("#383CE8");
            $scope.grid.init();        
        }, 0, true);
    }
    
    $scope.onRefresh = function () {
        refreshContent(true);
    }

    $scope.print = function() {
        updateGridRows();
    }

    $scope.settingsChanged = function() {
        refreshContent(true);
    }

    //- Event Handlers --------------------------------------------------------------

    function refreshContent(force) {
        if (!$scope.tab.active) return;

        if (!loaded && !initCalled) {
            initCalled = true;
            init();
        }

        SQResultsData.getData(
            handlerId, 
            [$scope.config.showAll], 
            force
        ).then(function (result) {
            print(result);
        });
    }

    function print(result) {
        $scope.stats.print(result.data.stats);
        $scope.treeMap.print(result.data.treeMap);
        $scope.chartTop10Profit.print(result.data.top10Profit);
        $scope.chartTop10Loss.print(result.data.top10Loss);
        $scope.chartTop10Volume.print(result.data.top10Volume);
        $scope.grid.print(result.data.listAll);

        try { $scope.$digest(); } catch (err) { }
    }

    function fetchData(params) {
        var deferred = $q.defer();

        SPOverviewService.print(params, function (result) {
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    $scope.$on('$destroy', function () {
        destroyConfigWatch();

        if (window.parent && window.parent.removeListener) {
            window.parent.removeListener("SPOverviewCtrl");
        }

        SQResultsData.removeHandler(handlerId);
    });


    //- Initialization --------------------------------------------------------------

    var initCalled = false;
    var loaded = false;
     
    //components
    $scope.stats = {};
    $scope.treeMap = {};
    $scope.chartTop10Profit =   { title: L.tsq('TOP 10 STOCKS BY PROFIT') };
    $scope.chartTop10Loss =     { title: L.tsq('TOP 10 STOCKS BY LOSS')   };
    $scope.chartTop10Volume =   { title: L.tsq('TOP 10 STOCKS BY VOLUME') };
    $scope.grid = {};

    $scope.config = {
        showAll: false
    }

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    var handler = SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['showAll']);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function(){ watchersHandler.onActivated(false); }, 0, false);

    $scope.tab.tabChanged = function() {
       watchersHandler.onActivated($scope.tab.active);
    }
});