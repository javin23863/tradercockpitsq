angular.module('app').controller('OptProfileSysParamPermutationCtrl', function ($scope, $rootScope, OptProfileSysParamPermutationService, $q, OptimizationService) {
    
    $scope.config = {
        maxTests: 1000,

        distributionUp: 100,
        distributionDown: 100,
        steps: 25,

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

    $scope.tab.applySettings = function(settingsElem) {
		OptProfileSysParamPermutationService.applySettings(settingsElem, $scope.config);

		try { $scope.$digest(); } catch(er) {}
    }

    $scope.tab.loadSettings = function(settingsElem) {
        OptProfileSysParamPermutationService.loadSettings(settingsElem, $scope.config);
    }

    $scope.maxOptimizationValues = OptimizationService.maxOptimizationValues;
    $scope.printMaxTestsLabel = OptimizationService.printMaxTestsLabel;
});

