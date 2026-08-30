angular.module('app.resultstabs.spoverview').directive('spOverviewTreeMap', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsSPOverview/directives/spOverviewTreeMap/spOverviewTreeMap.html',
        scope : {
            instance: '='
        },
        controller: 'SPOverviewTreeMapCtrl'
    }
});