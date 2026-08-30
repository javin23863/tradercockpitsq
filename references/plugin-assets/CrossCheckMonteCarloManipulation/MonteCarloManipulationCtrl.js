angular.module('app').controller('MonteCarloManipulationCtrl', function ($scope, $timeout, $rootScope, MonteCarloManipulationService, $q) {
    
    var methods = [];

    $scope.config = {
        settingsTableInstance : {},
        numberOfSimulations: 10,
        useFullSample: false
    };

    $scope.tab.applySettings = function(settingsElem) {
        deferred.promise.then(function(){
            $scope.config.settingsTableInstance.load({rows : methods});
            MonteCarloManipulationService.applySettings(settingsElem, $scope.config);

            try { $scope.$digest(); } catch(er) {}
        });
    }

    $scope.tab.loadSettings = function(settingsElem) {
        MonteCarloManipulationService.loadSettings(settingsElem, $scope.config);
    }

    $scope.onChange = function() {

    }
        
    $scope.afterInit = function() {   
        $timeout(init, 100, false);   
    };

    function init() {
        MonteCarloManipulationService.list(function(data) {
            methods = data.rows;

            deferred.resolve();
        });
    }

    var deferred = $q.defer();
});