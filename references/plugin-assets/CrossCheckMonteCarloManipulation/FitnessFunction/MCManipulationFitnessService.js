angular.module('app').service('MCManipulationFitnessService', function($rootScope, $injector, $q, AppService, BackendService, SQConstants) {

    this.list = function(callback) {
        BackendService.sendRequest('monteCarloManipulation/fitnessList', null, callback);
    }

    this.getLevels = function(callOnSuccess) {
        BackendService.sendRequest('monteCarloManipulation/fitnessGetConfidenceLevels', null, callOnSuccess);
    }   
});