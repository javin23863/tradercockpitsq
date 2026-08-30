angular.module('app.datasource.dukascopy').service('DukascopyService', function ($rootScope, SQEvents, $q, BackendService, SQConstants) {

    function loadData() {
        dataPromise = BackendService.getPromise('dataSourceDukascopy/getDataList', null).then(function (result) {
            if (result.success) {
                data = result.data.data;
                loadToArray(instance.categories, result.data.categories);
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

    this.addData = function (postfix, symbols, dataType, broker, instruments) {
        var data = {
            postfix: postfix,
            symbols: symbols,
            dataType: dataType,
            broker: broker,
            instruments: instruments
        }

        return BackendService.getPromise('dataSourceDukascopy/addData', data, 'POST');
    }
    
    this.addCancel = function() {
        BackendService.sendRequest('/dataSourceDukascopy/addCancel', null);
    }
    
    //------------------------------------------------------------------------

    this.importData = function (data) {
        return BackendService.getPromise('dataSourceDukascopy/importData', data, 'POST');
    }

    this.importDataAction = function (symbol, action) {
        return BackendService.getPromise('dataSourceDukascopy/importDataAction', { symbol: symbol, action: action });
    }

    //- Initialization --------------------------------------------------------------

    var instance = this;
    var data, dataPromise;
    var settings, settingsPromise;

    this.categories = [];
});