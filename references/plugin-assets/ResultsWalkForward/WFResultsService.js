angular.module('app.resultstabs.wf').service('WFResultsService', function(BackendService, $rootScope, L) {
    
    this.print = function(data, callOnSuccess) {
        BackendService.sendRequest('walkforward/print', data, callOnSuccess, 'POST');
    }

    this.getConditions = function(data, callOnSuccess) {
        BackendService.sendRequest('walkforward/getConditions', data, callOnSuccess);
    }

    this.printTable = function(data, callOnSuccess) {
        BackendService.sendRequest('walkforward/printTable', data, callOnSuccess);
    }

    this.setParametersToStrategy = function(data, callOnSuccess){
        BackendService.sendRequest('resultsDatabankActions/setStrategyParameters', data, callOnSuccess, 'POST');
    }

    this.export = function(data){
        BackendService.sendRequest('walkforward/export', data, function(){
            $rootScope.showSuccess(L.tsq("WF params exported"));
        });
    }
});