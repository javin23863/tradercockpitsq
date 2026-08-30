angular.module('app').controller('SequentialOptimizationCtrl', function ($scope, $rootScope, SequentialOptimizationService, $q) {
    
    $scope.config = {
		distributionUp: 50,
        distributionDown: 50,
        steps: 50,
        applyToStrategy: false,

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
		SequentialOptimizationService.applySettings(settingsElem, $scope.config);

		try { $scope.$digest(); } catch(er) {}
    }

    $scope.tab.loadSettings = function(settingsElem) {
        SequentialOptimizationService.loadSettings(settingsElem, $scope.config);
    }
});

