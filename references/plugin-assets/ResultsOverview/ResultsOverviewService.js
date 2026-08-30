angular.module('app.resultstabs.overview').service('ResultsOverviewService', function(BackendService, $rootScope, $q) {
    
    this.getOverviewContent = function(params){
        var deferred = $q.defer();
        var url = 'overview/getOverviewContent';
        
        params.reportName = params.strategyName;

        BackendService.getPromise(url, params).then(function(result){
            if(result.data.success){
                deferred.resolve({
                    success: true,
                    overviewHtml: result.data.overviewHtml
                });
            }
            else {
                deferred.resolve({
                    success: false,
                    error: result.data.error
                });
            }
        });
        
        return deferred.promise;
    } 
});
    