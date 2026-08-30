angular.module('app').controller('MonteCarloRetestCtrl', function ($scope, $timeout, $rootScope, MonteCarloRetestService, $q, SQConstants, DataService, L) {
    
    var methods = [];

    $scope.precisions = [];

    $scope.config = {
        settingsTableInstance : {},
        numberOfSimulations: 10,
        useFullSample: false,
        usePrecision: false,
        precision: null
    };

    $scope.tab.applySettings = function(settingsElem) {
        deferred.promise.then(function() {
            $scope.config.settingsTableInstance.load({rows : methods});    
            
            var mainSetup = DataService.config.setups[0];
            DataService.loadAvailablePrecisions($scope.precisions, mainSetup ? mainSetup.symbol : null, mainSetup ? mainSetup.engine : null);
            
            MonteCarloRetestService.applySettings(settingsElem, $scope.config, mainSetup);

            if($scope.config.precision == -1){
                $scope.config.precision = mainSetup.precision
            }

            if(!isPrecisionCorrect($scope.config.precision)) {
                $scope.config.precision = $scope.precisions[0].value;
            }

            try { $scope.$digest(); } catch(er) {}
        });
    }

    $scope.tab.loadSettings = function(settingsElem) {
        MonteCarloRetestService.loadSettings(settingsElem, $scope.config);
    }

    $scope.onChange = function() {

    }
        
    $scope.afterInit = function() {
        $timeout(init, 100, false);   
    };

    function init() {
        MonteCarloRetestService.list(function(data) {
            methods = data.rows;
        
            deferred.resolve();
        });
    }

    $scope.getPrecisionLabel = function(){
        var selectedPrecision = getItem($scope.precisions, "value", $scope.config.precision);
        return selectedPrecision ? L.tsq(selectedPrecision.name) : '';
    }

    $scope.usePrecisionChange = function(){
        if(!$scope.config.usePrecision){

            var mainSetup = DataService.config.setups[0];
            $scope.config.precision = mainSetup.precision
        }
    }

    function isPrecisionCorrect(precision) {
        for(var i=0; i<$scope.precisions.length; i++) {
            if($scope.precisions[i].value==precision) {
                return true;
            }
        }

        return false;
    }

     var deferred = $q.defer();
});

