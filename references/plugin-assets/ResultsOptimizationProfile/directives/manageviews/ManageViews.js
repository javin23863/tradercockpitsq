angular.module('app.resultstabs.optimprof').directive('optManageViews', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsOptimizationProfile/directives/manageviews/manageviews.html',
        scope : {
            instance: '='
        },
        controller: 'OptManageViewsCtrl',
    }
});