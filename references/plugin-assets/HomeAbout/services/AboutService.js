angular.module('app').service('AboutService', function(BackendService) {
    this.license = function(callOnSuccess) {
        BackendService.sendRequest('about/license', {}, callOnSuccess);
    }

    this.updateLicense = function(code, callOnSuccess) {
        BackendService.sendRequest('about/updateLicense', {code: code}, callOnSuccess);
    }
});