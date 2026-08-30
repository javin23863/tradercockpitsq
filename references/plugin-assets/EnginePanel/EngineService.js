angular.module('app.engine').service('EngineService', function(BackendService, L, SettingsService) {
    
    this.getTypes = function(projectName, callOnSuccess) {
        BackendService.sendRequest('engine/getTypes', {projectName: projectName}, callOnSuccess);
    } 

    this.saveSelection = function(data) {
        BackendService.sendRequest('engine/saveSelection', data);
    } 

    this.saveSettings = function(data) {
        BackendService.sendRequest('engine/saveSettings', data);
    } 

    this.loadSettings = function(data, callOnSucces) {
        BackendService.sendRequest('engine/loadSettings', data, callOnSucces);
    } 

    this.loadTextLog = function(projectName, callOnSucces) {
        BackendService.sendRequest('engine/loadTextLog', {projectName: projectName}, callOnSucces);
    } 

    this.loadVisualLog = function(projectName, type, callOnSucces) {
        BackendService.sendRequest('engine/loadVisualLog', {projectName: projectName, type: type}, callOnSucces);
    } 

    this.stopSendingEngineStats = function() {
        BackendService.sendRequest('engine/stopSendingEngineStats', null, null);
    }

    this.clearLog = function(projectName, callOnSuccess) {
        BackendService.sendRequest('engine/clearLog', {projectName: projectName}, callOnSuccess);
    }

    this.cleanupMemory = function(callOnSuccess){
        BackendService.sendRequest('engine/cleanupMemory', null, callOnSuccess);
    }
    
    this.getStrategyTypeInfo = function(strategyType){
        switch(strategyType) {
            case instance.strategyTypes.default: return L.tsq('Default build config');
            case instance.strategyTypes.market: return L.tsq('Strategies that open at market price, they use almost all availablle signals and can produce a variety of trading approaches');
            case instance.strategyTypes.trendFollowing: return L.tsq('Trend following (breakout) strategies that use stop orders to catch breakouts and go with the trend');
            case instance.strategyTypes.meanReversal: return L.tsq('Strategies that are exploiting reversal of price from extreme values to mean');
            case instance.strategyTypes.fuzzy: return L.tsq('Strategies using fuzzy logic rules - fuzzy rule has multiple conditions, but only a defined % of them must be valid in order for rule to be triggered');
            case instance.strategyTypes.daily: return L.tsq('Daily strategies that place high emphasis on their robustness in multiple markets');
            default: return L.tsq('Custom build settings are used.') + '<br/>' + L.tsq('Choose one of the types above to apply standard settings.');
        }
    }

    var instance = this;
    
    this.strategyTypes = SettingsService.strategyTypes;

});