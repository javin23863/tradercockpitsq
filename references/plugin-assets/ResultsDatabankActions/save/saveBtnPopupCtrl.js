angular.module('app').controller('saveBtnPopupCtrl', function ($rootScope, $scope, $timeout, DatabankService, SQEvents, SQConstants, SaveButtonService, AppService, L){

    $scope.openLastDir = function(){
        $rootScope.showFilePicker(L.tsq("Select target folder"), pathName, false, false, null, null, null, function(paths){
            $scope.config.folder = paths[0];

            try { $scope.$digest(); } catch(err){}
        });
    }
    
    $scope.saveReports = function(){
        $scope.config.strategies = AppService.getSelectedStrategies();
        $scope.config.fileName = valueFilled($scope.config.fileName) ? $scope.config.fileName : 'not set';

        SaveButtonService.save($scope.config, function(result){
            if(result.success != "Canceled") $rootScope.showSuccess(result.success);  
            hidePopup("#saveStrategyResultsModal"); 
        });
    }
    
    function getFilePrefix(){
        return getDateString(new Date());
    }
    
    function pad(str, max) {
        str = str.toString();
        return str.length < max ? pad("0" + str, max) : str;
    }

    function onClick(config) {
        angular.merge($scope.config, config);

        if(config.extension && ("" + config.extension.constructor).indexOf("Array") >= 0){
            $scope.config.extensions = config.extension;
            $scope.config.extension = $scope.config.extensions[0];
            $scope.config.selectedExtension = $scope.config.extension.name;
        }
        else {
            $scope.config.extensions = [];
        }

        pathName = "databankSelectFile-" + $scope.config.extension.name;
        $scope.config.pathName = pathName;
        
        $scope.config.folder = SQConstants.getSettings()[pathName] || SQConstants.getSettings()["projectsPath"];
        $scope.config.fileName = '';

        var selectedStrategies = AppService.getSelectedStrategies();
        if(!selectedStrategies){
            $rootScope.showError(L.tsq("You must select at least one strategy"));
            return;
        }

        $scope.config.exportTradesActive = $scope.config.action == 'ExportStrategyTrades';

        if($scope.config.exportTradesActive) {
            $scope.popupTitle = L.tsq("Export strategy trades to CSV/XLSX");
            $scope.config.extension.name = $scope.config.format;    
        } 
        else {
            $scope.popupTitle = L.tsq("Save strategies as %s", [$scope.config.extension.description]);
        }

        var selectedRows = selectedStrategies.split(",");

        if(selectedRows.length > 1 || (selectedRows.length == 1 && selectedRows[0] == 'all') || $scope.config.exportTradesActive){
            $scope.singleResults = false;
        } 
        else {
            $scope.singleResults = true;
            $scope.config.fileName = selectedRows[0]; 

            if(window.electron) {
                SaveButtonService.saveSingleFileJava($scope.config);
                return;
            }
        }

        $scope.showMagicNumberSettings = $scope.config.extension.name != 'xml' && $scope.config.extension.name != 'csv' && $scope.config.extension.name != 'xlsx';

        try { $scope.$digest(); } catch(err){}

        showPopup("#saveStrategyResultsModal");    
    }

    $scope.getDataLabel = function() {
        var data = getItem($scope.dataTypes, "value", $scope.config.data);
        return data ? data.name : '';
    }

    $scope.changeFormat = function(format) {
        $scope.config.extension.name = format;
    }

    $scope.changeExtension = function(extension){
        $scope.config.extension = extension;
        $scope.popupTitle = L.tsq("Save strategies as %s", [$scope.config.extension.description]);
    }
    
    //- Event Handlers --------------------------------------------------------------

    var pathName = "databankChoose";
    
    //the object to be used by ng-model must have at least one level structure to work properly
    //(using at least one dot)
    $scope.config = SaveButtonService.config;

    SaveButtonService.callback = onClick;
    
    $scope.singleResults;
    $scope.showMagicNumberSettings = false;

    $scope.dataTypes = [{value: 'all', name: L.tsq('All')}, {value: 'main', name: L.tsq('Main backtest')}]
    
});