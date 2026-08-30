angular.module('app.results.chart', []).service('ResultsChartService', function(BackendService) {

    this.cancelLoading = function(){
        return BackendService.getPromise('resultsCharts/cancelLoading');
    }

    this.loadChartData = function(params){
        return BackendService.getPromise('resultsCharts/loadChartData', params, 'POST');
    }

    var instance = this;

});