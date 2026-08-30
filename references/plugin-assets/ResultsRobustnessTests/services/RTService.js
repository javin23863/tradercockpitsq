angular.module('app.resultstabs.robustnesstests').service('RTService', function (BackendService) {

    this.printChart = function (data, callOnSuccess) {
        BackendService.sendRequest('rtresults/printChart', data, callOnSuccess);
    }

    this.printConfLevels = function (data, callOnSuccess) {
        BackendService.sendRequest('rtresults/printConfLevels', data, callOnSuccess);
    }

    this.getMethods = function (data, callback) {
        BackendService.sendRequest('rtresults/getMethods', data, callback, 'GET', true);
    }

});
