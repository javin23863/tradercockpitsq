angular.module('app.dashboardResults').directive('resultPanel', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/DashboardResults/directives/resultPanel.html',
        scope : {
            instance: '=',
        },
        controller: 'ResultPanelCtrl',
        replace: true,
    }
});