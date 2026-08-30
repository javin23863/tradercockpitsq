angular.module('app.portfoliocomposer').service('PortfolioComposerService', function($rootScope, $q, BackendService, SQEvents) {
    
    SQEvents.add("PORTFOLIO_COMPOSER_WEIGHTS", "portfolio-composer-weights");

    this.addBuyHoldStrategy = function(data, callback) {
        BackendService.sendRequest('/portfoliocomposer/addBuyHoldStrategy', data, callback, 'POST');
    }

    this.recompute = function(data, callback) {
        BackendService.sendRequest('/portfoliocomposer/recompute', data, callback, 'POST');
    }

    this.stop = function() {
        BackendService.sendRequest('/portfoliocomposer/stop');
    }

    this.loadFiles = function(filePaths, callback) {
        var data = {
            filePaths: filePaths
        }

        BackendService.sendRequest('/portfoliocomposer/loadFiles', data, callback, 'POST');
    }

    this.saveFiles = function(data, callback) {
        BackendService.sendRequest('/project/saveReports', data, callback, 'POST');
    }

    this.savePortfolio = function(data, callback) {
        BackendService.sendRequest('/portfoliocomposer/savePortfolio', data, callback, 'POST');
    }

    this.loadGridData = function(callback) {
        BackendService.sendRequest('/portfoliocomposer/loadGridData', null, callback, 'GET');
    }

    this.removeAllReports = function(callback) {
        BackendService.sendRequest('/portfoliocomposer/removeAllReports', null, callback, 'GET');
    }

    this.removeAllReports = function(callback) {
        BackendService.sendRequest('/portfoliocomposer/removeAllReports', null, callback, 'GET');
    }

    this.removeReports = function(data, callback) {
        BackendService.sendRequest('/portfoliocomposer/removeReports', data, callback, 'POST');
    }

    this.loadFiles = function (filePaths) {
        var deferred = $q.defer();
        var data = {
            filePaths: filePaths
        }
    
        BackendService.getPromise('/portfoliocomposer/loadFiles', data, 'POST').then(function (result) {
            if (result.data.success) {
                console.log("Files loaded.");
                deferred.resolve({
                    success: true,
                    rows: result.data.rows
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
                console.log("Files not loaded. Error: " + result.data.error);
            }
        });
    
        return deferred.promise;
    }

    var instance = this;

    instance.config = {
        dateRange: "full",
        startDay: timeToDateString(new Date().getTime()),
        endDay: timeToDateString(new Date().getTime()),
        initialCapital: 100000,
        leverage: 1,
        results: [],
        weights: [],
        mmMethods: [],
        mmMethodsDifferent: false,
        portfolioName: null,

        //automatic computation
        modelType: "markowitz",
        selectionType: "SharpRatio", 
        maxSimulations: 10,
        confLevel: 95,
        riskFreeRate: 0.7,
        reqStdDev: -1
    }
});
