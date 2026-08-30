angular
  .module("app.resultstabs.sequentialoptimization")
  .directive("sequentialParamChart", function () {
    return {
      restrict: "AE",
      templateUrl:
        "../../../plugins/ResultsSequentialOptimization/directives/sequentialParamChart.html",
      scope: {
        columns: "=",
        instance: "=",
      },
      controller: "SequentialParamChartCtrl",
    };
  });
