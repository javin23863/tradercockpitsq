angular.module('app.data').controller('ExportToMT5Ctrl', function ($rootScope, $scope, $q, SQEvents, SQConstants, ExportToMT5Service, $timeout, DMDataService, L) {
    $scope.TF = SQConstants.getConstants().timeframe;

    $scope.dataDetails = {
        dateFrom : null,
        dateTo : null,
        dateType: "custom",
        timeframe: null,
        spread: null,
        spreadValue: 3,
        spreadPoints: 10,
        targetTimezone: ''
    };
    $scope.fileExtension = '.csv';

    $scope.dateRange = {};
    $scope.timezones = [];
    
    var dataSource = angular.copy(SQConstants.getConstants().dataSource);
    
    var grid;
    var actionSymbol = null;

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("ExportToMT5Ctrl");

        grid = grid2;

        if (symbols.length==0) {
            $rootScope.showError(L.tsq('You have to select some symbol.'));
            return;
        }

        if (symbols.length>1) {
            $rootScope.showError(L.tsq('You can choose only one symbol to export.'));
            return;
        }

        symbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (symbols.length==0) {
            return;
        }

        var symbolInfo = symbols[0];
        if (symbolInfo.totalRecords == 0) {
            $rootScope.showError(L.tsq("Symbol '%s' doesn't contain any data", [symbolInfo.symbol]));
            return;
        }

        if (symbolInfo.timeframe != $scope.TF.TICK && symbolInfo.timeframe != $scope.TF.M1) {
            $rootScope.showError(L.tsq("Export to MT5 works only with TICK and M1 data."));
            return;
        }

        actionSymbol = symbols[0];
        $scope.isTickData = symbolInfo.timeframe == $scope.TF.TICK;

        $scope.dataDetails.spread = symbolInfo.timeframe == $scope.TF.TICK ? "real" : "pips";

        $scope.dataDetails.connection = symbolInfo.connection;
        $scope.dataDetails.symbol = symbolInfo.symbol;
        $scope.dataDetails.timeframe = symbolInfo.timeframe;

        $scope.dataDetails.directory = SQConstants.getSettings()['ExportToMT5'] || SQConstants.getSettings().projectsPath;
        $scope.dataDetails.fileName = symbols[0].symbol;

        $scope.dataDetails.dateFrom = symbolInfo.dateFrom;
        $scope.dataDetails.dateTo = symbolInfo.dateTo;
        
        $scope.dateRange.setRangeLimit($scope.dataDetails.dateFrom, $scope.dataDetails.dateTo);
        $scope.dateRange.setRange($scope.dataDetails.dateFrom, $scope.dataDetails.dateTo);  

        setGridProgressBarAction(grid, actionSymbol.rowIndex, progressAction);

        showPopup('#exportToMT5Modal');
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        var confirmed = false;

        if(action=='stop') {
            $rootScope.showConfirm(L.tsq("Cancel export"), L.tsq("Do you want to keep the export file?"), function (confirmed) {
                confirmed = !confirmed;

                ExportToMT5Service.action(symbol, action, confirmed);
            }, true);
        } else {
            ExportToMT5Service.action(symbol, action, confirmed);
        }
    }

    $scope.getTimezoneLabel = function(){
        var tz = getItem($scope.timezones, 'value', $scope.dataDetails.targetTimezone);        
        return tz ? tz.name : 'Original';
    }

    $scope.onExport = function () {
        if (!valueFilled($scope.dataDetails.directory)) {
            $rootScope.showError(L.tsq("Directory cannot be empty."));
            return;
        }

        if (!valueFilled($scope.dataDetails.fileName)) {
            $rootScope.showError(L.tsq("File name cannot be empty."));
            return;
        }

        var filePath = $scope.dataDetails.directory + "/" + $scope.dataDetails.fileName + ".csv";
        $rootScope.checkFileExists(filePath, function(result) {
            if(result.exists) {
                $rootScope.showConfirm("Export to MT5", "File '"+filePath+"' already exists, do you want to overwrite it?", function (confirmed) {
                    if(confirmed) {
                        startExport();
                    }
                }, true);   
            } else {
                startExport();
            }
        });
    }

    function startExport() {
        ExportToMT5Service.export($scope.dataDetails, function (result) {
            hidePopup('#exportToMT5Modal');

            displayGridProgressBar(grid, actionSymbol.rowIndex, L.tsq("Preparing MT5 export to CSV"), true);
        });
    }

    $scope.openExportFilePicker = function () {
        var fileExtension = { name : 'csv', description : L.tsq('Comma Separated File') };
        
        $rootScope.showFilePicker(L.tsq("Select export file"), "ExportToMT5", false, false, $scope.dataDetails.directory, fileExtension, null, function (paths) {
            if (paths) {
                $scope.dataDetails.directory = paths[0];

                try { $scope.$digest(); } catch (er) {}
            }
        });
    }
    
     function initTimezones(){
        var tzList = SQConstants.getConstants().timezones;
        var newTimezones = [];
        newTimezones.push({value:'',name:'Original'});
        for(var i=0;i<tzList.length;i++){
            newTimezones.push(tzList[i]);
        }
        $scope.timezones = newTimezones;
    }

    initTimezones();

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.dataDetails.dateFrom = dateFrom;
        $scope.dataDetails.dateTo = dateTo;
        $scope.dataDetails.dateType = type;
    }
});

               