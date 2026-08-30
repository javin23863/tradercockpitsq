angular.module('app').service('MCRetestFitnessService', function($rootScope, $injector, $q, AppService, BackendService, SQConstants) {

    this.list = function(callback) {
        BackendService.sendRequest('monteCarloRetest/fitnessList', null, callback);
    }

    this.getLevels = function(callOnSuccess) {
        BackendService.sendRequest('monteCarloRetest/fitnessGetConfidenceLevels', null, callOnSuccess);
    }   
});