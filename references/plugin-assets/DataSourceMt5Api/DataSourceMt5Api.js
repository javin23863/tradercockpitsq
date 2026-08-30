angular.module('app.datasource.mt5api').service('DataSourceMt5ApiService', function($rootScope, $q, BackendService) {

    this.loadAvailableSymbols = function (data, callback) {
        BackendService.sendRequest('dataSourceMt5Api/loadAvailableSymbols', data, callback, 'POST', true);
    }

    this.importData = function (data, callOnSuccess) {
        BackendService.sendRequest('dataSourceMt5Api/importData', data, callOnSuccess, 'POST');
    }

    this.importDataAction = function (symbol, action) {
        BackendService.sendRequest('dataSourceMt5Api/importDataAction', { symbol: symbol, action: action });
    }
});