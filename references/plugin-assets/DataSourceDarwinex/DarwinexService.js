angular.module('app.datasource.darwinex').service('DarwinexService', function ($rootScope, SQEvents, $q, BackendService, SQConstants) {

    // import -----------------------------------------------------------------------------------------------

    this.loadAvailableSymbols = function (data, callOnSuccess) {
        BackendService.sendRequest('darwinex/importLoadAvailableSymbols', data, callOnSuccess);
    }

    this.importData = function (data, callOnSuccess) {
        BackendService.sendRequest('darwinex/importData', data, callOnSuccess, 'POST');
    }

    this.importDataCancel = function() {
        BackendService.sendRequest('/darwinex/importDataCancel', null);
    }

    this.importDataAction = function (symbol, action) {
        BackendService.sendRequest('darwinex/importDataAction', { symbol: symbol, action: action });
    }

    // download -----------------------------------------------------------------------------------------------

    function loadData() {
        dataPromise = BackendService.getPromise('darwinex/downloadGetDataList', null).then(function (result) {
            if (result.success) {
                data = result.data.data;
            }
        });
    }

    this.getData = function () {
        if (data == null) {
            loadData();
            return dataPromise;
        }
        return data;
    }

    this.getSymbolInfo = function (symbol) {
        for (var i = 0; i < data.length; i++) {
            var info = data[i];
            if (symbol.indexOf(info.symbol) == 0) {
                return info;
            }
        }
    }

    this.addData = function (postfix, symbols, broker, instruments) {
        var data = {
            postfix: postfix,
            symbols: symbols,
            broker: broker,
            instruments: instruments
        }

        return BackendService.getPromise('darwinex/downloadAddData', data, 'POST');
    }
	
    this.dataCancel = function() {
        BackendService.sendRequest('darwinex/importDataCancel', null);
    }
    
    this.downloadData = function (data) {
        return BackendService.getPromise('darwinex/downloadData', data, 'POST');
    }

    this.downloadDataAction = function (symbol, action) {
        return BackendService.getPromise('darwinex/downloadDataAction', { symbol: symbol, action: action });
    }

    //- Initialization --------------------------------------------------------------

    var instance = this;
    var data, dataPromise;
});