angular.module('app.resultsdatabankactions.mergestrategies').controller('MergeStrategiesPopupCtrl', function ($scope, $rootScope, $timeout, SQEvents, SQConstants, L, DatabankActionsService, DatabankService, AppService) {

    $scope.onMergeStrategies = function(){
        $rootScope.setProgressInfo("Merging strategies", "");
                
        DatabankService.mergeStrategies(DatabankActionsService.selectedStrategies, $scope.config).then(function(result) {
            if(!result.success){
                $rootScope.showError(L.tsq("Error while merging strategies. %s", [ result.error ]));
            }
        });
    }

    $scope.onHelp = function(){
        AppService.openHelp("https://strategyquant.com/doc/strategyquant/merge-split-portfolio/");
    }

    function onEvent(event, data){
        if(event == "showMergeStrategiesPopup") {
            if(!isPopupOpen('#mergeStrategiesPopup')) {
                var strategies = DatabankActionsService.selectedStrategies.split(',');
                loadToArray($scope.strategies, strategies);

                $scope.config.name = "Portfolio";
                $scope.config.databank = getDefaultDatabank(data.databanks, $scope.config.databank);
                $scope.config.mergeType = "simulated",
                $scope.config.simulatedInitialCapital = "first",
                $scope.config.signalsConditionsPct = 70,
                $scope.config.signalsStrategy = strategies[0];

                loadToArray($scope.availableDatabanks, data.databanks);

                try { $scope.$digest(); } catch (err) {}

                showPopup('#mergeStrategiesPopup');
            }        
        }
    }

    function getDefaultDatabank(databanks, lastDatabank){
        var resultsDB = SQConstants.getConstants().gedatabanks.results;

        if(!lastDatabank || !databanks){
            return resultsDB;
        }

        var lastDatabankUsed = getItem(databanks, 'name', lastDatabank);
        if(lastDatabankUsed){
            return lastDatabank;
        }
        else return resultsDB;
    }

    $scope.$on('$destroy', function(){
        SQEvents.removeListener(listenerId);
    });

    $scope.balanceTypes = SQConstants.getConstants().portfolioInitialBalanceTypes;

    $scope.config = {
        name: null,
        databank: null,
        mergeType : "simulated",
        simulatedInitialCapital: "first",
        signalsConditionsPct: 70,
        signalsStrategy: null
    }

    $scope.strategies = [];
    $scope.availableDatabanks = [];

    var listenerId = "MergeStrategiesPopupCtrl-" + window.appConfig.product;

    SQEvents.addListener(listenerId, ["showMergeStrategiesPopup"], onEvent);
});