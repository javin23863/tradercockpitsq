angular.module('app').controller('RetestWithHigherPrecisionCtrl', function ($scope, $rootScope, SQConstants, RetestWithHigherPrecisionService, DataService, AppService) {
    
    $scope.availableData = [];
    $scope.availableTimeframes = [];
    $scope.precisions = [];
    $scope.allTimeframes = angular.copy(SQConstants.getConstants().timeframes);

    $scope.config = {
        symbol: null,
        timeframe: null,
        precision: null,
        spread: null,
        crossCheckPrecision: null,
        crossCheckSpread: null
    }

    $scope.onCustomSpreadClick = function(){
        $scope.config.customSpread=!$scope.config.customSpread;

        if (!$scope.config.customSpread){
            $scope.config.crossCheckSpread = $scope.config.spread;
        }
    }

    $scope.isAutoRetest = AppService.getTask().type == 'AutomaticRetest';

    $scope.tab.applySettings = function(settingsElem) {
        var mainSetup = DataService.config.setups[0];

        RetestWithHigherPrecisionService.loadConfig($scope.config);

        if($scope.isAutoRetest){
            loadToArray($scope.precisions, SQConstants.getConstants().precisions);
        }
        else {
            DataService.loadAvailablePrecisions($scope.precisions, $scope.config.symbol, mainSetup ? mainSetup.engine : null);
        }

        RetestWithHigherPrecisionService.applySettings(settingsElem, $scope.config);

        if(!isPrecisionCorrect($scope.config.crossCheckPrecision)) {
             $scope.config.crossCheckPrecision = $scope.precisions[0].value;
        }

        try { $scope.$digest(); } catch(er) {}
    }

    $scope.tab.loadSettings = function(settingsElem) {
        RetestWithHigherPrecisionService.loadSettings(settingsElem, $scope.config);
    }
    
    $scope.getCrossCheckPrecisionLabel = function() {
        var precision = getItem($scope.precisions, "value", $scope.config.crossCheckPrecision);
        return precision ? precision.name : '';
    }

    function isPrecisionCorrect(precision) {
        for(var i=0; i<$scope.precisions.length; i++) {
            if($scope.precisions[i].value==precision) {
                return true;
            }
        }

        return false;
    }

});