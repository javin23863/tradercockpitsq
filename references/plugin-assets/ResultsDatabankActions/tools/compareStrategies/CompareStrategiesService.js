angular.module('app').service('CompareStrategiesService', function($rootScope, SQEvents, BackendService, SQConstants) {

    this.compare = function(data, callback){
        BackendService.sendRequest("resultsDatabankActions/compareStrategies", data, callback, 'GET', true);
    }

    var instance = this;
});