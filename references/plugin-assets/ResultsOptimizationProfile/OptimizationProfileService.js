angular.module('app.resultstabs.optimprof').service('OptimizationProfileService', function(BackendService, $rootScope, $q) {
    
     this.print = function(data, callOnSuccess) {
        BackendService.sendRequest('optimprof/print', data, callOnSuccess);
    }   

    this.getAvailableParams = function(data, callOnSuccess) {
        BackendService.sendRequest('optimprof/getAvailableParams', data, callOnSuccess);
    } 

    this.printChart = function(data, callOnSuccess) {
        BackendService.sendRequest('optimprof/printChart', data, callOnSuccess);
    }  
    
    this.printResults = function(data, callOnSuccess) {
        BackendService.sendRequest('optimprof/printResults', data, callOnSuccess);
    }  

    this.export = function(data, callOnSuccess) {
        BackendService.sendRequest('optimprof/export', data, callOnSuccess);
    }  
});
    