angular.module('app.datasource.yahoo').service('YahooService', function($rootScope, SQEvents, $q, BackendService, SQConstants) {
    
    this.add = function(data, callOnSuccess) {
        BackendService.sendRequest('dataSourceYahoo/add', data, callOnSuccess, 'POST');
    }   

    this.addCancel = function() {
        BackendService.sendRequest('/dataSourceYahoo/addCancel', null);
    }

    this.importData = function (data) {
        return BackendService.getPromise('dataSourceYahoo/importData', data, 'POST');
    }

    this.importDataAction = function (symbol, action) {
        return BackendService.getPromise('dataSourceYahoo/importDataAction', { symbol: symbol, action: action });
    }
});