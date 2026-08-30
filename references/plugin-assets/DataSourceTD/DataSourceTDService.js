angular.module('app.datasource.td').service('DataSourceTDService', function($rootScope, $q, BackendService) {

    this.loadAvailableSymbols = function (data, callOnSuccess) {
        BackendService.sendRequest('dataSourceTD/loadAvailableSymbols', data, callOnSuccess);
    }

    this.importData = function (data, callOnSuccess) {
        BackendService.sendRequest('dataSourceTD/importData', data, callOnSuccess, 'POST');
    }

    this.importDataAction = function (symbol, action) {
        BackendService.sendRequest('dataSourceTD/importDataAction', { symbol: symbol, action: action });
    }
});