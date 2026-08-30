angular.module('app.datasource.crypto').controller('cryptoImportPopupCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, DMDataService, DataSourceCryptoService, SQConstants, L, AppService) {
    
    var grid;
    var dataSource = angular.copy(SQConstants.getConstants().dataSource);

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.newImport.dateFrom = dateFrom;
        $scope.newImport.dateTo = dateTo;
        $scope.newImport.dateType = type;
    }

    $scope.onStartImport = function() {
        DataSourceCryptoService.importData($scope.newImport).then(function(result){
            if(result.success){
                hidePopup('#cryptoImportModal');

                for (var i=0; i<cryptoSymbols.length; i++) {
                    displayGridProgressBar(grid, cryptoSymbols[i].rowIndex, L.tsq("Preparing Crypto data download"));
                }

                grid.bodyRedraw();
            }
            else {
                $rootScope.showError(result.data.error);
            }
        })
    }

    function getSymbolsString(array, variable){
        var str = "";

        for(var i=0; i<array.length; i++){
            var symbol = array[i][variable];
            str += symbol + ",";
        }

        return str.substr(0, str.length - 1);
    }

    //- Event Handlers --------------------------------------------------------------

    var cryptoSymbols = [];

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("cryptoImportPopupCtrl")

        grid  = grid2;

        cryptoSymbols = DMDataService.filterSymbolsBySourceType(symbols, dataSource.crypto);
        if (cryptoSymbols.length==0) {
            $rootScope.showError(L.tsq('You must select at least one Crypto record.'));
            return;
        }	

        cryptoSymbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (cryptoSymbols.length==0) {
            return;
        }

        if(DMDataService.containsClonedData(symbols)) {
            $rootScope.showError(L.tsq('You cannot download to cloned data.'));
            return; 
        }

        $scope.selectedFor = cryptoSymbols.length>1 ? L.tsq('multiple') : "'"+cryptoSymbols[0].symbol+"'";

        var dateFrom = new Date();
        dateFrom.setMonth(dateFrom.getMonth() - 6);

        $scope.newImport = {
            dateFrom : cryptoSymbols[0].dateTo,
            dateTo : getDateString(new Date()),
            dateType: "custom",
            overwrite : "false",
            symbols : implodeObjects(cryptoSymbols, "symbol"),
            cryptoSymbols : implodeObjects(cryptoSymbols, "instrument"),
            datesTo : implodeObjects(cryptoSymbols, "dateTo"),
            uSymbols : getUSymbols(cryptoSymbols),
        }

        //set date range
        $scope.dateRange.setRange($scope.newImport.dateFrom, $scope.newImport.dateTo, $scope.newImport.dateFrom, true);  

        for (var i=0; i<cryptoSymbols.length; i++) {
            setGridProgressBarAction(grid, cryptoSymbols[i].rowIndex, progressAction);
        }

        try { $scope.$digest(); } catch(err) {}

        showPopup("#cryptoImportModal");
    }

    function getUSymbols(cryptoSymbols) {
        var str = "";

        for (var i = 0; i < cryptoSymbols.length; i++) {
            var symbol = cryptoSymbols[i]["uSymbol"] ? cryptoSymbols[i]["uSymbol"] : cryptoSymbols[i]["instrument"];
            str += symbol + ",";
        }

        return str.substr(0, str.length - 1);
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DataSourceCryptoService.importDataAction(symbol, action);
    }

    //- Initialization --------------------------------------------------------------

    var loaded = false;

    $scope.dateRange = {};

    $scope.newImport = {}; 
    $scope.dataRanges = SQConstants.getConstants().dataRanges;
    $scope.selectedFor = 'NA';
});