angular.module('app.datasource.yahoo').controller('yahooDownloadPopupCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, DMDataService, YahooService, SQConstants, L, AppService) {
    
    var grid;
    var dataSource = angular.copy(SQConstants.getConstants().dataSource);

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.newImport.dateFrom = dateFrom;
        $scope.newImport.dateTo = dateTo;
        $scope.newImport.dateType = type;
    }

    $scope.onStartImport = function() {
        YahooService.importData($scope.newImport).then(function(result){
            if(result.success){
                hidePopup('#yahooImportModal');

                for (var i=0; i<yahooSymbols.length; i++) {
                    displayGridProgressBar(grid, yahooSymbols[i].rowIndex, L.tsq("Preparing Yahoo data download"));
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

    var yahooSymbols = [];

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("yahooDownloadPopupCtrl")

        grid  = grid2;

        yahooSymbols = DMDataService.filterSymbolsBySourceType(symbols, dataSource.yahoo);
        if (yahooSymbols.length==0) {
            $rootScope.showError(L.tsq('You must select at least one Yahoo record.'));
            return;
        }	

        yahooSymbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (yahooSymbols.length==0) {
            return;
        }

        if(DMDataService.containsClonedData(symbols)) {
            $rootScope.showError(L.tsq('You cannot download to cloned data.'));
            return; 
        }

        $scope.selectedFor = yahooSymbols.length>1 ? L.tsq('multiple') : "'"+yahooSymbols[0].symbol+"'";

        var dateFrom = new Date();
        dateFrom.setMonth(dateFrom.getMonth() - 6);

        $scope.newImport = {
            dateFrom : getDateString(dateFrom),
            dateTo : getDateString(new Date()),
            dateType: "custom",
            overwrite : "false",
            symbols : implodeObjects(yahooSymbols, "symbol"),
            yahooSymbols : implodeObjects(yahooSymbols, "instrument"),
            datesTo : implodeObjects(yahooSymbols, "dateTo"),
            uSymbols : getUSymbols(yahooSymbols),
        }

        //set date range
        $scope.dateRange.setRange($scope.newImport.dateFrom, $scope.newImport.dateTo, $scope.newImport.dateFrom, true);  

        for (var i=0; i<yahooSymbols.length; i++) {
            setGridProgressBarAction(grid, yahooSymbols[i].rowIndex, progressAction);
        }

        try { $scope.$digest(); } catch(err) {}

        showPopup("#yahooImportModal");
    }

    function getUSymbols(yahooSymbols) {
        var str = "";

        for (var i = 0; i < yahooSymbols.length; i++) {
            var symbol = yahooSymbols[i]["uSymbol"] ? yahooSymbols[i]["uSymbol"] : yahooSymbols[i]["instrument"];
            str += symbol + ",";
        }

        return str.substr(0, str.length - 1);
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        YahooService.importDataAction(symbol, action);
    }

    //- Initialization --------------------------------------------------------------

    var loaded = false;

    $scope.dateRange = {};

    $scope.newImport = {}; 
    $scope.dataRanges = SQConstants.getConstants().dataRanges;
    $scope.selectedFor = 'NA';
});