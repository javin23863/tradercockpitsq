angular.module('app.dmhome').service('DMHomeService', function($rootScope, $q, BackendService, SQEvents) {
        
        this.link = function (callOnSuccess) {
            BackendService.sendRequest('/home/link', null, callOnSuccess);
        }
});