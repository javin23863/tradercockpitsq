angular.module('app.data').service('DMDataService', function ($rootScope, $q, BackendService) {

    this.listConnections = function (dataDetails) {
        BackendService.sendRequest('/connections/listConnections', null, function (result) { });
    }

    this.cloneToTimezone = function (data, callOnSuccess) {
        BackendService.sendRequest('/data/cloneToTimezone', data, callOnSuccess);
    }

    this.cloneToTimezoneAction = function (symbol, action) {
        BackendService.sendRequest('/data/cloneToTimezoneAction', { symbol: symbol, action: action });
    }

    this.checkQualitySummary = function (symbol, timeframe, session, callOnSuccess) {
        BackendService.sendRequest('/data/checkQualitySummary', { symbol: symbol, timeframe: timeframe, session: session }, callOnSuccess);
    }
	
	this.getIndexForDate = function (symbol, timeframe, session, date, callOnSuccess) {
        BackendService.sendRequest('/data/getIndexForDate', { symbol: symbol, timeframe: timeframe, session: session, date:date }, callOnSuccess);
    }

    this.editData = function (dataDetails) {
        //correct data
        dataDetails.timeframe = dataDetails.timeframe || 0;
        dataDetails.rows = dataDetails.rows || 0;

        BackendService.sendRequest('/data/editData', dataDetails, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        });
    }
	
	this.saveDataChanges = function(data, callOnSuccess){
		BackendService.sendRequest('/data/saveDataChanges', data, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
				if (callOnSuccess){
					callOnSuccess();
				}
            }
        }, 'POST');
	}

    this.clearData = function (data) {
        BackendService.sendRequest('/data/clearData', data, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        }, 'POST');
    }
	
	this.cancelDataOperation = function () {
        BackendService.sendRequest('/data/cancelDataOperation', null);
    }

    this.removeData = function (data) {
        BackendService.sendRequest('/data/removeData', data, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        }, 'POST');
    }

    this.loadChartData = function (params, callback) {
        return BackendService.sendRequest('data/reviewChart', params, callback);
    }

    this.showData = function (data) {
        return BackendService.sendRequest('data/showData', data);
    }

    this.updateAll = function (data) {
        BackendService.sendRequest('/data/updateAll', null, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        });
    }
	
	this.stopAll = function (data) {
        BackendService.sendRequest('/data/stopAll', null, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        });
    }
	
	this.pauseAll = function (data) {
        BackendService.sendRequest('/data/pauseAll', null, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        });
    }	
	
	this.resumeAll = function (data) {
        BackendService.sendRequest('/data/resumeAll', null, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        });
    }	

    this.updateSelected = function (data) {
        BackendService.sendRequest('/data/updateSelected', data, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        }, 'POST');
    }

    this.save = function (data) {
        BackendService.sendRequest('/data/save', data, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        }, 'POST');
    }

    this.load = function (data) {
        BackendService.sendRequest('/data/load', data, function (result) {
            if (result.success) {
                $rootScope.showSuccess(result.success);
            }
        });
    }

    // utils ---------------------------------------------------------------------------------------------------------------
    this.filterSymbolsBySourceType = function (symbols, sourceType) {
        var symbolsNew = [];

        for (var i = 0; i < symbols.length; i++) {
            if (symbols[i].sourceType != sourceType) {
                continue;
            }

            symbolsNew.push(symbols[i]);
        }

        return symbolsNew;
    }

    this.getSymbolsWithConnections = function (symbols) {
        var connections = "";
        var symbolsTxt = "";

        for (var i = 0; i < symbols.length; i++) {
            symbolsTxt += symbols[i].symbol + ",";
            connections += symbols[i].connection + ",";
        }

        symbolsTxt = symbolsTxt.substr(0, symbolsTxt.length - 1);
        connections = connections.substr(0, connections.length - 1);

        return {
            connections: connections,
            symbols: symbolsTxt
        }
    }

    this.filterSymbolsInProgress = function (symbols, grid) {
        var symbolsNew = [];

        for (var i = 0; i < symbols.length; i++) {
            var progressPct = grid.getUserData(symbols[i].rowIndex, 'progress');
            if (progressPct !== null && progressPct >= 0 && progressPct < 100) {
                continue;
            }

            symbolsNew.push(symbols[i]);
        }

        if (symbolsNew.length == 0) {
            $rootScope.showError('Cannot start another action on data in progress.');
        }

        return symbolsNew;
    }

    this.containsClonedData = function (symbols) {
        for (var i = 0; i < symbols.length; i++) {
            if (symbols[i].sourceDataId) {
                return true;
            }
        }

        return false;
    }

    this.callAction = function (actionLink, symbol, action) {
        BackendService.sendRequest(actionLink, { symbol: symbol, action: action });
    }

    this.action = function (actionLink, symbol, action) {
        BackendService.sendRequest(actionLink, { symbol: symbol, action: action });
    }

    this.setParallelDownload = function (parallelDownload, cdnParallelDownload, cdnPreferred) {
        BackendService.sendRequest('dataSourceDukascopy/setParallelDownload', { parallelDownload: parallelDownload, cdnParallelDownload: cdnParallelDownload, cdnPreferred: cdnPreferred });
    }

    this.getParallelDownload = function (callback) {
        BackendService.sendRequest('dataSourceDukascopy/getParallelDownload', {}, callback);
    }
});