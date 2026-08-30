angular.module('app').service('CCHigherPrecisionService', function(BackendService) {

    this.list = function(callback) {
        BackendService.sendRequest('retestWithHigherPrecision/list', null, callback);
    }
});