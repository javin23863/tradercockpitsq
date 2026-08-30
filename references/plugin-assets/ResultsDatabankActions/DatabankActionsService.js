angular.module('app').service('DatabankActionsService', function($rootScope, SQEvents, BackendService, AppService, DatabankService) {
    this.strategyName = null;
       
    this.getSelectedStrategy = function() {
        return { 
            app: window.appConfig.product,
            project: AppService.getProject(),
            databank: $rootScope.activeDatabankTab.title,
            strategy: instance.strategyName
        }; 
    }           
    
    this.getStrategyXml = function(data, callOnSuccess) {
        BackendService.sendRequest('resultsDatabankActions/getStrategyXml', data, callOnSuccess, "POST");
    }    

    function updateSelectedStrategies(strategies){
        instance.selectedStrategies = strategies; 

        if(strategies) {
            var strategyIDs = strategies.split(",");
            
            if(strategyIDs.length > 0) {
                instance.strategyName = strategyIDs[0];
            } else {
                instance.strategyName = null;
            }
        } else {
            instance.strategyName = null;
        }
    }
    
    var instance = this;
    
    SQEvents.addListener("DBActionsService", [SQEvents.get('STRATEGY_SELECTED')], function(event, strategies) { 
        updateSelectedStrategies(strategies);
    });
    
});