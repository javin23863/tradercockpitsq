angular.module('app').directive('maintabs', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ServletCodeEditor/directives/maintabs/maintabs.html',
        scope : {
            instance: '=',
            afterInit: '=',
            tabsChanged: '='
        },
        controller: 'MainTabsCtrl',
    }
});