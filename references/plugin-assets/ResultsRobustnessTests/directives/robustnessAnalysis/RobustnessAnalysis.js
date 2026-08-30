angular.module('app.resultstabs.robustnesstests').directive('robustnessAnalysis', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/ResultsRobustnessTests/directives/robustnessAnalysis/robustnessAnalysis.html',
        scope : {
            instance: '=',
            type: '@',
        },
        controller: 'RobustnessAnalysisCtrl',
    }
});