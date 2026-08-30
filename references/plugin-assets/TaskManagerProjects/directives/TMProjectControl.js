angular.module('app.tmprojects').directive('tmProjectControl', function () {
    return {
        restrict: 'AE',
        replace: true,
        templateUrl: '../../../plugins/TaskManagerProjects/directives/tmProjectControl.html',
        scope : {
            instance: '=',
        },
        controller: 'TMProjectControlCtrl'
    }
});