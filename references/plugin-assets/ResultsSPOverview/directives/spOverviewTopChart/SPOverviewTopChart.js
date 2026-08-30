angular.module('app.resultstabs.spoverview').directive('spOverviewTopChart', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsSPOverview/directives/spOverviewTopChart/spOverviewTopChart.html',
        scope : {
            instance: '='
        },
        controller: 'SPOverviewTopChartCtrl'
    }
});