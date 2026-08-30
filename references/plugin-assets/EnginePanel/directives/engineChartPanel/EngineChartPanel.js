angular.module('app.engine').directive('engineChartPanel', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/EnginePanel/directives/engineChartPanel/engineChartPanel.html',
        scope : {
            types: '=',
            instance: '=',
        },
        controller: 'EngineChartPanelCtrl'
    }
});