angular.module('app.resultstabs.equitychart').directive('benchmark', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsEquityChart/benchmark/benchmark.html',
        scope : {
            instance: '=',
            types: '=',
            learnMore: '='
        },
        controller: 'BenchmarkCtrl'
    }
});