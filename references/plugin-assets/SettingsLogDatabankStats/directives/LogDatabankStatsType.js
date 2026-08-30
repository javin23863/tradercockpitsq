angular.module('app.settings').directive('logDatabankStatsType', function () {
    return {
        restrict: 'AE',
        replace: true,
        templateUrl: '../../../plugins/SettingsLogDatabankStats/directives/logDatabankStatsType.html',
        scope : {
            instance: '=',
        },
        controller: 'LogDatabankStatsTypeCtrl'
    }
});