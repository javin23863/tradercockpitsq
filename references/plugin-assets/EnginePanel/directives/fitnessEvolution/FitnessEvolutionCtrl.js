angular.module('app.engine').controller('FitnessEvolutionCtrl', function ($scope, $rootScope, EngineService, AppService, $timeout, $element, FitnessEvolutionService, SQConstants) {
    console.log("FitnessEvolutionCtrl initialized");
    $scope.broker = SQConstants.getConstants().broker;
    
    $scope.islands = [];

    $scope.chartFor = 0;
    $scope.charts = [{}, {}, {}];
    $scope.geneticBuild = false;

    $scope.lastData = null;

    $scope.popupOpened = false;

    $scope.keydown = function(event) {
        if(event.keyCode == 27) {
            $scope.popupOpened = false;
        }
    }

    $scope.instance.display = function() {  
        $scope.lastData = null;      
        $scope.popupOpened = true;
        
        try {$scope.$digest();} catch (err) { }

        showPopup('#fitnessEvolutionModal');
    }

    $scope.instance.setData = function(data) {
        $scope.lastData = data;

        $scope.geneticBuild = data.geneticBuild;
        if(data.geneticBuild) {
            loadToArray($scope.islands, data.islands);   

            if($scope.chartFor >= $scope.islands.length) {
                $scope.chartFor = 0;
            }
        } else {
            $scope.islands.length = 0;
            $scope.chartFor = -1;
        }

        $scope.printCharts($scope.chartFor);
    }

    $scope.printCharts = function(chartFor) {
        if(!$scope.lastData || !$scope.popupOpened) return;

        $scope.chartFor = chartFor;

        if($scope.lastData.geneticBuild && $scope.chartFor >= 0 && $scope.chartFor < $scope.islands.length) {
            $scope.charts[0].print($scope.islands[$scope.chartFor].chart[0]);
            $scope.charts[1].print($scope.islands[$scope.chartFor].chart[1]);
            $scope.charts[2].print($scope.islands[$scope.chartFor].chart[2]);

        } else {
            $scope.charts[0].print($scope.lastData.databank[0]);
            $scope.charts[1].print($scope.lastData.databank[1]);
            $scope.charts[2].print($scope.lastData.databank[2]);

        }

        try {$scope.$digest();} catch (err) { }
    }
});