angular.module('app.resultstabs.spoverview').directive('spOverviewList', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsSPOverview/directives/spOverviewList/spOverviewList.html',
        scope : {
            instance: '='
        },
        controller: 'SPOverviewListCtrl'
    }
});