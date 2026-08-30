angular.module('app').directive('bottomtabs', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ServletCodeEditor/directives/bottomtabs/bottomtabs.html',
        scope : {
            instance: '=',
        },
        controller: 'BottomTabsCtrl',
    }
});