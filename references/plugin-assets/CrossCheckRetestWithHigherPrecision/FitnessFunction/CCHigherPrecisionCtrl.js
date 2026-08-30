angular.module('app').controller('CCHigherPrecisionCtrl', function ($rootScope, $scope, $timeout, $interval, $q, CCHigherPrecisionService, UniqueIdGenerator, RankingService, AppService, L, SQConstants) {
    console.log("CCHigherPrecision controller initialized");

    $scope.weightedGridId = UniqueIdGenerator.generateId();
    $scope.typeSelected = null;
    $scope.types = [];
    
    var valueTypes = SQConstants.getConstants().valueTypes;
    
    var grid;
    var gridDeferred = $q.defer();
    var intervalPromise = null;

    CCHigherPrecisionService.list(function(data) {
        $scope.types = data.types;

        if($scope.types.length>0) {
            $scope.typeSelected = $scope.types[0].key;
        }

        try { $scope.$digest(); } catch(err){}

        intervalPromise = $interval(tryInitGrid, 50, false);     
    }); 

    function tryInitGrid(){
        var element = angular.element('#' + $scope.weightedGridId);
        if(element[0]){
            $interval.cancel(intervalPromise);
            initGrid();
        }
    }

    function initGrid() {
        if (!grid) {
            grid = new sqGrid($scope.weightedGridId);
        } else {
            grid.removeAllRows(true);
        }

        var columns = [
            { title: L.tsq('Ranking Criterium'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Type'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Weight'), type: "text", sort: 'text', align: 'left' },
            { title: L.tsq('Target'), type: "text", sort: 'text', align: 'left' }
        ];
        var widths = [ '*', 72, 80, 90 ];

        grid.enableCheckboxes(true);
        grid.setColumns(columns, !!grid);
        grid.setWidths(widths, !!grid);
        grid.setEmptyGridText(L.tsq('No items available.'));

        grid.defineWidget('spinnerWidget', sqGridSpinner);
        grid.defineWidget('selectWidget', sqGridSelect);

        var type = getTypeByKey('Weighted');
        if(!type) {
            return;
        }

        grid.loadFromJSON(type);

        for (var row_ind=0; row_ind<grid.getNumberOfRows(); row_ind++) {
            var valueType = grid.getUserData(row_ind, "type");

            if(valueType != valueTypes.aproximate) {
                grid.setCellDisabled(row_ind, 3, true, true);
            }
        }

        grid.onSelectionChanged = function (rowIndexes, values) {
            saveSettings();
        };

        grid.cellEventHandler = function (rowIndex, cellIndex, eventName, args) {
            switch (eventName) {
                case "spinnerUpdated": onGridSpinnerChange(rowIndex, cellIndex, args[0]); break;
                case "selectUpdated":  onGridSelectChange(rowIndex, cellIndex, args[0]); break;
            }
        };

        grid.headerRedraw();
        grid.bodyRedraw();

        gridDeferred.resolve();
    }

    function onGridSelectChange(rowIndex, colIndex, value) {
        checkColumns();
        saveSettings();
    }

    function checkColumns() {
        for (var row_ind = 0; row_ind < grid.getNumberOfRows(); row_ind++) {
            var valueType = sqGridSelectValueGet(grid, row_ind, 1);

            if(valueType != valueTypes.aproximate) {
                grid.setCellDisabled(row_ind, 3, true, true);
                sqGridSpinnerValueSet(grid, row_ind, 3, 0);
            } else {
                grid.setCellDisabled(row_ind, 3, false, true);
            }
            
        }

        grid.bodyRedraw();
    }

    function onGridSpinnerChange(rowIndex, colIndex, value) {
        if(value == parseFloat(value)) {
            if(value < 0) {
                grid.modifyRowByIndex(0, rowIndex, colIndex);
            }
        }
        else {
            $rootScope.showError(L.tsq("%s must be a number", [colIndex == 2 ? L.tsq("Weight") : L.tsq("Target")]));
            grid.modifyRowByIndex(1, rowIndex, colIndex);
        }
        
        saveSettings();
    }

    $scope.onTypeChange = function(key) {
        var type = getTypeByKey(key);
        if(!type) {
            return;
        }

        $scope.typeSelected = key;

        saveSettings();
        setTimeout(function () {window.dispatchEvent(new Event('resize'));}, 0);
    }

    function getTypeByKey(key) {
        if(!key) {
            return key;
        }

        for(var i=0; i<$scope.types.length; i++){
            var type = $scope.types[i];

            if(type.key == key){
                return type;
            }
        }
    }

    function correctNumber(value, defaultValue){
        if(parseFloat(value) == value) return value;
        else return defaultValue;
    }   

    $scope.$on("$destroy", function(){
        UniqueIdGenerator.removeId($scope.weightedGridId);
    });

    function getRowIndexByType(key) {
        for(var row_ind=0; row_ind<grid.getNumberOfRows(); row_ind++) {
            if(key == grid.getUserData(row_ind, "key")) {
                return row_ind;
            }
        }

        return -1;
    }

    function saveSettings() {
        $scope.method.onFitnessChange($scope.method);
    }

    $scope.method.resetSettings = function() {
        resetGoals();
    }

    function resetGoals() {
        gridDeferred.promise.then(function() {
            for (var row_ind = 0; row_ind < grid.getNumberOfRows(); row_ind++) {
                var valueType = grid.getUserData(row_ind, "type");
                var target = grid.getUserData(row_ind, "target");

                grid.setRowChecked(row_ind, false);
                sqGridSelectValueSet(grid, row_ind, 1, valueType, true);
                sqGridSpinnerValueSet(grid, row_ind, 2, 1, true);

                if (valueType == valueTypes.aproximate) {
                    sqGridSpinnerValueSet(grid, row_ind, 3, target, true);
                }
            }
            
            grid.bodyRedraw();
        });
    }

    $scope.method.getSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var settingsObj =  xmlDoc.createElement('Settings');
        
        var rankingObj = settingsObj.appendChild(xmlDoc.createElement('Ranking'));
        rankingObj.setAttribute("type", $scope.typeSelected);

        if($scope.typeSelected=='Weighted') {
            for (var row_ind=0; row_ind<grid.getNumberOfRows(); row_ind++) {
                var use = grid.isRowChecked(row_ind);
                var key = grid.getUserData(row_ind, "key");
                var valueType = sqGridSelectValueGet(grid, row_ind, 1);
                var weight = sqGridSpinnerValueGet(grid, row_ind, 2);
                var target = sqGridSpinnerValueGet(grid, row_ind, 3);

                var goalObj =  xmlDoc.createElement('Goal');

                goalObj.setAttribute("use", use);
                goalObj.setAttribute("type", key);
                goalObj.setAttribute("weight", weight);
                goalObj.setAttribute("valueType", valueType);

                if(valueType == valueTypes.aproximate) {
                    goalObj.setAttribute("target", sqGridSpinnerValueGet(grid, row_ind, 3));
                } else {
                    goalObj.setAttribute("target", 0);
                }

                rankingObj.appendChild(goalObj);
            }
        }

        return settingsObj;
    }

    $scope.method.applySettings = function(settingsObj) {
        if(!settingsObj || settingsObj.nodeName!='Settings') {
            return;
        }

        var rankingObj = getChildElement(settingsObj, 'Ranking');
        var key = getAttrStringValue(rankingObj, "type", null);

        var type = getTypeByKey(key);
        if(!type) {
            return;
        }

        $scope.typeSelected = key;

        if(key=='Weighted') {
            gridDeferred.promise.then(function(){
                var goalElements = getChildElements(rankingObj, 'Goal');

                for(var i=0; i<goalElements.length; i++) {
                    var goalEl = goalElements[i];

                    var type = getAttrValue(goalEl, "type", null);
                    var row_ind = getRowIndexByType(type);
                    if(row_ind==-1) {
                        continue;
                    }
                    
                    grid.setRowChecked(row_ind, getAttrBooleanValue(goalEl, "use", false));
                    sqGridSpinnerValueSet(grid, row_ind, 2, correctNumber(getAttrFloatValue(goalEl, "weight", 1), 1), true);

                    var valueType = grid.getUserData(row_ind, "type");
                    valueType = getAttrIntValue(goalEl, "valueType", valueType);
                    sqGridSelectValueSet(grid, row_ind, 1, valueType, true);

                    if(valueType == valueTypes.aproximate) {
                        sqGridSpinnerValueSet(grid, row_ind, 3, correctNumber(getAttrIntValue(goalEl, "target", 0), 0), true);
                    }
                }

                checkColumns();
                grid.bodyRedraw();
            });
        }
    }
});