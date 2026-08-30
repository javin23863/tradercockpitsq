angular.module('app').controller('IndicatorsCalibrationPopupCtrl', function ($rootScope, $scope, L, SQEvents, BuildTaskService, usSpinnerService, $timeout) {
    console.log("IndicatorsCalibrationPopupCtrl controller initialized");

    function init() {
        var columns = [{
            title: L.tsq('Indicator'),
            type: "text",
            sort: 'text',
            align: 'left',
            width: "45%",
        },
        {
            title: L.tsq('Minimum'),
            type: "text",
            sort: 'number',
            align: 'left',
            width: "17%",
        },
        {
            title: L.tsq('Maximum'),
            type: "text",
            sort: 'number',
            align: 'left',
            width: "17%",
        },
        {
            title: L.tsq('Step'),
            type: "text",
            sort: 'number',
            align: 'left',
            width: "17%",
        }
        ];

        grid = new sqGrid("indicatorRangesGrid");
        grid.setFirstColumnAsId(false, false);
        grid.enableCheckboxes(false);
        grid.setColumns(columns, !!grid);
        grid.setEmptyGridText(L.tsq('No indicators available.'));
        grid.disableSorting();

        grid.defineWidget('spinnerWidget', sqGridSpinner);

        grid.headerRedraw();
    }

    function loadData(callback) {               //callback is used when calibrating automatically before Builder start
        if (!BlocksService.gui.initialized) {
            BlocksService.init();
            BlocksService.loadSettings(function () {
                $scope.config.stepType = BlocksService.gui.useCalibrationMaxSteps ? "custom" : "default";
                $scope.config.maxSteps = BlocksService.gui.calibrationMaxSteps;

                refreshGrid(getBlocksServiceData());

                $scope.$evalAsync();

                if(callback) callback();
            });
        } 
        else {
            $scope.config.stepType = BlocksService.gui.useCalibrationMaxSteps ? "custom" : "default";
            $scope.config.maxSteps = BlocksService.gui.calibrationMaxSteps;

            refreshGrid(getBlocksServiceData());

            $scope.$evalAsync();
            
            if(callback) callback();
        }
    }

    function getBlocksServiceData() {
        var indicators = angular.copy(BlocksService.indicators);

        for(var i=0; i<BlocksService.stopLimitBlocks.length; i++){
            indicators.push(BlocksService.stopLimitBlocks[i]);
        }

        var calibrationData = [];
        var usedKeys = [];

        for (var i = 0; i < indicators.length; i++) {
            var indicator = indicators[i];

            if (!shouldUseIndicator(indicator)) continue;

            var keyParts = indicator.key.split(".");
            var key = keyParts[keyParts.length - 1];

            if (usedKeys.indexOf(key) >= 0 || key == 'Number' || key == 'FixedPips') continue;

            calibrationData.push({
                key: key,
                name: indicator.name,
                ranges: [
                    { minValue: indicator.indicatorMin, maxValue: indicator.indicatorMax, step: indicator.indicatorStep }
                ]
            });

            usedKeys.push(key);
        }

        return calibrationData;
    }

    function shouldUseIndicator(indicator) {
        if ((indicator.group != "Indicators" && indicator.group != "Stop/Limit Price Ranges") || !valueFilled(indicator.key)) return false;

        var returnType = indicator.returnType;
        return returnType == 'number' || returnType == 'pricerange';
    }

    function refreshGrid(calibrationData) {
        for (var i = 0; i < calibrationData.length; i++) {
            var dataObj = calibrationData[i];
            var mainRange = dataObj.ranges[0];
            
            var rowIndex = findIndicatorRow(dataObj.key);
            let minValue = mainRange.minValue ? mainRange.minValue : 100;
            let maxValue = mainRange.maxValue ? mainRange.maxValue : 200;
            let step = ($scope.config.stepType == "custom" ? mainRange.step : getDefaultStep(minValue, maxValue));
            if(!step){
                step = (maxValue - minValue) / $scope.config.maxSteps
            }
            if (rowIndex < 0) {
                var rowData = [
                    dataObj.name,
                    "{{spinnerWidget value='" + minValue + "' min='-999999999' max='999999999'}}",
                    "{{spinnerWidget value='" + maxValue + "' min='-999999999' max='999999999'}}",
                    "{{spinnerWidget value='" + step + "' min='-999999999' max='999999999'}}",
                ];

                grid.addRow(rowData, true);
                grid.setUserData(grid.getNumberOfRows() - 1, "blockKey", dataObj.key);
            }
            else {
                sqGridSpinnerValueSet(grid, rowIndex, 1, minValue, true);
                sqGridSpinnerValueSet(grid, rowIndex, 2, maxValue, true);

                if($scope.config.stepType == "custom"){
                    sqGridSpinnerValueSet(grid, rowIndex, 3, step, true);
                }
            }
        }

        grid.bodyRedraw(true);
    }

    function checkStep(step) {
        return valueFilled(step) && step != 'NaN' && parseFloat(step) != 0;
    }

    function findIndicatorRow(indicatorKey) {
        for (var i = 0; i < grid.getNumberOfRows(); i++) {
            var rowKey = grid.getUserData(i, "blockKey");
            if (rowKey == indicatorKey) return i;
        }

        return -1;
    }

    function getDefaultStep(min, max) {
        try {
            min = parseFloat(min);
            max = parseFloat(max);
            
            var decimals = Math.max(countDecimals(max), countDecimals(min));

            return trimToDecimalPlaces((max - min) / 10, decimals + 1);
        }
        catch (err) {
            return 0;
        }
    }

    function getCalibrationDataFromGrid() {
        var calibrationData = [];

        for (var i = 0; i < grid.getNumberOfRows(); i++) {
            calibrationData.push({
                key: grid.getUserData(i, "blockKey"),
                ranges: [{
                    minValue: sqGridSpinnerValueGet(grid, i, 1),
                    maxValue: sqGridSpinnerValueGet(grid, i, 2),
                    step: sqGridSpinnerValueGet(grid, i, 3)
                }]
            });
        }

        return calibrationData;
    }

    $rootScope.showIndicatorsCalibrationSettings = function (BlocksServiceRef, DataServiceRef, productCode) {
        BlocksService = BlocksServiceRef;
        DataService = DataServiceRef;

        $scope.config.isBuilder = productCode == "BUILDER";

        loadData(null);

        showPopup("#indicatorsCalibrationPopup");
    }

    $rootScope.calibrateBeforeStart = function(BlocksServiceRef, DataServiceRef, callback){
        BlocksService = BlocksServiceRef;
        DataService = DataServiceRef;

        $scope.config.isBuilder = true;

        loadData(function(){
            if(!BlocksService.gui.autoCalibrateBeforeStart || BlocksService.gui.isStockPicker){
                callback();   //skip calibration
                return;
            }

            $scope.onCalibrate(function(){
                $scope.onSave(callback);
            });
        });
    }

    $scope.onCalibrate = function (callback) {              //callback is used when calibrating automatically before Builder start
        DataService.loadDataConfig();

        var mainSetup = DataService.config.setups[0];
        if (!mainSetup || !mainSetup.symbol) {
            $rootScope.showError(L.tsq("Indicator calibration failed - Main setup symbol not defined"));
            return;
        }

        var mainSetupSymbol = mainSetup.symbol;
        var symbols = mainSetupSymbol;
        var timeframes = mainSetup.timeframe;
        var engine = mainSetup.engine;

        for (var i = 0; i < mainSetup.subcharts.length; i++) {
            var symbol = mainSetup.subcharts[i].symbol;

            symbols += "," + (symbol == "Same as main chart" ? mainSetupSymbol : symbol);
            timeframes += "," + mainSetup.subcharts[i].timeframe;
        }

        $scope.calibrating = true;
        usSpinnerService.spin($scope.spinnerKey);

        var maxSteps = -1;
        if($scope.config.stepType == "custom"){
            maxSteps = $scope.config.maxSteps;
        }

        BuildTaskService.calibrate(symbols, timeframes, maxSteps, engine, function (result) {
            $rootScope.showSuccess(L.tsq("Calibration done"));

            usSpinnerService.stop($scope.spinnerKey);
            $scope.calibrating = false;

            refreshGrid(result.calibrationResults);

            if(callback) callback();
        });
    }

    $scope.onSave = function (callback) {               //callback is used when calibrating automatically before Builder start
        $scope.calibrating = true;
        usSpinnerService.spin($scope.spinnerKey);

        BlocksService.gui.useCalibrationMaxSteps = $scope.config.stepType == "custom";
        BlocksService.gui.calibrationMaxSteps = $scope.config.maxSteps;

        $timeout(function () {
            BlocksService.calibrateBlocks(getCalibrationDataFromGrid(), callback);

            hidePopup("#indicatorsCalibrationPopup");

            usSpinnerService.stop($scope.spinnerKey);
            $scope.calibrating = false;

            if(callback) {
                callback();
            }
            else {
                $rootScope.showSuccess(L.tsq("Settings saved"));
            }
        }, 0);
    }

    var grid;
    var DataService;
    var BlocksService;

    $scope.spinnerKey = "indy-calibration-spinner";
    $scope.calibrating = false;

    $scope.config = {};

    init();

});