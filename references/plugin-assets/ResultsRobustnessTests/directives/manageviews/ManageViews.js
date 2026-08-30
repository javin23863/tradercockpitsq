angular.module('app.resultstabs.robustnesstests').directive('manageviews', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsRobustnessTests/directives/manageviews/manageviews.html',
        scope : {
            instance: '=',
            type: '@',
        },
        controller: 'ManageViewsCtrlRt',
    }
});