angular.module('app.settings').controller('SimpleClearDatabanksCtrl', function ($scope, TaskClearDatabanksService) {

    $scope.onAddDatabank = function(){
        $scope.config.databanks.push('Results');
        TaskClearDatabanksService.saveSettings();
    }

    $scope.onRemoveDatabank = function(index){
        $scope.config.databanks.splice(index, 1);
        TaskClearDatabanksService.saveSettings();
    }

    $scope.onSave = function(index, databankName){
        $scope.config.databanks[index] = databankName;
        TaskClearDatabanksService.saveSettings();
    }

    $scope.getDatabankTitle = function(name) {
        var databank = getItem($scope.availableDatabanks, 'name', name);
        return databank ? databank.title : '';
    }

    $scope.projectStates = TaskClearDatabanksService.constants.runningStatuses;
    $scope.availableDatabanks = TaskClearDatabanksService.availableDatabanks;
    $scope.config = TaskClearDatabanksService.config;

    TaskClearDatabanksService.loadSettings();
});