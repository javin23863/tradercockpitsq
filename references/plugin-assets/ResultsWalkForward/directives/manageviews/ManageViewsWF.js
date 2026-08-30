angular.module('app.resultstabs.wf').directive('manageViewsWf', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsWalkForward/directives/manageviews/manageviews.html',
        scope : {
            instance: '=',
        },
        controller: 'ManageViewsCtrlWF',
    }
});