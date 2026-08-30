angular.module('app.resultstabs.wf').controller('WFResultsCtrl', function ($rootScope, $scope, $timeout, BackendService, SQEvents, SQConstants, WFResultsService, ColumnSettingsService, DatabankService, $q, SQResultsData, $element, L) {
    console.log("WFResults controller initialized");

    var grid;
    var wfresultkey = null;

    var RESULT_TYPE_WF = "WalkForwardOptimization";

    function initTable() {
        grid = new sqGrid("wf-grid");

        var gridColumns = [{
                title: L.tsq('Period IS'),
                type: "text",
                sort: 'text',
                align: 'left'
            },
            {
                title: L.tsq('Period OOS'),
                type: "text,background-oos",
                sort: 'text',
                align: 'left'
            },
            {
                title: L.tsq('Days IS'),
                type: "float2",
                sort: 'number'
            },
            {
                title: L.tsq('Days OOS'),
                type: "float2,background-oos",
                sort: 'number'
            }
        ];

        var widths = [150, 180, 70, 70];

        var view = $scope.manageviews.getSelectedView();
        if (view) {
            let cols = view.columns;
            for (var i = 0; i < cols.length; i++) {
                var title = cols[i].columnName + " " + (cols[i].sampleType == $scope.sampleTypes.in ? L.tsq('IS') : L.tsq('OOS'));

                gridColumns.push({
                    title: title,
                    type: DatabankService.getColumnTypeDefinition(cols[i]),
                    sort: getSortType(cols[i].type),
                });

                widths.push(cols[i].width || 150);
            }
        }

        gridColumns.push({
            title: L.tsq('Parameters'),
            type: "text",
            sort: 'text',
            align: 'left'
        });
        widths.push(300);

        gridColumns.push({
            title: '',
            type: "text",
            sort: 'text',
            align: 'center'
        });
        widths.push(160);

        grid.setFirstColumnAsId(false);
        grid.setColumns(gridColumns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText('No WF data');
        grid.enableCellTitles(true, ",");
        grid.disableSorting();
        grid.enableCheckboxes(false);
        // grid.enableHtmlCells();

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            if (eventName == "applyParams") {
                if(!$scope.applyingParams){
                    var paramIndex = grid.rows[rowIndex].cells.length - 2;
                    var params = grid.rows[rowIndex].cells[paramIndex];
                    var data = getRequestData();
                    data.parameters = params;
                    data.symmetricVariables = params.indexOf("Long") < 0 && params.indexOf("Short") < 0;
                    $scope.applyingParams = true;
                    WFResultsService.setParametersToStrategy(data, function () {
                        $rootScope.showSuccess(L.tsq("Strategy parameters modified"));
                        $scope.applyingParams = false;
                    });
                } else {
                    $rootScope.showError(L.tsq("Applying parameters...Please wait..."))
                }
            }
        };

        grid.headerRedraw();

        initialized = true;
    }

    function printTable() {
        if (!initialized) {
            return;
        }

        grid.removeAllRows(true, true);

        var dataInfo = SQResultsData.getDataInfo();

        var data = {};
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;
        data.resultkey = wfresultkey;
        data.view = $scope.manageviews.getSelectedViewName();

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        if (!data.strategy) {
            return;
        }

        WFResultsService.printTable(data, function (result) {
            for (var i = 0; i < result.rows.length; i++) {
                var rowData = result.rows[i].data;
                rowData.push(createActionLink(L.tsq('Apply params to strategy'), "action-link", "applyParams"));
            }

            grid.loadFromJSON(result);

            $scope.result.combination = result.combination;
            $scope.result.matrixResult = result.matrixResult;
            $scope.result.parametersStability = result.parametersStability;
        });
    }

    $scope.print = function (defaultConditions) {
        cgDeferred.promise.then(function () {
            _print(defaultConditions)
        });
    }

    function _print(defaultConditions) {
        var dataInfo = SQResultsData.getDataInfo();

        var data = {};
        data.project = dataInfo.projectName;
        data.databank = dataInfo.databankName;
        data.strategy = dataInfo.strategyName;

        if (!data.strategy) {
            return;
        }

        data.resultkey = wfresultkey;

        if (defaultConditions) {
            data.conditions = defaultConditions;
        } else {
            let conditionsObj = $scope.config.condGridInstance.getConditionsObj();
            data.conditions = xmlToString(conditionsObj);
        }

        data.thresholdPct = $scope.config.thresholdPct;
        data.robRows = $scope.config.robRows;
        data.robColumns = $scope.config.robColumns;
        data.robComb = $scope.config.robComb;

        data.type = $scope.config.chartType;

        if (dataInfo.isAlgoWizardCloud || dataInfo.isAlgoWizard) data.backtestID = dataInfo.backtestID;

        var columnObj = getColumnObj();
        if (columnObj) data.column = new XMLSerializer().serializeToString(columnObj);

        WFResultsService.print(data, function (resData) {
            printChart(resData.chart);

            //print conditions
            if (resData.conditions) {
                let conditionsObj = xmlToObject(resData.conditions).find("Conditions")[0];
                $scope.config.condGridInstance.setConditionsObj(conditionsObj, true);

                $scope.config.thresholdPct = getAttrIntValue(conditionsObj, "thresholdPct", 80);
                $scope.config.robRows = getAttrIntValue(conditionsObj, "robCombRows", 3);
                $scope.config.robColumns = getAttrIntValue(conditionsObj, "robCombCols", 3);
                $scope.config.robComb = getAttrIntValue(conditionsObj, "robMinComb", 7);
            }

            //print results
            if (resData.result) {
                var parametersStability = $scope.result.parametersStability;
                $scope.result = resData.result;
                $scope.result.parametersStability = parametersStability;
            }

            updateConditionRowColors($scope.config.condGridInstance.getGrid(), 'wf', 4);

            window.dispatchEvent(new Event('resize'));

            $scope.$evalAsync();
        });
    }

    $scope.chartTypeChanged = function (chartType) {
        $scope.config.chartType = chartType;
        $scope.print();
    }

    $scope.openDisplayOptions = function () {
        $scope.columnSettings.class = valueToDisplay.columnName;
        $scope.columnSettingsInstance.setSettings($scope.columnSettings);

        showPopup("#wfDisplayOptionsPopup");
    }

    $scope.saveDisplayOptions = function () {

        $scope.columnSettings = $scope.columnSettingsInstance.getSettings();
        updateDisplayOptionsText();
        $scope.print();

        hidePopup("#wfDisplayOptionsPopup");
    }

    $scope.onLoadDefaultWFConditions = function () {
        var options = [{
                title: L.tsq("Replace"),
                key: "replace-default-conditions"
            },
            {
                title: L.tsq("Add"),
                key: "add-default-conditions"
            }
        ]

        if ($scope.config.condGridInstance.size() > 0) {
            $rootScope.showOptionsDialog(L.tsq("Set recommended WF conditions"), L.tsq("There already are some conditions in the table. Do you want to Replace them or Add recommended WF conditions to the table?"), function (option) {
                $scope.print(option.key);
            }, options);
        } else {
            $scope.print("add-default-conditions");
        }
    }

    function updateDisplayOptionsText() {
        var columnObj = getColumnObj();
        if (columnObj) {
            $scope.displayOptions = ColumnSettingsService.printColumnTitle(columnObj);
        } else {
            $scope.displayOptions = "N/A";
        }
    }

    function getColumnObj() {
        var selectedItems = $scope.valueToDisplay==0 ? $scope.instanceItemChooser.getSelectedItems() : $scope.instanceItemChooserSpecial.getSelectedItems();
        var colSettings = $scope.columnSettings;

        if (selectedItems && selectedItems[0] && colSettings) {
            var selectedItem = selectedItems[0];

            var column = getItem($scope.valueToDisplay==0 ? valueToDisplayColumns : valueToDisplayColumnsSpecial, 'class', selectedItem);
            if (column) {
                valueToDisplay.columnName = column.class;

                var xmlDoc = document.implementation.createDocument(null, "WFResult");

                var columnObj = xmlDoc.createElement('Column');
                columnObj.setAttribute("class", column.class);
                columnObj.setAttribute("name", selectedItem);

                columnObj.setAttribute("sampleType", colSettings.sampleType);
                columnObj.setAttribute("direction", colSettings.direction);
                columnObj.setAttribute("plType", colSettings.plType);
                columnObj.setAttribute("resultType", RESULT_TYPE_WF);

                columnObj.setAttribute("confidenceLevel", colSettings.confidenceLevel);
                columnObj.setAttribute("market", colSettings.market);
                columnObj.setAttribute("subresult", $scope.valueToDisplay==0 ? colSettings.subresult : columnTypes.wfSpecial.value);

                return columnObj;
            }
        }

        return null;
    }

    var graph = null;

    function printChart(chart) {
        if (!chart || !chart.values) {
            return;
        }

        $scope.result.matrixResult = true;

        if($scope.config.chartType=='score') {
            printWFMResultTable(chart);
        } 
        else {
            $timeout(function () {
                let data = new vis.DataSet();
                var values = chart.values;

                for (var i = 0; i < values.length; i++) {
                    data.add(values[i]);
                }

                // specify options
                var options = {
                    axisColor: "#808080",
                    width: '100%',
                    height: '100%',
                    yCenter: "50%",
                    xLabel: chart.xlabel,
                    yLabel: chart.ylabel,
                    zLabel: chart.zlabel,
                    zMin: chart.zMin,
                    zMax: chart.zMax,
                    showPerspective: true,
                    showGrid: true,
                    keepAspectRatio: false,
                    showLegend: true,
                    legendLabel: "",
                    tooltip: function (point) {
                        if(chart.periodInPercent) {
                            return 'WF: ' + point.y + ' runs: ' + point.x + ' % OOS<br>' + chart.zlabel+': ' + point.z;
                        } else {
                            return 'WF: ' + point.y + ' OOS days: ' + point.x + ' IS days<br>' + chart.zlabel+': ' + point.z;
                        }
                    },
                    cameraPosition: {
                        horizontal: -0.35,
                        vertical: 0.22,
                        distance: 2.0
                    },
                    reverseColorScale: !chart.reverseColorScale
                };

                options.verticalRatio = 0.5;

                if ($scope.config.chartType == 'top') {
                    options.style = 'surface';
                    options.verticalRatio = 0.0001;
                    options.yCenter = "40%";
                } else {
                    options.style = $scope.config.chartType;
                }

                // create our graph
                if (graph) {
                    graph.setData(data);
                    graph.setOptions(options);
                    graph.redraw();
                } else {
                    var container = document.getElementById('wf-chart');
                    graph = new vis.Graph3d(container, data, options);
                }

                if ($scope.config.chartType == 'top') {
                    var pos = {
                        horizontal: 1.57,
                        vertical: 1.57,
                        distance: 2.5,
                    };
                    graph.setCameraPosition(pos);
                }
            });
        }
    }

    function printWFMResultTable(chart) {
        $scope.chart.values = chart ? chart.values : null;
        $scope.chart.xlabel = chart ? chart.xlabel : null;
        $scope.chart.ylabel = chart ? chart.ylabel : null;
        $scope.chart.x = chart ? chart.x : null;
        $scope.chart.y = chart ? chart.y : null;
        $scope.chart.periodInPercent = chart ? chart.periodInPercent : null;
        
        $scope.$evalAsync();
    }

    $scope.onFilterChange = function (strategyName, resultkey, direction, sampleType) {
        wfresultkey = resultkey;

        refreshContent();
    }

    function printAll() {
        var data = getRequestData();
        if (!data.strategy) {
            return;
        }

        WFResultsService.getConditions(data, function (result) {
            cgDeferred.promise.then(function () {
                var conditionsObj = xmlToObject(result.conditions).find("Conditions")[0];
                $scope.config.condGridInstance.setConditionsObj(conditionsObj, true);

                $scope.config.thresholdPct = getAttrIntValue(conditionsObj, "thresholdPct", 80);
                $scope.config.robRows = getAttrIntValue(conditionsObj, "robCombRows", 3);
                $scope.config.robColumns = getAttrIntValue(conditionsObj, "robCombCols", 3);
                $scope.config.robComb = getAttrIntValue(conditionsObj, "robMinComb", 7);
            });

            printTable();
            $scope.print();
        });
    }

    function getRequestData() {
        var dataInfo = SQResultsData.getDataInfo();
        return {
            project: dataInfo.projectName,
            databank: dataInfo.databankName,
            strategy: dataInfo.strategyName
        };
    }

    $scope.resetSettings = function () {
        $rootScope.showError("Not implemented yet.");
    }

    $scope.openArticle = function () {
        BackendService.sendRequest('/main/openlink', {
            url: 'https://docs.strategyquant.com/walk-forward-optimization'
        });
    }

    //-----------------------------------
    var initialized = false;
    $scope.instanceItemChooser = {};
    $scope.instanceItemChooserSpecial = {};

    $scope.config = {
        condGridInstance: {},
        thresholdPct: 50,
        robRows: 3,
        robColumns: 3,
        robComb: 7,
        chartType: 'score'
    }

    $scope.result = {
        passed: false,
        matrixResult: false,
        combination: 'N/A',
        condPassed: 0,
        condTotal: 0,
        combPassed: 0,
        combTotal: 0,
        condPassedInBestGroup: 0,
        groupSize: 0,
        bestGroupArroundCell: 'N/A',
        recommendedCombination: 'N/A',
        scorePct: 0,
    };

    $scope.manageviews = {
        initCallback : initTable
    };
    $scope.manageviews.viewChanged = function () {
        initTable();
        printTable();
    }

    var plTypes = SQConstants.getConstants().plTypes;

    var loadedCG = false;

    var recentColumns = SQConstants.getColumns().recentColumns;

    var columnTypes = SQConstants.getConstants().databankColumnTypes;
    var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

    var valueToDisplayColumns = SQConstants.getColumnsByType([columnTypes.general], null, true);
    var valueToDisplayColumnsSpecial = SQConstants.getColumnsByType([columnTypes.wfSpecial], null, true);

    var valueToDisplay = {
        columnName: "NetProfit", //"Net profit"
        columnType: columnTypes.general,
    }

    function onColumnChange() {
        var selectedItems = $scope.instanceItemChooser.getSelectedItems();
        if (selectedItems && selectedItems[0]) {
            var selectedItem = selectedItems[0];

            var column = getItem(valueToDisplayColumns, 'class', selectedItem);
            if (column) {
                valueToDisplay.columnName = column.class;
                valueToDisplay.columnType = column.columnType;

                $scope.columnSettingsInstance.columnChanged(column.class);
            }
        }
    }

    function onColumnSpecialChange() {
        var selectedItems = $scope.instanceItemChooserSpecial.getSelectedItems();
        if (selectedItems && selectedItems[0]) {
            var selectedItem = selectedItems[0];

            var column = getItem(valueToDisplayColumnsSpecial, 'class', selectedItem);
            if (column) {
                valueToDisplay.columnName = column.class;
                valueToDisplay.columnType = column.columnType;

                $scope.columnSettingsInstance.columnChanged(column.class);
            }
        }
    }

    $scope.afterChooserInit = function () {
        var item = null;

        if (valueToDisplayColumns.length > 0) {
            item = "NetProfit";
        }

        $scope.instanceItemChooser.init(valueToDisplayColumns, onColumnChange, item);

        $timeout(function () {
            updateDisplayOptionsText();
            try { $scope.$digest(); } catch (err) {}
        }, 0, false);
    }

    $scope.afterChooserSpecialInit = function () {
        var item = null;

        if (valueToDisplayColumnsSpecial.length > 0) {
            item = valueToDisplayColumnsSpecial[0].class;
        }

        $scope.instanceItemChooserSpecial.init(valueToDisplayColumnsSpecial, onColumnSpecialChange, item);

        $timeout(function () {
            updateDisplayOptionsText();
            try { $scope.$digest(); } catch (err) {}
        }, 0, false);
    }

    $scope.afterCGInit = function () {
        loadedCG = true;

        $scope.config.condGridInstance.setData(columns, recentColumns);

        cgDeferred.resolve();
    }

    $scope.onCGRefresh = function (_grid) {
        if (!loadedCG) {
            return;
        }

        $scope.print();
    }

    $scope.onValueToDisplaySelect = function(type) {
        $scope.valueToDisplay = type;

        if($scope.valueToDisplay==0) {
            onColumnChange();
        } else {
            onColumnSpecialChange();
        }
    }

    $scope.rows = [2, 3, 4, 5];
    $scope.columns = [2, 3, 4, 5];

    $scope.combinations = [];
    for (let i = 0; i <= 25; i++) {
        $scope.combinations.push(i);
    }

    var cgDeferred = $q.defer();

    $scope.onExport = function () {
        $rootScope.showCSVXLSXExportDialog("WFParamsExport", "WFParamsExport", function (path, useComma) {
            var dataInfo = SQResultsData.getDataInfo();
            
            var data = {};
            data.project = dataInfo.projectName;
            data.databank = dataInfo.databankName;
            data.strategy = dataInfo.strategyName;
            data.resultkey = wfresultkey;
            data.view = $scope.manageviews.getSelectedViewName();
            data.path = path;
            data.useComma = useComma;

            WFResultsService.export(data);
        });
    }

    function refreshContent(strategyChanged, handlerContent) {
        if (strategyChanged) {
            $scope.toolbar.refresh();
            return; //data will be fetched during next function call from onFilterChange method
        }

        if (!$scope.tab.active) return;

        SQResultsData.getData(handlerId, []).then(function (result) {
            printAll();
        });
    }

    function fetchData(params) {
        var deferred = $q.defer();

        $timeout(function () {
            deferred.resolve({});
        }, 0, false);

        return deferred.promise;
    }

    $scope.applyingParams = false;
    $scope.directions = ColumnSettingsService.directions;
    $scope.sampleTypes = ColumnSettingsService.sampleTypes;
    $scope.plTypes = ColumnSettingsService.plTypes;
    $scope.resultTypes = ColumnSettingsService.resultTypes;
    $scope.wfSubresultsList = ColumnSettingsService.wfSubresultsList;

    $scope.chart = {};
    $scope.toolbar = {};
    $scope.columnSettingsInstance = {};
    $scope.columnSettings = {
        class: valueToDisplay.columnName,
        resultType: RESULT_TYPE_WF,
        sampleType: $scope.sampleTypes.full,
        direction: $scope.directions.both,
        plType: $scope.plTypes.money,
        confidenceLevel: 50,
        market: 1,
        subresult: $scope.wfSubresultsList[0].value
    };

    var settings = {};

    $scope.displayOptions = "N/A";

    $scope.valueToDisplay = 0;

    var handlerId = $scope.tab.title; //handler id must be set to tab title
    SQResultsData.addHandler(handlerId, fetchData, refreshContent, []);

    var watchersHandler = new WatchersHandler($($element));
    $timeout(function () {
        watchersHandler.onActivated(false);
    }, 0, false);

    $scope.tab.tabChanged = function () {
        watchersHandler.onActivated($scope.tab.active);
    }
});