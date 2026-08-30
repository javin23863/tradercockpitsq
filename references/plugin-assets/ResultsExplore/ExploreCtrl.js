angular.module('app.resultstabs.explore').controller('ExploreCtrl', function ($scope, $timeout, $q, ExploreService, SQResultsData, $element, BackendService , SQEvents, L) {
    console.log("Explore controller initialized");

    $scope.filter = {
        symbol: '',
        dateFrom: "2022.01.01",
        dateTo: "2022.12.31",
        dateMin: "2022.01.01",
        dateMax: "2022.12.31"
    };

    $scope.symbols = []

    $scope.onDateFromChange = function (date) {
        $scope.filter.dateFrom = date;
        
        $scope.onFilterChange();
    }

    $scope.onDateToChange = function (date) {
        $scope.filter.dateTo = date;
        
        $scope.onFilterChange();
    }

    $scope.onFilterChange = function() {
        if(loading) {
            return;
        }

        loading = true;

        updateGridRows(function () {
            $timeout(SQResultsData.hideLoadingOverlay, 0, false);

            loading = false;
        });
    }

    function updateGridRows(callback) {
        if (!exploreGrid) {
            console.error("Trades grid not initialized");
            return;
        }

        var dataInfo = SQResultsData.getDataInfo();
        var projectName = dataInfo.projectName;
        var databankName = dataInfo.databankName;
        var strategyName = dataInfo.strategyName;

        var url = "explore/data?";
        url += "projectName=" + projectName + "&";
        url += "databankName=" + databankName + "&";
        url += "strategyName=" + strategyName + "&";

        url += "symbol=" + $scope.filter.symbol + "&";
        url += "dateFrom=" + $scope.filter.dateFrom + "&";
        url += "dateTo=" + $scope.filter.dateTo + "&";

        url += "token=" + BackendService.getToken() + "&";


        if (valueFilled($scope.resultkey)) {
            url += "resultKey=" + $scope.resultkey.replace("&", "amp") + "&";
        }
       
        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) {
            url += "backtestID=" + dataInfo.backtestID + "&";
        }

        if (dataInfo.isAlgoWizardCloud) {
            url += "nodeID=" + dataInfo.nodeID + "&";
            
            if(window.top.requestsPrefix){
                url = window.top.requestsPrefix + url;
            }
        }

        exploreGrid.setSmartRendering(encodeUriCustom(url), callback, true);
        exploreGrid.setSmartRenderingSorting();

        exploreGrid.debouncedBodyRedrawWithResort();
    }

    function initExploreGrid(customColumns) {
        var columns = [
            { title: L.tsq('Symbol'), type: "text", sort: 'text', width: 150 },
            { title: L.tsq('Date'), type: "text", sort: 'text', width: 200 },
        ];

        if(customColumns) {
            for(var i = 0; i < customColumns.length; i++) {
                columns.push({ title: customColumns[i], type: "text", sort: 'text', width: 150 });
            }
        }

        if (!exploreGrid) {
            exploreGrid = new sqGrid('exploreGrid');

            console.log("initializing explore grid", columns)

            exploreGrid.setFirstColumnAsId(true, false);
            exploreGrid.enableCheckboxes(false);
            exploreGrid.setColumns(columns);
            exploreGrid.setEmptyGridText(L.tsq('No data.'));
        } else {
            exploreGrid.removeAllRows(true);

            exploreGrid.setColumns(columns);
        }
    }

    //- Event Handlers --------------------------------------------------------------

    function refreshContent() {
        if (!$scope.tab.active) {
            return;
        } else {
            window.dispatchEvent(new Event('resize'));
        }
        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
            exploreGrid.removeAllRows();
            return;
        }

        if(loading) {
            return;
        }

        loading = true;

        SQResultsData.getData(handlerId, []).then(function (result) {
            if (!result.changed) {
                SQResultsData.hideLoadingOverlay();
                return;
            }

            SQResultsData.showLoadingOverlay();

            ExploreService.info(SQResultsData.getDataInfo(), function (data) {
                loadToArray($scope.symbols, data.symbols);

                if(data.symbols) {
                    if(!data.symbols.includes($scope.filter.symbol)) {
                        $scope.filter.symbol = data.symbols[0];
                    }
                }
                
                $scope.filter.dateFrom = data.dateFrom;
                $scope.filter.dateTo = data.dateTo;
                $scope.filter.dateMin = data.dateFrom;
                $scope.filter.dateMax = data.dateTo;

                initExploreGrid(data.columns);

                updateGridRows(function () {
                    $timeout(SQResultsData.hideLoadingOverlay, 0, false);

                    loading = false;
                });
            });
        });
    }

    function fetchData(params) {
        var deferred = $q.defer();

        deferred.resolve("no data");

        return deferred.promise;
    }
    
    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    $scope.toolbar = {};
    $scope.resultkey;
    $scope.direction;

    var exploreGrid;

    var loading = false;

    $timeout(initExploreGrid, 0, false);

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
        SQResultsData.resultsTabLoaded("ResultsTradelistCtrl");
    }, 0, false);

    $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
    }
});