angular.module('app').controller('WalkForwardOptimizationCtrl', function ($scope, $rootScope, SQConstants, WalkForwardOptimizationService, OptimizationService) {
    
    $scope.constants = SQConstants.getConstants().optimization;
    $scope.wfTypes = objectToList($scope.constants.wfTypes);
    $scope.wfTypes.sort(function (a, b) {
        return a.value - b.value;
    });
    $scope.MaxOptimizationsWarning = SQConstants.MAX_OPTIMIZATIONS_WARNING;    

    $scope.config = {
        maxTests: "100",

        //Parameters settings
        symmetricVariables: true,
        periodParams: true,
        shiftParams: true,
        constantsParams: true,
        otherParams: true,
        entryParams: true,
        entryLogic: true,
        exitParamsUsed: true,
        exitParamsUnused: true,
        booleanParams: true,
        parametrizeType: 0
    };

    $scope.getWFTypeLabel = function() {
        var type = getItem($scope.wfTypes, "value", $scope.config.wfType);
        return type ? type.name : '';
    }

    $scope.tab.applySettings = function(settingsElem) {
        WalkForwardOptimizationService.applySettings(settingsElem, $scope.config);

        try { $scope.$digest(); } catch(er) {}
    }

    $scope.tab.loadSettings = function(settingsElem) {
        WalkForwardOptimizationService.loadSettings(settingsElem, $scope.config);
    }

    $scope.maxOptimizationValues = OptimizationService.maxOptimizationValues;
    $scope.printMaxTestsLabel = OptimizationService.printMaxTestsLabel;
    
    $scope.printParamLabel = OptimizationService.printParamLabel;
});