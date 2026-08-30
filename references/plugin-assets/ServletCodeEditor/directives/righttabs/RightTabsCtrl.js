angular.module('app').controller('RightTabsCtrl', function ($scope, $rootScope, $timeout) {
    console.log("RightTabsCtrl controller initialized");

    $scope.bottomTabs = {
        tabs: [
            {
                title: 'Navigator',
                dataItem: 'navigator',
                templateUrl: '../../../plugins/ServletCodeEditor/directives/righttabs/navigator/navigator.html',
                controller: 'NavigatorCtrl'
            },
        ]
    }
    $scope.topTabs = {
        tabs: [
            {
                title: 'Open editors',
                dataItem: 'open-editors',
                templateUrl: '../../../plugins/ServletCodeEditor/directives/righttabs/editors/editors.html',
                controller: 'EditorsCtrl'
            },
        ]
    }

    var domInited = false;

    $scope.domInited = function () {
        if (domInited) {
            return;
        }
        domInited = true;
        $timeout(function () {
            var splitComponent = Split(['#editorsTabs', '#navigatorTabs'], {
                minSize: 100,
                sizes: [25, 75],
                direction: 'vertical',
                snapOffset: 0
            });
        }, 1000, false);
    }
});