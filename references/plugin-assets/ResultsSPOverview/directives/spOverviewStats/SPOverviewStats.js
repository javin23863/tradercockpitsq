angular.module('app.resultstabs.spoverview').directive('spOverviewStats', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsSPOverview/directives/spOverviewStats/spOverviewStats.html',
        scope : {
            instance: '='
        },
        controller: 'SPOverviewStatsCtrl'
    }
});