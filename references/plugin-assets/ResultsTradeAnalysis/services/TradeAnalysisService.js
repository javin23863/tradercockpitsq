angular.module('app.resultstabs.tradeanalysis').service('TradeAnalysisService', function(BackendService) {
    
    this.print = function(data, callOnSuccess) {
        data.projectName = data.project;
        data.databankName = data.databank;
        data.strategyName = data.strategy;
        BackendService.sendRequest('tradeanalysis/print', data, callOnSuccess);
    }   
});