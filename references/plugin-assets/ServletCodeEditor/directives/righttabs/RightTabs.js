angular.module('app').directive('righttabs', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ServletCodeEditor/directives/righttabs/righttabs.html',
        scope : {
            instance: '=',
        },
        controller: 'RightTabsCtrl',
    }
});