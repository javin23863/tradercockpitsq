angular.module('app.resultstabs.sysparampermutation').service('SysParamPermutationService', function(BackendService) {
    
    this.print = function(data, callOnSuccess) {
        BackendService.sendRequest('sysParamPermutation/print', data, callOnSuccess);
    }   

    this.printTable = function(data, callOnSuccess) {
        BackendService.sendRequest('sysParamPermutation/printTable', data, callOnSuccess);
    }  
});