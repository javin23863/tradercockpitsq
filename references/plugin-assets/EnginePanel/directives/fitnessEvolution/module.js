angular.module('app.engine').directive('fitnessEvolution', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/EnginePanel/directives/fitnessEvolution/fitnessEvolution.html',
        scope : {
            instance: '=',
        },
        controller: 'FitnessEvolutionCtrl'
    }
});