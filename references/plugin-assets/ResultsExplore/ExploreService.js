angular.module('app.resultstabs.explore').service('ExploreService', function (BackendService) {

    this.info = function(data, callOnSuccess) {
        BackendService.sendRequest('explore/info', data, callOnSuccess);
    }  
});