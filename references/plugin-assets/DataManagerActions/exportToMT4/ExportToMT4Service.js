angular.module('app.data').service('ExportToMT4Service', function($rootScope, $q, BackendService) {

        this.export = function(data, callOnSuccess) {
            BackendService.sendRequest('/data/exportToMT4', data, callOnSuccess);
        } 

        this.action = function(symbol, action) {
            BackendService.sendRequest('/data/exportToMT4Action', {symbol: symbol, action: action});
        }

        this.getDataFolder = function(data, callOnSuccess) {
            BackendService.sendRequest('/data/exportToMT4GetDataFolder', data, callOnSuccess);
        } 

        this.getServerNames = function(data, callOnSuccess) {
            BackendService.sendRequest('/data/exportToMT4GetServerNames', data, callOnSuccess);
        } 

        this.loadProperties = function(data, callOnSuccess) {
            BackendService.sendRequest('/data/exportToMT4LoadProperties', data, callOnSuccess);
        } 

    });