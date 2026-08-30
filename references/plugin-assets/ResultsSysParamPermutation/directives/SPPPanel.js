angular.module('app.resultstabs.sysparampermutation').directive('sppPanel', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsSysParamPermutation/directives/sppPanel.html',
        scope : {
            columns: '=',
            instance: '=',
        },
        controller: 'SPPPanelCtrl'
    }
});