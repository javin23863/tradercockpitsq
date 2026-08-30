angular.module('app.fitnessStrategyResult').service('FitnessMethodStrategyResultService', function(BackendService) {

    this.list = function(callback) {
        BackendService.sendRequest('fitnessMethodStrategyResult/list', null, callback);
    }
});