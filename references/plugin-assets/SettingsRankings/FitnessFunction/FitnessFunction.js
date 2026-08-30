angular.module('app.settings').directive('fitnessFunction', function () {
    return {
        restrict: 'AE',
        templateUrl: '../../../plugins/SettingsRankings/FitnessFunction/fitnessFunction.html',
        scope : {
            instance : '=',
            afterInit: '=',
            onChange: '=',
        },
        controller: 'FitnessFunctionCtrl'
    }
});