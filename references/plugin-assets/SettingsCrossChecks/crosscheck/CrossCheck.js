angular.module('app.settings').directive('crossCheck', function () {
    return {
        restrict: 'E',
        templateUrl: '../../../plugins/SettingsCrossChecks/crosscheck/crossCheck.html',
        scope : {
            crosscheck : '=',
            license : '='
        },
        controller: 'CrossCheckCtrl'
    }
});