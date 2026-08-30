angular.module('app').controller('IndicatorTesterCtrl', function ($rootScope, $scope, $timeout, SQConstants, L, IndicatorTesterService, SQWebSocketService, AppService) {

    function init() {

        var checkExist = setInterval(function () {
            if ($('#indyErrorGrid').length) {
                initGrid();
                initErrorGrid();
                clearInterval(checkExist);
            }
        }, 100);

        refreshIndicators();
    };

    function initGrid() {
        if (!grid) {
            grid = new sqGrid("indyTestGrid");
        } else {
            grid.removeAllRows(true);
        }

        var columns = [
            { title: L.tsq('Indicator'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Test file name'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Exists?'), type: "text", sort: 'text' },
            { title: L.tsq('Test parameters'), type: "text", sort: 'text' },
            { title: L.tsq('Decimals'), type: "number", sort: 'text' },
            { title: L.tsq('Test result'), type: "text", sort: 'text', align: 'left' },
            { title: '', type: "text", sort: 'text' }
        ];
        var widths = [120, '*', 50, 160, 60, 170, 20];

        grid.defineWidget('spinnerWidget', sqGridSpinner);
        grid.defineWidget('inputWidget', sqGridInput);

        grid.enableCheckboxes(true);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No tests.'));
        grid.disableSorting();

        grid.onSelectionChanged = function (rowIndexes, checked) {
            if (rowIndexes && rowIndexes.length) {
                //saveConfig();
                for (var i = 0; i < grid.getNumberOfRows(); i++) {
                    IndicatorTesterService.updateRowStyle(grid, i);
                }
            }
        };

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            if (eventName == "inputUpdated") {
                //saveConfig();
            } else if (eventName == "editParams") {
                onEditParams(rowIndex, args[0]);
            } else if (eventName == "showErrors") {
                showErrors(rowIndex);
            } else if (eventName == "removeTest") {
                onRemoveTest(rowIndex);
            } else if (eventName == "spinnerUpdated") {
                var value = args[0];
                if (value == parseInt(value)) {
                    //saveConfig();
                } else {
                    $rootScope.showError(L.tsq("Decimals count must be a number"));
                    sqGridSpinnerValueSet(grid, rowIndex, cellIndex, 6);
                    //saveConfig();
                }
            }
        };

        grid.headerRedraw();

        var filePath = SQConstants.getSettings()[pathName];
        IndicatorTesterService.loadConfig($scope.config, grid, filePath, function () {
            if (filePath) {
                $scope.configFileName = limitString(filePath, 45, true);
            }
            updateTestFolder();
        });
    }

    function initErrorGrid() {
        if (!errorGrid) {
            errorGrid = new sqGrid("indyErrorGrid");
        } else {
            errorGrid.removeAllRows(true);
        }

        var columns = [
            { title: L.tsq('Time'), type: "text", sort: 'text' },
            { title: L.tsq('SQ value'), type: "text", sort: 'text' },
            { title: L.tsq('Value from file'), type: "text", sort: 'text' }
        ];
        var widths = [180, 180, '*'];

        errorGrid.enableCheckboxes(false);
        errorGrid.setColumns(columns, !!errorGrid);
        errorGrid.setWidths(widths, !!errorGrid);
        errorGrid.setEmptyGridText(L.tsq('No errors.'));
        errorGrid.disableSorting();

        errorGrid.headerRedraw();
    }

    function showErrors(rowIndex) {
        var errors = grid.getUserData(rowIndex, 'errors');
        var showGrid = grid.getUserData(rowIndex, 'showGrid');

        if (errors) {
            if (showGrid) {
                errors = errors.split('\n');
                loadErrorsToGrid(errors);
                showPopup("#indicatorTesterErrorPopup");
            } else {
                $rootScope.showErrorModal(L.tsq("Errors"), errors, true, true);
            }
        }
    }

    function loadErrorsToGrid(errors) {
        errorGrid.removeAllRows(true, true);

        for (var i = 0; i < errors.length; i++) {
            var error = errors[i];

            errorGrid.addRow(error.split(','), true);
        }

        errorGrid.bodyRedraw();
    }

    function onEditParams(rowIndex, params) {
        var indicator = grid.getCellValue(rowIndex, 0);
        modifiedRowIndex = rowIndex;

        IndicatorTesterService.getIndicators(function (indicators) {
            var indy = getItem(indicators, 'name', indicator);
            if (!indy) {
                $rootScope.showError(L.tsq("Indicator '%s' not found", [indicator]));
                return;
            }

            copyIndyData($scope.currentIndy, indy);

            setCurrentParams(params);

            try { $scope.$digest(); } catch (err) { }

            showPopup('#editIndyParamsPopup');
        });
    }

    function copyIndyData(target, source) {
        target.className = source.className;
        target.name = source.name;
        target.shift = source.shift;
        loadToArray(target.parameters, source.parameters);
    }

    function setCurrentParams(params) {
        var rawParams = IndicatorTesterService.getRawParameters(params).split(',');
        var shift = IndicatorTesterService.getShift(params);

        for (var i = 0; i < $scope.currentIndy.parameters.length; i++) {
            var indyParam = $scope.currentIndy.parameters[i];
            indyParam.value = rawParams[i];
        }

        $scope.currentIndy.shift = shift;
    }

    $scope.onSaveParams = function () {
        var paramsStr = "";

        for (var i = 0; i < $scope.currentIndy.parameters.length; i++) {
            var indyParam = $scope.currentIndy.parameters[i];
            paramsStr += ((indyParam.name && indyParam.name.indexOf("Time") >= 0) ? getTimeStr(indyParam.value) : indyParam.value) + ",";
        }

        var newParamsValue = IndicatorTesterService.formatParameters($scope.currentIndy.name, removeLastChar(paramsStr), $scope.currentIndy.shift);
        grid.modifyRowByIndex(createActionLink(newParamsValue, "", "editParams", newParamsValue), modifiedRowIndex, 3);
        grid.bodyRedraw();

        hidePopup('#editIndyParamsPopup');
        //saveConfig();
    }

    function getTimeStr(value) {
        var hours = ("00" + parseInt(value / 3600)).slice(-2);
        var minutes = ("00" + parseInt((value % 3600) / 60)).slice(-2);
        return hours + ":" + minutes;
    }

    function saveConfig() {
        if (initialized) {
            IndicatorTesterService.saveConfig(null, IndicatorTesterService.getXMLConfig($scope.config, grid));
        }
    }

    function updateTestFolder() {
        $scope.testDataFolder = SQConstants.getSettings()['testsPath'] + "/Indicators/" + $scope.config.engine;
        $scope.testDataFolder = limitString($scope.testDataFolder, 45, true);

        $timeout(function () {
            initialized = true;
        });
    }

    function onSelectAll(select) {
        for (var i = 0; i < grid.getRowsNum(); i++) {
            grid.cellByIndex(i, 0).setValue(select);
            IndicatorTesterService.updateRowStyle(grid, grid.getRowId(i));
        }
    }

    function onRemoveTest(rowIndex) {
        var testName = grid.getCellValue(rowIndex, 0);

        $rootScope.showConfirm(L.tsq('Remove test'), L.tsq("Do you want to remove test '%s'?", [testName]), function (confirmed) {
            if (confirmed) {
                grid.removeRowByIndex(rowIndex);
                //saveConfig();
            }
        }, true);
    }

    function onRemoveAllTests() {
        $rootScope.showConfirm(L.tsq('Remove tests'), L.tsq("Do you want to remove all tests?"), function (confirmed) {
            if (confirmed) {
                grid.removeAllRows();
                //saveConfig();
            }
        }, true);
    }

    $scope.onAddNewTest = function () {
        $scope.indicatorsChooser.enableSameChoices();
        $scope.indicatorsChooser.clearSelection();
        showPopup('#newIndyTestPopup', function () {
            setTimeout(function () {
                $(
                    "#newIndyTestPopup input.chosen-search-input"
                ).focus();
            }, 10);
        });
    }

    function refreshIndicators() {
        IndicatorTesterService.getIndicators(function (indicators) {
            $scope.indicatorsChooser.init(indicators);
        });
    }

    $scope.addTests = function () {
        var selectedIndicators = $scope.indicatorsChooser.getSelectedItems();
        IndicatorTesterService.addTests(toSingleString(selectedIndicators, ","), IndicatorTesterService.getXMLConfig($scope.config, grid), grid);

        hidePopup('#newIndyTestPopup');
    }

    $scope.onStartTesting = function () {
        var selectedRows = grid.getSelectedRows().selectedRows;
        var somethingSelected = selectedRows.length > 0 && (selectedRows.length != 1 || grid.isRowChecked(selectedRows[0])); //recognize really checked rows, not just highlighted row

        if (somethingSelected) {
            IndicatorTesterService.startTesting(IndicatorTesterService.getXMLConfig($scope.config, grid));
        } else {
            $rootScope.showError("You have to select at least one test");
        }
    }

    $scope.onStopTesting = function () {
        IndicatorTesterService.stopTesting();
    }

    $scope.onNewConfig = function () {
        IndicatorTesterService.createNewConfig($scope.config, grid, function () {
            $scope.configFileName = L.tsq("<New config>");
        });
    }

    $scope.onLoadConfig = function () {
        var path = SQConstants.getSettings()[pathName];
        var defaultPath = SQConstants.getSettings()["settingsPath"];

        $rootScope.showFilePicker(L.tsq('Select config file to load'), path ? pathName : null, true, false, path ? null : defaultPath, fileExtension, null, function (paths) {
            if (paths) {
                IndicatorTesterService.loadConfig($scope.config, grid, paths[0], function () {
                    $rootScope.showSuccess("Tester config loaded");
                    $scope.configFileName = limitString(paths[0], 45, true);

                    SQConstants.getSettings()[pathName] = path;

                    window.dispatchEvent(new Event('resize'));
                    grid.bodyRedraw(true);

                    try { $scope.$digest(); } catch (err) { }
                });
            }
        });
    }

    $scope.onSaveConfig = function () {
        if ($scope.configFileName == L.tsq("<New config>")) {
            $scope.onSaveConfigAs();
        } else {
            saveConfig();
        }
    }

    $scope.onSaveConfigAs = function () {
        var fileName = "TesterConfig";
        var fileContent = IndicatorTesterService.getXMLConfig($scope.config, grid);

        $rootScope.saveFile(L.tsq("Save config"), fileExtension, pathName, fileName, fileContent, function (path) {
            IndicatorTesterService.saveConfig(path, fileContent, function () {
                window.parent.hidePopup("#saveFileDialog");

                SQConstants.getSettings()[pathName] = path;

                $rootScope.showSuccess("Tester config saved");
                $scope.configFileName = limitString(path, 45, true);
                try {
                    $scope.$digest();
                } catch (err) { }
            });
        });
    }

    $scope.onHowToMakeTestFile = function () {
        AppService.openBrowser('https://strategyquant.com/doc/programming-for-sq/testing-new-indicator-in-sq-x-vs-data-from-mt');
    }

    $scope.onDownloadTests = function () {
        IndicatorTesterService.downloadTests();
    }

    $scope.getParamOptionName = function (param) {
        var option = getItem(param.options, 'value', param.value);
        return option ? option.name : '';
    }

    function onWebSocketData(data) {
        if (data.IndicatorTesterStatus) {
            $scope.progressInfo = data.IndicatorTesterStatus;

            if ($scope.progressInfo.percent >= 100) {
                $timeout(function () {
                    $scope.progressInfo.percent = 0;
                }, 500);
            }

            updateButtons();

            IndicatorTesterService.reloadGrid(grid, $scope.progressInfo.xmlConfig);

            try {
                $scope.$digest();
            } catch (err) { }
        }
        else if (data.TestsDownloadStatus) {
            if (data.TestsDownloadStatus.error) {
                $rootScope.showError(data.TestsDownloadStatus.error);
                $rootScope.updateProgress(100);
            }
            else {
                if (data.TestsDownloadStatus.percent == 100) {
                    IndicatorTesterService.reloadGrid(grid, data.TestsDownloadStatus.xmlConfig);
                    $scope.configFileName = limitString(data.TestsDownloadStatus.filePath, 45, true);
                    $rootScope.showSuccess(L.tsq("Indicator tests successfully downloaded"));
                }
                $rootScope.updateProgress(data.TestsDownloadStatus.percent, data.TestsDownloadStatus.info, L.tsq("Indicator tests download"));
            }
        }
    }

    function updateButtons() {
        switch ($scope.progressInfo.status) {
            case $scope.projectStates.running:
                $scope.stopBtnEnabled = true;
                $scope.startBtnEnabled = false;
                break;
            default:
                $scope.stopBtnEnabled = false;
                $scope.startBtnEnabled = true;
                break;
        }
    }

    var destroyConfigWatch = $scope.$watch("config", function (newValue, oldValue) {
        if (initialized) {
            if (newValue.engine != oldValue.engine) {
                updateTestFolder();
            }
        }
    }, true);

    $scope.$on("$destroy", function () {
        destroyConfigWatch();
    });

    var grid, errorGrid;
    var initialized = false;
    var modifiedRowIndex;

    var pathName = "IndicatorTesterPath";
    var fileExtension = {
        name: 'xml',
        description: L.tsq('XML Config Files')
    };

    $scope.projectStates = SQConstants.getConstants().runningStatuses;
    $scope.progressInfo = {
        state: $scope.projectStates.beforeStart,
        progressPercent: 0
    }

    $scope.engines = SQConstants.getConstants().engines;
    $scope.config = {
        barsToReserve: 300,
        engine: 'MetaTrader4'
    };

    $scope.stopBtnEnabled = false;
    $scope.startBtnEnabled = true;

    $scope.indicatorsChooser = {};
    $scope.currentIndy = {
        parameters: [],
    };
    $scope.dataSeriesOptions = [Ltsq('Open'), Ltsq('High'), Ltsq('Low'), Ltsq('Close'), Ltsq('Volume'), Ltsq('Median'), Ltsq('Typical'), Ltsq('Weighted')];

    $scope.newConfig = false;
    $scope.configFileName = "IndicatorTester.xml";

    $timeout(init, 1000);

    SQWebSocketService.subscribeGeneral("IndicatorTesterCtrl-" + window.appConfig.appCode, onWebSocketData);

});