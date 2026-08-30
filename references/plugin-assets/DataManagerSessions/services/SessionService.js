angular.module('app.sessions').service('SessionService', function($rootScope, $q, BackendService, SQEvents) {
        
        this.addSession = function(template){
            return BackendService.getPromise('/sessions/addSession', template);
        }

        this.removeSession = function(names){
            return BackendService.sendRequest('/sessions/removeSession', {names : names}, function(result){
                if(!result.success){
                    $rootScope.showError(result.error);
                }
            },'POST');
        }

        this.updateSession = function(template){
            return BackendService.getPromise('/sessions/updateSession', template);
        }

        this.save = function (data) {
            BackendService.sendRequest('/sessions/save', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            }, 'POST');
        }

        this.load = function (data) {
            BackendService.sendRequest('/sessions/load', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }

        this.clone = function (data, callback) {
            BackendService.sendRequest('/sessions/clone', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                    callback();
                }
            });
        }
    });