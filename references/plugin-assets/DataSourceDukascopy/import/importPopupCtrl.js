angular.module('app.datasource.dukascopy').controller('dukasImportPopupCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, DMDataService, DukascopyService, SQConstants, L, AppService) {
    
    var grid;
    var dataSource = angular.copy(SQConstants.getConstants().dataSource);

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.newImport.dateFrom = dateFrom;
        $scope.newImport.dateTo = dateTo;
        $scope.newImport.dateType = type;
    }

    $scope.onStartImport = function() {
        if($scope.newImport.downloadType != 'standard' && AppService.isTrial()) {
            $rootScope.showConfirm(L.tsq("Fast Data Download"), L.tsq("Fast downloading is possible only in full version or for some specific symbols in M1 timeframe"), function(confirmed){
                if(confirmed) {
                    startImport();
                }
            }, false);
        } else {
            startImport();
        }
    }

    function startImport() {
        DukascopyService.importData($scope.newImport).then(function(result){
            if(result.success){
                hidePopup('#dukasImportModal');

                for (var i=0; i<dukasSymbols.length; i++) {
                    displayGridProgressBar(grid, dukasSymbols[i].rowIndex, L.tsq("Preparing Dukascopy data import"));
                }

                grid.bodyRedraw();
            }
            else {
                $rootScope.showError(result.data.error);
            }
        })
    }

    $scope.onUpgradeLicense = function() {
        hidePopup('#dukasImportModal');
        AppService.openBrowser('https://strategyquant.com/quantdatamanager#proversion', function () { });
    }

    function getSymbolsString(array, variable){
        var str = "";

        for(var i=0; i<array.length; i++){
            var symbol = array[i][variable];
            str += symbol + ",";
        }

        return str.substr(0, str.length - 1);
    }

    function getDateRange(symbol, dataInfo) {
        var dateFrom = getDateString(new Date());

        if(dataInfo) {
            dateFrom = timeToDateString(dataInfo.dateFrom);
            if(symbol.dateTo) {
                dateFrom = symbol.dateTo;
            }
        }

        return [dateFrom, todaysTime];
    }

    //- Event Handlers --------------------------------------------------------------

    var dukasSymbols = [];

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("dukasImportPopupCtrl")

        grid  = grid2;

        dukasSymbols = DMDataService.filterSymbolsBySourceType(symbols, dataSource.dukascopy);
        if (dukasSymbols.length==0) {
            $rootScope.showError(L.tsq('You must select at least one Dukascopy record.'));
            return;
        }	

        dukasSymbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (dukasSymbols.length==0) {
            return;
        }

        if(DMDataService.containsClonedData(symbols)) {
            $rootScope.showError(L.tsq('You cannot download to cloned data.'));
            return; 
        }

        $scope.selectedFor = dukasSymbols.length>1 ? L.tsq('multiple') : "'"+dukasSymbols[0].symbol+"'";

        var dataInfo = DukascopyService.getSymbolInfo(dukasSymbols[0].symbol);
        var dateRange = getDateRange(dukasSymbols[0], dataInfo);
        
        $scope.newImport = {
            dateFrom : dateRange[0],
            dateTo : dateRange[1],
            dateType: "custom",
            overwrite : "false",
            downloadType: 'standard',
            symbols : implodeObjects(dukasSymbols, "symbol"),
            dukasSymbols : implodeObjects(dukasSymbols, "instrument"),
            datesTo : implodeObjects(dukasSymbols, "dateTo"),
            uSymbols : getUSymbols(dukasSymbols),
        }

        if(!AppService.isTrial()) {
            if(SQConstants.getSettings().cdnPreferred) {
                $scope.newImport.downloadType = SQConstants.getSettings().cdnPreferred;
            } else {
                $scope.newImport.downloadType = 'cdn';
            }
        }

        $scope.cdnAllowed = true;//!AppService.isTrial();

        //set date range
        $scope.dateRange.setRangeLimit(dataInfo ? timeToDateString(dataInfo.dateFrom) : null, getDateString(new Date()));
        $scope.dateRange.setRange($scope.newImport.dateFrom, $scope.newImport.dateTo, $scope.newImport.dateFrom, true);  

        for (var i=0; i<dukasSymbols.length; i++) {
            setGridProgressBarAction(grid, dukasSymbols[i].rowIndex, progressAction);
        }

        try { $scope.$digest(); } catch(err) {}

        showPopup("#dukasImportModal");
    }

    function getUSymbols(dukasSymbols) {
        var str = "";

        for (var i = 0; i < dukasSymbols.length; i++) {
            var symbol = dukasSymbols[i]["uSymbol"] ? dukasSymbols[i]["uSymbol"] : dukasSymbols[i]["instrument"];
            str += symbol + ",";
        }

        return str.substr(0, str.length - 1);
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DukascopyService.importDataAction(symbol, action);
    }

    function onEvent(event, data){
        if(event == SQEvents.get('LICENSE_CHANGED')) {
            $scope.displayUpgradeLink = (isQuantDataManager() && !AppService.isProVersion());

            try { $scope.$digest(); } catch(err) {}
        }
    }

    //- Initialization --------------------------------------------------------------

    var todaysTime = getDateString(new Date());
    var loaded = false;

    $scope.dateRange = {};

    $scope.newImport = {}; 
    $scope.dataRanges = SQConstants.getConstants().dataRanges;
    $scope.selectedFor = 'NA';

    $scope.displayUpgradeLink = true;
    SQEvents.addListener("DukasImportPopupCtrl", [SQEvents.get('LICENSE_CHANGED')], onEvent);
});