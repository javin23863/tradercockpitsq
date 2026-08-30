angular.module('app.resultstabs.sysparampermutation').controller('SPPPanelCtrl', function ($scope, SysParamPermutationService, SQConstants) {
    console.log("SPP panel controller initialized");

    $scope.selectedType = $scope.instance.selectedType;

    $scope.chart = {};
    $scope.median = null;

    $scope.getColumnLabel = function(){
        var column = getItem($scope.columns, "value", $scope.selectedType);
        return column ? column.name : '';
    }

    $scope.onTypeSelect = function () {
        $scope.instance.onTypeSelect();
    }

    $scope.instance.print = function (data) {
        if (!$scope.chart.print) {
            return;
        }

        $scope.median = data.median;
        $scope.chart.print(data.chart, "spp");

        try { $scope.$digest(); } catch (err) { }
    }

    $scope.instance.setSelectedType = function (type) {
        $scope.selectedType = type;
    }

    $scope.instance.getSelectedType = function () {
        return $scope.selectedType;
    }

    $scope.instance.reset = function () {
        $scope.chart.print(null);
    }
});