angular
  .module("app.resultstabs.sequentialoptimization")
  .controller("SequentialParamChartCtrl", function ($scope, SQConstants) {
    console.log("Sequential parameter optimization chart controller initialized");

    $scope.selectedType = $scope.instance.selectedType;

    $scope.chart = {};

    $scope.getColumnLabel = function () {
      var column = getItem($scope.columns, "value", $scope.selectedType);
      return column ? column.name : "";
    };

    $scope.onTypeSelect = function () {
      $scope.instance.onTypeSelect();
    };

    $scope.instance.print = function (data) {
      if (!$scope.chart.print) {
        return;
      }
      $scope.chart.print(data.chart, "spp");

      try {
        $scope.$digest();
      } catch (err) {}
    };

    $scope.instance.setSelectedType = function (type) {
      $scope.selectedType = type;
    };

    $scope.instance.getSelectedType = function () {
      return $scope.selectedType;
    };

    $scope.instance.reset = function () {
      $scope.chart.print(null);
    };

    function tryPrint(){
      setTimeout(function(){
        if (!$scope.chart.print) {
            tryPrint();
        }
        else {
            $scope.chart.print($scope.instance.data, "spp");
        }
      }, 100);
    }

    tryPrint();
  });
