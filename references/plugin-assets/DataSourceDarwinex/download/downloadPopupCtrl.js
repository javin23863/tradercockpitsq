angular.module('app.datasource.darwinex').controller('darwinexDownloadPopupCtrl', function ($rootScope, $scope, $q, $timeout, SQEvents, DMDataService, DarwinexService, SQConstants, L, AppService) {
    
    var grid;
    var dataSource = angular.copy(SQConstants.getConstants().dataSource);

    $scope.onDateRangeChange = function(dateFrom, dateTo, type) {        
        $scope.newImport.dateFrom = dateFrom;
        $scope.newImport.dateTo = dateTo;
        $scope.newImport.dateType = type;
    }

    $scope.onDownload = function() {
        DarwinexService.downloadData($scope.newImport).then(function(result){
            if(result.success) {
                hidePopup('#darwinexDownloadModal');

                for (var i=0; i<darwinexSymbols.length; i++) {
                    displayGridProgressBar(grid, darwinexSymbols[i].rowIndex, L.tsq("Preparing Darwinex data download"));
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
    var darwinexSymbols = [];

    $scope.action.onSelect = function(symbols, grid2) {
        console.log("darwinexDownloadPopupCtrl")

        grid  = grid2;

        darwinexSymbols = DMDataService.filterSymbolsBySourceType(symbols, dataSource.darwinex);
        if (darwinexSymbols.length==0) {
            $rootScope.showError(L.tsq('You must select at least one Darwinex record.'));
            return;
        }	

        darwinexSymbols = DMDataService.filterSymbolsInProgress(symbols, grid);
        if (darwinexSymbols.length==0) {
            return;
        }

        if(DMDataService.containsClonedData(symbols)) {
            $rootScope.showError(L.tsq('You cannot download to cloned data.'));
            return; 
        }

        $scope.selectedFor = darwinexSymbols.length>1 ? L.tsq('multiple') : "'"+darwinexSymbols[0].symbol+"'";

		var dataInfo = DarwinexService.getSymbolInfo(darwinexSymbols[0].symbol);
		var dateRange = getDateRange(darwinexSymbols[0], dataInfo);

		$scope.newImport = {
			dateFrom : dateRange[0],
			dateTo : dateRange[1],
			dateType: "custom",
			overwrite : "false",
			symbols : implodeObjects(darwinexSymbols, "symbol"),
			darwinexSymbols : implodeObjects(darwinexSymbols, "instrument"),
			datesTo : implodeObjects(darwinexSymbols, "dateTo"),
			uSymbols : getUSymbols(darwinexSymbols),
		}

		//set date range
		$scope.dateRange.setRangeLimit(dataInfo ? timeToDateString(dataInfo.dateFrom) : null, getDateString(new Date()));
		$scope.dateRange.setRange($scope.newImport.dateFrom, $scope.newImport.dateTo, $scope.newImport.dateFrom, true);  

		for (var i=0; i<darwinexSymbols.length; i++) {
			setGridProgressBarAction(grid, darwinexSymbols[i].rowIndex, progressAction);
		}

		try { $scope.$digest(); } catch(err) {}

        showPopup("#darwinexDownloadModal");
    }

    $scope.onUpgradeLicense = function() {
        hidePopup('#darwinexDownloadModal');
        AppService.openBrowser('https://strategyquant.com/quantdatamanager#proversion', function () { });
    }

    function getUSymbols(darwinexSymbols) {
        var str = "";

        for (var i = 0; i < darwinexSymbols.length; i++) {
            var symbol = darwinexSymbols[i]["uSymbol"] ? darwinexSymbols[i]["uSymbol"] : darwinexSymbols[i]["instrument"];
            str += symbol + ",";
        }

        return str.substr(0, str.length - 1);
    }

    function progressAction(rowIndex, row, action) {
        var symbol = grid.getRowId(rowIndex);

        DarwinexService.downloadDataAction(symbol, action);
    }
	
    function onEvent(event, data){
        if(event == SQEvents.get('LICENSE_CHANGED')) {
            $scope.fullLicense = true;//!$rootScope.license.isTrial;
            try { $scope.$digest(); } catch(err) {}
        }
    }		

    //- Initialization --------------------------------------------------------------

    var todaysTime = getDateString(new Date());
    var loaded = false;

    $scope.dateRange = {};

    $scope.newImport = {}; 
	$scope.dataTypeSet = false;
    $scope.dataRanges = SQConstants.getConstants().dataRanges;
    $scope.selectedFor = 'NA';
	
	SQEvents.addListener("DarwinexDownloadPopupCtrl", [SQEvents.get('LICENSE_CHANGED')], onEvent);
});