angular.module('app.resultsdatabankactions.tools.compareStrategies').controller('CompareStrategiesPopupCtrl', function ($rootScope, $scope, CompareStrategiesService) {
    $scope.list1 = {};

    $scope.strategy1 = '';
    $scope.strategy2 = '';

    $rootScope.compareStrategiesPopup = function(projectName, databankName, strategies) {
        var data = {
            project: projectName,
            databank: databankName,
            strategies: strategies
        }

        CompareStrategiesService.compare(data, function(result) {
            print(result);
        });
    }

    function print(result) {
        $scope.strategy1 = result.strategy1;
        $scope.strategy2 = result.strategy2;

        if (!checkDirectiveReady($scope.list1, 'StrategyConfigList.list1', 'print')) {
            return;
        }

        $scope.list1.print(result.settings);   
    }

    $scope.expandAll = function () {
        if (!checkDirectiveReady($scope.list1, 'StrategyConfigList.list1', 'expandAll')) {
            return;
        }

        $scope.list1.expandAll();
    }

    $scope.collapseAll = function () {
        if (!checkDirectiveReady($scope.list1, 'StrategyConfigList.list1', 'collapseAll')) {
            return;
        }

        $scope.list1.collapseAll();
    }


});