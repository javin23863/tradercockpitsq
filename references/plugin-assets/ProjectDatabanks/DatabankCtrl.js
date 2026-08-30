angular.module('app.databanks').controller('DatabankCtrl', function($rootScope, $scope, $timeout, $q, DatabankService, DatabankActionsService, ColumnSettingsService, SQEvents, AppService, SQWebSocketService, SQConstants, UniqueIdGenerator, L) {
    console.log("DatabankCtrl initialized");

    function updateDatabankColumns(databankGrid){
        var currentView = getCurrentView();
        currentColumns = [{
            title: L.tsq('Strategy Name'),
            type: "text",
            sort: 'text',
            tooltip: L.tsq('Strategy Name'),
            width: 200,
            align: 'left'
        }];

        if (window.appConfig.project != 'Builder' && window.appConfig.project != 'PortfolioMaster') {
            currentColumns.push({
                title: L.tsq("Filters result"),
                type: "filterResult",
                sort: 'text',
                tooltip: L.tsq('Filters result'),
                width: 100,
            });
        }

        if (window.appConfig.project == 'Optimizer') {
            currentColumns.push({
                title: L.tsq("Parameters"),
                type: "text",
                sort: 'text',
                tooltip: L.tsq('Parameters'),
                width: 200,
                align: 'left'
            }, {
                title: L.tsq("Best WF"),
                type: "text",
                sort: 'text',
                tooltip: L.tsq('Best WF'),
                width: 120,
            });
        }

        if (currentView && currentView.columns) {
            for (var i = 0; i < currentView.columns.length; i++) {
                let column = currentView.columns[i];
                var width = column.width || 150;
                let className = column.class;

                if (className == "MiniEquityChart") {
                    width = "100f"
                }

                var title = ColumnSettingsService.printColumnTitle(createColumnObj(column));

                let tooltip = getColumnTooltip(className);
                tooltip = tooltip ? title + ' - ' + tooltip : title;

                currentColumns.push({
                    title: title,
                    type: DatabankService.getColumnTypeDefinition(column),
                    sort: getSortType(column.type),
                    tooltip: tooltip,
                    width: width,
                });
            }
        } else {
            $rootScope.showError(L.tsq("Cannot get databank view"));
        }

        //prepare reset data
        resetData = [];

        for (var i = 0; i < currentColumns.length; i++) {
            var column = currentColumns[i];
            resetData.push(DatabankService.getColumnResetValue(column.type));
        }
        databankGrid.setColumns(currentColumns, !!databankGrid);
    }

    function getColumnTooltip(className) {
        let columns = SQConstants.getColumns();
        var column = getItem(columns.databankColumns, 'class', className);
        return column ? column.tooltip : null;
    }

    function initGrid() {
        if ($scope.tab.display === false) return; //don't init databank if not visible

        var isFirstInit = !databankGridInitialized;

        databankGridInitialized = false;

        if (!databankGrid) {
            databankGrid = new sqGrid($scope.gridId);
        } else {
            databankGrid.removeAllRows(true);
        }

        updateDatabankColumns(databankGrid)
        databankGrid.showCellTitles = true;
        databankGrid.setFirstColumnAsId(true, false);
        databankGrid.enableCheckboxes(true);
        databankGrid.enableHtmlCells();
        databankGrid.setEmptyGridText(L.tsq('No results in databank.'));
        databankGrid.defineWidget('sparklinesWidget', sqGridSparklines);
        databankGrid.defineWidget('tooltipWidget', sqGridTooltip);
        databankGrid.defineWidget('inputWidget', sqGridInput);

        databankGrid.cellEventHandler = function(rowIndex, cellIndex, eventName, args) {
            if (eventName == "inputUpdated") {
                var selectedStrategy = databankGrid.getRowId(rowIndex);

                var columnName = databankGrid.getColumnLabel(cellIndex);

                var currentView = getCurrentView();
                var column = getItem(currentView.columns, "name", columnName);

                DatabankService.setDatabankColumnValue(selectedStrategy, column.class, args[0]);
            }
        };

        databankGrid.onCellDoubleClick = function(rowIndex) {
            var selectedStrategy = databankGrid.getRowId(rowIndex);
            DatabankService.selectStrategy(selectedStrategy);
            AppService.switchToPanel('results');
        };

        databankGrid.onGridUserInteracted = function(action) {
            if (!loadingAllContent && !allContentLoaded) {
                gridLastChangeTime = 0;
                loadStrategies(false, databankGrid.sortAll);
            }
        };

        databankGrid.onSelectionChanged = function(rowIndexes, values) {
            if (!loadingAllContent && !allContentLoaded) {
                gridLastChangeTime = 0;

                loadStrategies(false, function() {
                    onSelectionChanged(rowIndexes, values);
                });
            } else {
                onSelectionChanged(rowIndexes, values);
            }
        }

        databankGrid.onKeyPress = function(keyCode, e) {
            if (keyCode == "Delete") {
                deleteStrategies();
            }
            if (window.appConfig.product == window.top.activeApp) {
                var key = e.key.toLowerCase();
                if (e.ctrlKey && (key == "c" || key == "v")) {
                    e.preventDefault();
                    var strategies = getSelectedIDs();
                    if (key == "c") {
                        if (strategies == "") {
                            return;
                        }
                        DatabankService.copyStrategies(strategies);
                    }
                    if (key == "v") {
                        initGrid()
                        DatabankService.pasteStrategies();
                    }
                    return false;
                }
            }
        };

        databankGridInitialized = true;
        initTimeoutID = null;

        databankGrid.headerRedraw();

        if (DatabankService.projectName != 'TaskManager' || !isFirstInit) {
            $timeout(function() {
                gridLastChangeTime = 0;
                loadStrategies(true);
            }, 0, false);
        }

        currentViewHash = getViewHash($scope.tab.view);

        if ($scope.tab.active) {
            SQEvents.notifyListeners(SQEvents.get('DATABANK_VIEW_SELECTED'), $scope.tab.view);
        }

        /*         $(document).on('keydown', function (e) {
                    
                }); */

        let element = document.getElementById($scope.gridId).querySelector(".sq-grid-inner");

        $.contextMenu({
            selector: "#" + $scope.gridId + " .sq-grid-inner",
            appendTo: element,
            callback: function(itemKey, opt, e) {
                var strategies = getSelectedIDs();
                if (itemKey == "cut") {
                    if (strategies == "") {
                        return;
                    }
                    DatabankService.cutStrategies(strategies);
                }
                if (itemKey == "copy") {
                    if (strategies == "") {
                        return;
                    }
                    DatabankService.copyStrategies(strategies);
                }
                if (itemKey == "paste") {
                    initGrid()
                    DatabankService.pasteStrategies();
                }
            },
            items: {
                "cut": {
                    name: "Cut",
                    icon: "fa-scissors"
                },
                "copy": {
                    name: "Copy",
                    icon: "fa-files-o"
                },
                "paste": {
                    name: "Paste",
                    icon: "fa-clipboard"
                }
            }
        });
    }

    function onSelectionChanged(rowIndexes, values) {
        var strategies = getSelectedIDs();

        AppService.setSelectedStrategies(strategies);
        SQEvents.notifyListeners(SQEvents.get('STRATEGY_SELECTED'), strategies);
    }

    function createColumnObj(column) {
        var xmlDoc = document.implementation.createDocument(null, "View");

        var columnObj = xmlDoc.createElement('Column');
        columnObj.setAttribute("class", column.class);
        columnObj.setAttribute("name", column.columnName);

        columnObj.setAttribute("sampleType", column.sampleType);
        columnObj.setAttribute("direction", column.direction);
        columnObj.setAttribute("plType", column.plType);
        columnObj.setAttribute("resultType", column.resultType);

        columnObj.setAttribute("confidenceLevel", column.confidenceLevel);
        columnObj.setAttribute("market", column.market);
        columnObj.setAttribute("subresult", column.subresult);

        return columnObj;
    }

    function getCurrentView() {
        var allViews = SQConstants.getConstants().databankViews;
        var view = getItem(allViews, "name", $scope.tab.view);
        if (!view) {
            view = getItem(allViews, "name", SQConstants.getConstants().defaultDatabankViews.main);
        }

        return view;
    }

    function getViewHash(viewName) {
        var currentView = getItem(SQConstants.getConstants().databankViews, 'name', viewName);
        return currentView ? JSON.stringify(currentView) : '';
    }

    function getSelectedIDs(checkedOnly) {
        if (!databankGrid) return '';

        var data = databankGrid.getSelectedRows().checkedRowsData;

        $timeout(function() {
            $scope.tab.selectedNumber = data.length;
            try { $scope.$digest(); } catch (err) {}
        }, 0, false);

        if (data.length == 0 && !checkedOnly) {
            var selectedRow = databankGrid.getSelectedRowOnly();
            return selectedRow && selectedRow.data ? selectedRow.data[0] : "";
        } else {
            var ids = '';
            for (var i = 0; i < data.length; i++) {
                ids += (i == 0 ? "" : ",") + data[i][0];
            }

            return ids;
        }
    }

    function deleteStrategies(strategies, deleteViaButton) {
        var selectedRow = databankGrid.getSelectedRowOnly();

        if (deletingStrategiesPromise || (!deleteViaButton && !selectedRow)) return;

        var delDeferred = $q.defer();
        deletingStrategiesPromise = delDeferred.promise;

        var rowsCount = databankGrid.getNumberOfRows();
        var gridSorted = databankGrid.isSorted();
        var lastSelectedIndex = gridSorted ? databankGrid.getSortedIndexFromRowIndex(selectedRow.index) : selectedRow.index;
        var strategyToLoad = null;

        var selectedStrategyNames = strategies ? strategies.split(",") : null;
        var nextIndex = -1;

        for (var i = deleteViaButton ? lastSelectedIndex : lastSelectedIndex + 1; i < databankGrid.getNumberOfRows(); i++) {
            var rowIndex = gridSorted ? databankGrid.sortedRowsBuffer[i].index : i;
            strategyToLoad = databankGrid.getCellValue(rowIndex, 0);

            if (!selectedStrategyNames || selectedStrategyNames.indexOf(strategyToLoad) < 0) {
                nextIndex = rowIndex;
                break;
            }
        }

        if (nextIndex === -1) {
            if (rowsCount > (selectedStrategyNames ? selectedStrategyNames.length : 0) && lastSelectedIndex > 0) {
                strategyToLoad = databankGrid.getCellValue(gridSorted ? databankGrid.sortedRowsBuffer[lastSelectedIndex - 1].index : (lastSelectedIndex - 1), 0);
            } else {
                strategyToLoad = null;
            }
        }

        if ($rootScope.project.name != 'Builder' && ($rootScope.project.state == $scope.projectStates.loading || $rootScope.project.state == $scope.projectStates.running)) {
            $rootScope.showError(L.tsq("Cannot delete records from databank when project is in loading/running state"));
            return;
        }

        DatabankService.removeReports(deleteViaButton ? strategies : selectedRow.data[0]).then(function(result) {
            if (result.success) {
                if (strategyToLoad !== null) {
                    if (!AppService.getActivePanel()) { //Results displayed
                        DatabankService.selectStrategy(strategyToLoad);
                    }
                    delDeferred.resolve({ strategyToLoad: strategyToLoad, deleteViaButton: deleteViaButton });
                } else {
                    SQEvents.notifyListeners(SQEvents.get('STRATEGY_SELECTED'), '');
                    deletingStrategiesPromise = null;
                }
            } else {
                $rootScope.showError(L.tsq("Error while removing reports.") + " " + result.error);
                deletingStrategiesPromise = null;
            }
        });
    }

    function refreshView() {
        SQEvents.notifyListeners(SQEvents.get("DB_VIEW_CHANGED"), AppService.getDatabank().view);
        window.parent.broadcastEvent(SQEvents.get("DB_VIEW_CHANGED"), AppService.getDatabank().view);
    }

    function loadStrategies(first1000, callback, refreshAction) {
        if ((loadingAllContent || loadingFirst1000) && !refreshAction) return;

        if (!first1000) {
            loadingAllContent = true;
        } else {
            loadingFirst1000 = true;
        }

        var dbName = $scope.tab.title;

        gridLoadTime = 1;

        gridLastChangeTime = refreshAction ? 0 : gridLastChangeTime;

        DatabankService.loadGridData(dbName, first1000, refreshAction, gridLastChangeTime, function(result) {
            if (!databankGridInitialized) {
                loadingAllContent = false;
                loadingFirst1000 = false;
                return;
            }

            if (refreshAction) AppService.printToLog("Databank contents received - " + (result.gridData ? result.gridData.length : 0) + " records", false);

            try {
                gridLoadTime = result.loadTime;
                gridLastChangeTime = gridLoadTime;

                if (result.gridData) {
                    databankGrid.removeAllRows(true, true, true);

                    for (var i = 0; i < result.gridData.length; i++) {
                        var rowData = result.gridData[i];
                        var rawData = angular.copy(result.gridData[i]);
                        rawData.length = rawData.length - 1;

                        var index = databankGrid.addRow(rawData, true);
                        updateStrategyProblems(rowData, index);
                    }

                    if (loadingAllContent) {
                        databankGrid.sortAll(true);
                    }
                    databankGrid.bodyRedraw();
                }

                loadingAllContent = false;
                loadingFirst1000 = false;

                allContentLoaded = !first1000 || result.totalCount <= 1000;

                if (missedUpdates.length) {
                    updateDatabankStrategies([{ name: $scope.tab.title, changes: missedUpdates }]);
                    missedUpdates.length = 0;
                } else {
                    updateStrategiesCount(result.totalCount);
                }

                if (refreshAction) AppService.printToLog("Databank grid updated. Total records in grid: " + databankGrid.getNumberOfRows(), false);
            } catch (err) {
                console.error(err);
                $rootScope.showError(L.tsq("Error while loading strategies into databank '%s' - %s", [dbName, err]));

                if (refreshAction) AppService.printToLog("Databank grid not updated" + err, true);
            }

            if (callback) callback();
        });
    }

    //Updates strategies list in current databank without reloading
    function updateDatabankStrategies(updates) {
        var refreshGrid = false;
        var changesMade = false;
        var refreshSelectedRows = false;

        if (!databankGrid) return;

        if (!allContentLoaded) {
            gridLastChangeTime = 0;
            loadStrategies();
            return;
        }

        if (executingUpdate) {
            var msg = "DatabankCtrl for " + AppService.getProject() + "/" + $scope.tab.title + " is overwhelmed, old updates were not processed yet and new updates are coming";
            AppService.printToLog(msg, true);
        }

        executingUpdate = true;

        //when project is running and switching between apps, grid gets reloaded and we may miss some updates
        var reloadingGrid = loadingAllContent || loadingFirst1000;

        for (var i = 0; i < updates.length; i++) {
            var update = updates[i];

            if (update.name != $scope.tab.title) continue;

            if (reloadingGrid && arrayNotEmpty(update.changes)) {
                for (var c = 0; c < update.changes.length; c++) {
                    missedUpdates.push(update.changes[c]);
                }

                continue;
            }

            databankGrid.onSelectionChanged = null; //disable selection changed handler

            if (arrayNotEmpty(update.changes)) {
                //console.error(update);
                for (var a = 0; a < update.changes.length; a++) {
                    var change = update.changes[a];

                    if (change.time <= gridLoadTime || gridLoadTime == 0) {
                        continue; //skip updates created before grid data load
                    }

                    gridLastChangeTime = change.time > gridLastChangeTime ? change.time : gridLastChangeTime;

                    var shouldRefresh = change.extraValue == "refreshGrid";
                    refreshGrid = refreshGrid || shouldRefresh;
                    if (change.type == 'add') {
                        changesMade = true;

                        var rawData = angular.copy(change.data);
                        rawData.length = rawData.length - 1;

                        var index = databankGrid.addRow(rawData, true);
                        updateStrategyProblems(change.data, index);
                    } else if (change.type == 'remove') {
                        changesMade = true;
                        refreshSelectedRows = true;

                        if (change.extraValue == "DatabankEmpty") {
                            databankGrid.removeAllRows();
                        } else if (change.id) {
                            databankGrid.removeRowById(change.id, true);
                        }
                    } else if (change.type == 'update') {
                        changesMade = true;
                        var index = databankGrid.modifyRowById(change.data, change.id, undefined, true);
                        updateStrategyProblems(change.data, index);
                    } else if (change.type == 'reset') {
                        if (change.id == null) {
                            resetGridRows();
                        } else {
                            resetGridRow(change.id);
                        }
                    } else if (change.type == 'clearAll') {
                        changesMade = true;
                        refreshSelectedRows = true;

                        databankGrid.removeAllRows(true, false, true);
                    } else {
                        console.error("Invalid change type '" + change.type + "'");
                    }
                }

                if (refreshGrid) {
                    databankGrid.sortAll();
                } else if (changesMade) {
                    databankGrid.bodyRedraw();
                }
                databankGrid.bodyRedraw()
                updateStrategiesCount();
            }
        }

        if (reloadingGrid) return;

        databankGrid.onSelectionChanged = onSelectionChanged; //reenable selection changed handler

        if (deletingStrategiesPromise) {
            deletingStrategiesPromise.then(function(params) {
                try {
                    databankGrid.selectRow(databankGrid.getRowIndex(params.strategyToLoad), params.deleteViaButton, true);
                    databankGrid.onSelectionChanged();
                    databankGrid.bodyRedraw();
                } catch (err) {
                    console.error(err);
                }
                deletingStrategiesPromise = null;
            });
        } else if (refreshSelectedRows) {
            onSelectionChanged();
        }

        executingUpdate = false;
    }

    function updateStrategyProblems(rowData, rowIndex) {
        var problemsCode = rowData[rowData.length - 1];
        databankGrid.setUserData(rowIndex, 'problemsCode', problemsCode);

        if (problemsCode != 0) {
            databankGrid.setIcon(rowIndex, 0, true, "right", 'strategy-problem', getProblemsText(problemsCode), true);
        } else {
            databankGrid.setIcon(rowIndex, 0, false, "", "", "", true);
        }
    }

    function getProblemsText(code) {
        var errorCodes = SQConstants.getConstants().strategyProblems;
        var errorText = "<b>" + L.tsq("Strategy problems") + ":</b><br>";

        var curErrorCode = parseInt(code);

        for (var i = 0; i < errorCodes.length; i++) {
            var value = errorCodes[i].code;

            if ((curErrorCode & value) == value) {
                errorText += errorCodes[i].shortInfo + "<br>";
            }
        }

        return errorText;
    }

    function updateStrategiesCount(count) {
        $scope.tab.records = count ? count : databankGrid.getNumberOfRows();

        var databank = getItem(DatabankService.databanks, 'name', $scope.tab.title);
        if (databank) {
            databank.records = count ? count : databankGrid.getNumberOfRows();
        }

        $scope.tab.triggerDigest();
    }

    function resetGridRows() {
        for (var i = 0; i < databankGrid.getNumberOfRows(); i++) {
            resetData[0] = databankGrid.getRowId(i);
            databankGrid.modifyRowByIndex(resetData, i);
        }
    }

    function resetGridRow(rowId) {
        resetData[0] = rowId;
        databankGrid.modifyRowByIndex(resetData, databankGrid.getRowIndex(rowId));
    }

    //- Event Handlers and Watches --------------------------------------------------------------

    function onEvent(event, data) {
        if (!$scope.tab.active) return;

        if (event == SQEvents.get('STRATEGY_SELECTED')) { //reaction on Select all/Unselect all action
            if (!databankGrid) return;

            if (data == 'all') {
                databankGrid.selectAllRows();
            } else if (!data) {
                if (databankGrid.getSelectedRows().selectedRowsData.length > 0) {
                    databankGrid.selectNoneRows();
                }
            }
        } else if (event == SQEvents.get('DB_VIEW_CHANGED')) {
            if (!$scope.tab.active) return;

            $scope.tab.view = data;
            $scope.tab.onSaveSettings();

            initGrid();

            try {
                $scope.$digest();
            } catch (err) {}
        } else if (event == SQEvents.get('DATABANK_VIEWS_LOADED')) {
            var view = getItem(data, 'name', $scope.tab.view);

            if (!view) {
                //if current view doesn't exist, set the default view
                console.log("Databank view '" + $scope.tab.view + "' doesn't exist anymore. Setting default view...");
                SQEvents.notifyListeners(SQEvents.get('DB_VIEW_CHANGED'), SQConstants.getConstants().defaultDatabankViews.main);
            } else { //current view properties were changed
                var viewHash = getViewHash(view.name);
                if (viewHash != currentViewHash) {
                    currentViewHash = viewHash;
                    initGrid();
                }
            }
        }
    }

    var destroyWatch = $scope.$watch("tab.display", function(newValue, oldValue) {
        if (oldValue === false && (newValue === undefined || newValue)) { //databank was hidden and now is visible
            $timeout(initGrid, 0, false);
        }
    });

    $scope.$on('$destroy', function() {
        UniqueIdGenerator.removeId($scope.gridId);
        SQEvents.removeListener(listenerId);
        window.parent.removeListener(listenerId);
        SQWebSocketService.removeListener(projectName, databankChannel, listenerId);
        destroyWatch();

        $.contextMenu('destroy', "#" + $scope.gridId + " .sq-grid-inner");
    });

    //- Initialization --------------------------------------------------------------

    var databankGrid;
    var databankGridInitialized = false;
    var currentViewHash = "";
    var currentColumns;
    var projectName = AppService.getProject();
    var initTimeoutID = null;
    var gridLoadTime = 0;
    var gridLastChangeTime = 0;
    var loadingAllContent = false;
    var loadingFirst1000 = false;
    var allContentLoaded = false;
    var deletingStrategiesPromise = null;
    var missedUpdates = [];
    var resetData = [];

    var executingUpdate = false;

    $scope.tab.getSelectedStrategies = getSelectedIDs;
    $scope.tab.getGrid = function() {
        return databankGrid;
    };
    $scope.tab.loadStrategies = loadStrategies;
    $scope.tab.deleteStrategies = deleteStrategies;
    $scope.tab.refreshView = refreshView;
    $scope.tab.selectedNumber = 0;

    $scope.projectStates = SQConstants.getConstants().runningStatuses;
    $scope.plTypes = SQConstants.getConstants().plTypes;

    $scope.gridId = "grid-" + $scope.tab.dataItem;

    $timeout(initGrid, 0, false);

    var listenerId = "databank-" + projectName + "/" + $scope.tab.title;
    SQEvents.addListener(listenerId, [
        SQEvents.get('STRATEGY_SELECTED'),
        SQEvents.get('DB_VIEW_CHANGED'),
        SQEvents.get("DATABANK_VIEWS_LOADED")
    ], onEvent);

    if (!isMainApp() && !isAlgoWizard()) {
        window.parent.addListener(listenerId, [
                SQEvents.get('DATABANK_SET_VIEW'),
                SQEvents.get('SKIN_CHANGED'),
                SQEvents.get('LANGUAGE_CHANGED'),
                SQEvents.get('APP_SWITCHED'),
            ],
            function(event, data) {
                if (event == SQEvents.get('LANGUAGE_CHANGED') || event == SQEvents.get('SKIN_CHANGED')) {
                    if (initTimeoutID != null) {
                        clearTimeout(initTimeoutID); //protection against double grid init when language and skin is changed at once  
                    }
                    initTimeoutID = setTimeout(initGrid, 300);
                } else if (event == SQEvents.get('APP_SWITCHED')) {
                    if (window.appConfig.product == data) {
                        var projectRunning = $rootScope.project.state == $scope.projectStates.loading || $rootScope.project.state == $scope.projectStates.running;
                        var loadAll = allContentLoaded || projectRunning;
                        loadStrategies(!loadAll);
                    }
                } else {
                    if (!data.viewRenamed && (AppService.getProject() != data.project || !$scope.tab.active)) return;

                    $scope.tab.view = data.viewName; //new view will be set on next views reload
                }
            });
    }

    var databankChannel = SQConstants.getConstants().channels.databanks;

    SQWebSocketService.subscribe(projectName, databankChannel, listenerId, updateDatabankStrategies);

});