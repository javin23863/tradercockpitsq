angular.module('app.data').service('ExportToMT5Service', function($rootScope, $q, BackendService) {

        this.export = function(data, callOnSuccess) {
            BackendService.sendRequest('/data/exportToMT5', data, callOnSuccess);
        }

        this.action = function(symbol, action, removeFile) {
            return BackendService.getPromise('/data/exportToMT5Action', {symbol: symbol, action: action, removeFile : removeFile});
        }
});