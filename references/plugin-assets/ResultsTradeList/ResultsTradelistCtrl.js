angular.module('app.resultstabs.tradelist').controller('ResultsTradelistCtrl', function ($rootScope, $scope, $timeout, $q, BackendService, SQEvents, TradelistViewsService, SQConstants, SQResultsData, $element, L, ManageViewsService) {
    console.log("Results tradelist controller initialized");

    $scope.isMTAnalyzer = isMTAnalyzer();
    $scope.isAlgoWizardCloud = isAlgoWizardCloud();

    $scope.filter = {};
    $scope.filter.expired = false;

    $scope.onFilterChange = function (strategyName, resultkey, direction, sampleType) {
        $scope.resultkey = resultkey;
        $scope.direction = direction;
        $scope.sampleType = sampleType;

        refreshContent();
    }

    $scope.onViewChange = function () {
        TradelistViewsService.changeView($scope.view.selected.name);
    }

    $scope.onManageViews = function () {
        showPopup('#manageViewsTradelist');
    }

    $scope.onExport = function () {
        $rootScope.showCSVXLSXExportDialog("TradelistExport", "saveTradelistToCSV", function (path, useComma) {
            var dataInfo = SQResultsData.getDataInfo();
            var projectName = dataInfo.projectName;
            var databankName = dataInfo.databankName;
            var strategyName = dataInfo.strategyName;

            var url = "tradelist/" + projectName + "/" + databankName + "/" + strategyName + "/export";
            
            if(window?.top?.isCloudVersion && window?.top?.requestsPrefix){
                url = window.top.requestsPrefix + "/" + url;
            }

            var params = {
                resultKey: $scope.resultkey,
                direction: $scope.direction,
                sampleType: $scope.sampleType,
                path: path,
                useComma: useComma
            }

            if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) {
                params.backtestID = dataInfo.backtestID;
            }

            BackendService.sendRequest(url, params, function () {
                $rootScope.showSuccess(L.tsq("Tradelist exported"));
            }, 'POST');
        });
    }

    $scope.print = function() {
        updateGridRows();
    }

    function updateGridRows(callback) {
        if (!tradesGrid) {
            console.error("Trades grid not initialized");
            return;
        }

        var dataInfo = SQResultsData.getDataInfo();
        var projectName = dataInfo.projectName;
        var databankName = dataInfo.databankName;
        var strategyName = dataInfo.strategyName;

        var url = "tradelist/" + projectName + "/" + databankName + "/" + strategyName + "/getTrades?";

        url += "token=" + BackendService.getToken() + "&";

        if (valueFilled($scope.resultkey)) {
            url += "resultKey=" + $scope.resultkey.replace("&", "amp") + "&";
        }
        if (valueFilled($scope.direction)) {
            url += "direction=" + $scope.direction + "&";
        }
        if (valueFilled($scope.sampleType)) {
            url += "sampleType=" + $scope.sampleType + "&";
        }

        url += "expired=" + $scope.filter.expired + "&";

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) {
            url += "backtestID=" + dataInfo.backtestID + "&";
        }

        if (dataInfo.isAlgoWizardCloud) {
            url += "nodeID=" + dataInfo.nodeID + "&";
            url += "viewXML=" + ManageViewsService.getViewXML($scope.view.selected) + "&"
            
            if(window.top.requestsPrefix){
                url = window.top.requestsPrefix + url;
            }
        }

        tradesGrid.setSmartRendering(encodeUriCustom(url), callback, true);
        tradesGrid.setSmartRenderingSorting();

        tradesGrid.debouncedBodyRedrawWithResort(true);
    }

    function initTradesGrid() {
        if (!tradesGrid) {
            tradesGrid = new sqGrid('tradesGrid');
        }
        else {
            tradesGrid.removeAllRows(true);
        }

        var columns = [];
        var columnsRaw = $scope.view.selected.columns;
        if (columnsRaw) {
            for (var i = 0; i < columnsRaw.length; i++) {
                let thisType = getColumnType(columnsRaw[i].type, SQConstants.getConstants().plTypes.money);
                if (thisType == "dollars") {
                    thisType = "dollarsColoured";
                }
                columns.push({
                    title: L.tsq(columnsRaw[i].entryName),
                    type: thisType + ((columnsRaw[i].fontBold) ? ",fontBold" : ""),
                    sort: getSortType(columnsRaw[i].type),
                    /* fontBold: columnsRaw[i].fontBold || false, */
                    width: columnsRaw[i].width || 150,
                });
            }
        }

        tradesGrid.setFirstColumnAsId(true, false);
        tradesGrid.enableCheckboxes(false);
        tradesGrid.setColumns(columns);
        tradesGrid.setEmptyGridText(L.tsq('No trades data.'));
        tradesGrid.onCellClick = function (selectedRowNumber, clickedCol, rowsData, e) {
            if (rowsData.length < 4 || rowsData[rowsData.length - 4] != 1) {
                return;
            }

            if (!$(e.target).hasClass("chart-data")) {
                return;
            }

            var ticket = rowsData[rowsData.length - 3];
            var openTime = rowsData[rowsData.length - 2];
            var closeTime = rowsData[rowsData.length - 1];
            var symbol = rowsData[1];

            $scope.$parent.instance.selectTab('tradesOnChart', {
                tradeTicket: ticket,
                symbol: symbol,
                openTime: openTime,
                closeTime: closeTime
            });
        };

        tradesGrid.onAfterDataGot = function () {
            for (var i = 0; i < tradesGrid.rows.length; i++) {
                let cl = tradesGrid.rows[i].cells.length;
                let chartData = tradesGrid.rows[i].cells[cl - 4];
                let sampleType = tradesGrid.rows[i].cells[cl - 5];

                if (chartData == 1) {
                    tradesGrid.rows[i].cellClass[0] = ["chart-data"];
                }
                else if (chartData == 2) {
                    tradesGrid.rows[i].cellClass[0] = ["no-chart-data"];
                }

                if (sampleType == 1) {
                    tradesGrid.rows[i].class = ["background-isv"];
                }
                else if (sampleType == 2) {
                    tradesGrid.rows[i].class = ["background-oos"];
                }
            }
        };
    }

    function loadViews() {
        var settings = SQConstants.getSettings();
        loadToArray($scope.views, angular.copy(SQConstants.getConstants().tradelistViews));

        setView(settings.tradelistView);

        try {
            $scope.$digest();
        } catch (err) { }
    }

    function setView(viewName) {
        if (arrayNotEmpty($scope.views)) {
            var defaultView;

            for (var i = 0; i < $scope.views.length; i++) {
                var view = $scope.views[i];

                if (view.name == 'Default') {
                    defaultView = view;
                }
                if (view.name == viewName) {
                    $scope.view.selected = view;
                    return;
                }
            }

            console.log("Tradelist view '" + viewName + "' not found. Using default view...");
            $scope.view.selected = defaultView;
        } else {
            console.error("no tradelist views available");
        }
    }

    //- Event Handlers --------------------------------------------------------------

    function onEvent(event, data) {
        if (event == SQEvents.get('TRADELIST_VIEWS_LOADED') || event == SQEvents.get('TRADELIST_VIEW_CHANGED')) {
            loadViews();
            initTradesGrid();
            updateGridRows();
        }
        else if (event == SQEvents.get('LANGUAGE_CHANGED')) {
            initTradesGrid();
            updateGridRows();
        }
        else return;

        try {
            $scope.$digest();
        } catch (er) { }
    }

    function refreshContent(strategyChanged, handlerContent) {
        if (strategyChanged) {
            strategyWasChanged = true;

            updateSettings(handlerContent);
            var args = handlerContent ? {
                resultKey: $scope.resultkey,
                direction: $scope.direction,
                sampleType: $scope.sampleType
            } : null;

            $scope.toolbar.refresh(args);
            return; //data will be fetched during next function call from onFilterChange method
        }

        if (!$scope.tab.active) {
            return;
        } else {
            window.dispatchEvent(new Event('resize'));
        }

        var dataInfo = SQResultsData.getDataInfo();
        if (!dataInfo.strategyName) {
            tradesGrid.removeAllRows();
            return;
        }

        SQResultsData.getData(handlerId, [$scope.resultkey, $scope.direction, $scope.sampleType]).then(function (result) {
            if (!result.changed && !strategyWasChanged) {
                SQResultsData.hideLoadingOverlay();
                return;
            }
            strategyWasChanged = false;

            SQResultsData.showLoadingOverlay();

            updateGridRows(function () {
                $timeout(SQResultsData.hideLoadingOverlay, 0, false);
            });
        });
    }

    function updateSettings(handlerContent) {
        if (!handlerContent || !handlerContent.projectName) {
            return;
        }

        var attributes = handlerContent.data.attributes;
        $scope.resultkey = attributes.resultKey;
        $scope.direction = attributes.direction;
        $scope.sampleType = attributes.sampleType;
    }

    function fetchData(params) {
        var deferred = $q.defer();

        //$timeout(function(){ deferred.resolve("no data"); }, 0, false);
        deferred.resolve("no data");
        return deferred.promise;
    }
    
    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    $scope.views = [];
    $scope.view = {};
    $scope.toolbar = {};
    $scope.resultkey;
    $scope.direction;

    var tradesGrid;

    var strategyWasChanged = false;

    loadViews();

    $timeout(initTradesGrid, 0, false);

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, ['resultKey', 'direction', 'sampleType']);

    var listenerId = "ResultsTradelistCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('TRADELIST_VIEWS_LOADED'),
        SQEvents.get("TRADELIST_VIEW_CHANGED"),
        SQEvents.get('LANGUAGE_CHANGED')
    ], onEvent);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
        SQResultsData.resultsTabLoaded("ResultsTradelistCtrl");
    }, 0, false);

    $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
    }
});