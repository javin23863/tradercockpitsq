angular.module('app.resultstabs.portfoliocorrelation').service('PortfolioCorrelationService', function(BackendService) {

    this.compute = function(data, callOnSuccess) {
        BackendService.sendRequest('portfoliocorrelation/compute', data, callOnSuccess);
    }   

    this.stop = function() {
        BackendService.sendRequest('portfoliocorrelation/stop', null, null);
    }  

    this.correlationSave = function(data, callOnSuccess) {
        BackendService.sendRequest('portfoliocorrelation/correlationSave', data, callOnSuccess);
    }  
});
    