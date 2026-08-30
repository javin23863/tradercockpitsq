angular.module('app.settings').controller('SimpleNotificationSettingsCtrl', function ($rootScope, $scope, sqPlugin, $timeout, AppService, SQEvents, TaskNotificationService, L) {
    console.log("SimpleNotificationSettings controller initialized");

    $scope.config = TaskNotificationService.config;

    $scope.getNotificationTypeLabel = function(){
        var type = getItem($scope.config.types, 'key', $scope.config.type);
        return type ? type.name : '';
    }

    $scope.showAdvancedSettings = function (event) {
        AppService.switchToPanel('settings');
        event.stopPropagation();
        $timeout(tabsShowHiddenTabs, 0, false);
    }
});