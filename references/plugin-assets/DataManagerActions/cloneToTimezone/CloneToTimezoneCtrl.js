angular.module('app.data').controller('CloneToTimezoneCtrl', function ($scope, $rootScope, SQConstants, $timeout, DMDataService, L) {
    console.log("CloneToTimezone controller initialized");    

    var grid;
    var actionSymbols = [];

    $scope.errors = "";
    $scope.timezones = SQConstants.getConstants().timezones;
    
    var dataSource = angular.copy(SQConstants.getConstants().dataSource);

    $scope.config = {
        postfix: null,
        cloneTimezoneType: 0,
        cloneTimezoneShift: 5,
        cloneTimezone: getDefaultTimezone(),
        removeWeekends: false
    };

    $scope.selectedFor = null;

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("CloneToTimezoneCtrl");

        grid = grid2;
        actionSymbols = symbols;

        if (symbols.length==0) {
            $rootScope.showError(L.tsq('You have to select some symbol.'));
            return;
        }

        symbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (symbols.length==0) {
            return;
        }

        if(symbols.length==1) {
            if(DMDataService.containsClonedData(symbols)) {
                $rootScope.showError(L.tsq('This is cloned data. You cannot clone it again.'));
                return; 
            }

            $scope.selectedFor =  symbols[0].symbol;
        } else {
            if(!checkClonedData(symbols)) {
                return; 
            }

            $scope.selectedFor =  L.tsq('multiple');
        }   

        $scope.config.postfix = "_{timeframe}_{cloneTime}";
        $scope.config.symbols = implodeObjects(symbols, "symbol");

        for (var i=0; i<symbols.length; i++) {
            setGridProgressBarAction(grid, symbols[i].rowIndex, progressAction);
        }

        showPopup('#cloneToTimezoneModal');
    }

    function checkClonedData(symbols) {
        for (var i = 0; i < symbols.length; i++) {
            if(symbols[i].sourceDataId) {
                $rootScope.showError(L.tsq("'%s' is cloned data. You cannot clone it again.", [symbols[i].symbol]));
                return false;
            }
        }

        return true;
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DMDataService.cloneToTimezoneAction(symbol, action);
    }

    function getDefaultTimezone() {
        if($scope.timezones.length>0) {
            return $scope.timezones[0].value;
        }
    }

    $scope.getCloneTimezoneLabel = function(){
        var tz = getItem($scope.timezones, 'value', $scope.config.cloneTimezone);        
        return tz ? tz.name : '';
    }

    $scope.onCloneToTimezone = function(form) {
        if (form) {
            $scope.errors = validate(form);
            if ($scope.errors) {
                console.error($scope.errors);
                return;
            }
        }

        if (!valueFilled($scope.config.postfix)) {
            $rootScope.showError(L.tsq("Symbol postfix cannot be empty."));
            return;
        }

        DMDataService.cloneToTimezone($scope.config, function (result) {
            hidePopup('#cloneToTimezoneModal');

            for (var i=0; i<actionSymbols.length; i++) {
                displayGridProgressBar(grid, actionSymbols[i].rowIndex, L.tsq("Preparing clone to timezone"));
            }

            grid.bodyRedraw();
        });
    }

    function loadLastSettings() {
        var settings = SQConstants.getSettings();
        $scope.config.removeWeekends =  stringToBoolean(settings.CloneRemoveWeekends);

        if(settings.CloneTimezone) {
            if(checkIntegerValue(settings.CloneTimezone)) { //hours
                $scope.config.cloneTimezoneType = 0; 
                $scope.config.cloneTimezoneShift = parseInt(settings.CloneTimezone)

            } else if(getItem($scope.timezones, 'value', settings.CloneTimezone)){ //timezone
                $scope.config.cloneTimezoneType = 1; 
                $scope.config.cloneTimezone = settings.CloneTimezone;
            }
        } 

        try { $scope.$digest(); } catch (err) { }
    }

    loadLastSettings();
});