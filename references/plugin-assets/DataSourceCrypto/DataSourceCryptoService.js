angular.module('app.datasource.crypto').service('DataSourceCryptoService', function($rootScope, $q, BackendService) {
    
    this.getExchanges = function(callback) {
        BackendService.sendRequest('/dataSourceCrypto/getExchanges', null, function(data) {
            loadToArray(instance.exchanges, data.exchanges);

            callback();
        });
    }

    this.getSymbols = function(exchange, callback) {
        BackendService.sendRequest('/dataSourceCrypto/getSymbols', {exchange: exchange}, callback);
    }
    
    this.add = function(data, callback) {
        BackendService.sendRequest('/dataSourceCrypto/add', data, callback);
    }

    this.addCancel = function() {
        BackendService.sendRequest('/dataSourceCrypto/addCancel', null);
    }

    this.importData = function (data) {
        return BackendService.getPromise('dataSourceCrypto/importData', data, 'POST');
    }

    this.importDataAction = function (symbol, action) {
        return BackendService.getPromise('dataSourceCrypto/importDataAction', { symbol: symbol, action: action });
    }

    
    var instance = this;
    this.exchanges = [];
});