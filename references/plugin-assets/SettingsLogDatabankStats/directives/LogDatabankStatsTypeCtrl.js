angular.module('app.settings').controller('LogDatabankStatsTypeCtrl', function ($rootScope, $scope, LogDatabankStatsService, SQConstants, $timeout, ColumnSettingsService) {
    console.log("LogDatabankStatsType controller initialized");

    $scope.availableTypes = LogDatabankStatsService.availableTypes;

    $scope.getTypeLabel = function() {
        var type = getItem($scope.availableTypes, "value", $scope.instance.type);
        return type ? type.name : 'NA';
    }

    $scope.remove = function() {
        LogDatabankStatsService.remove($scope.instance)
    } 
    
    $scope.edit = function() {
        LogDatabankStatsService.edit($scope.instance)
    }

    $scope.getColumnLabel = function() {
        var columnObj = $scope.instance.columnObj;
        if (columnObj) {
            return ColumnSettingsService.printColumnTitle(columnObj);
        }

        return "N/A";
    }

    $scope.afterCGInit = function(data){
        var columnTypes = SQConstants.getConstants().databankColumnTypes; 
        var columns = SQConstants.getColumnsByType([columnTypes.general, columnTypes.wfSpecial], null, true);

        $scope.instance.condGridInstance.setData(columns, SQConstants.getColumns().recentColumns);

        if($scope.instance.conditionsObj) {
            $scope.instance.condGridInstance.setConditionsObj($scope.instance.conditionsObj, true); 

            $scope.instance.conditionsObj = null;
        }
    }
    
    function init() {
        loaded = false;

        $timeout(function () {
            loaded = true;
        });
    }
    
    $scope.onCGChange = function(data){            
        onSettingsChange();
    }

    function onSettingsChange() {
        if(!loaded) return;

        if(LogDatabankStatsService.settingsChanged) LogDatabankStatsService.settingsChanged();
    }

    init();
});