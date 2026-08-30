angular.module('app.resultstabs.resultsplugins').service('ProfileChartService', function(BackendService) {

    /**
     * Get list of profile chart paths (filenames) and base URL for the current result.
     * Params: projectName, databankName, strategyName.
     * Callback receives response with profileChartPaths (array) and baseUrl.
     */
    this.getPaths = function(params, callback) {
        BackendService.sendRequest('profilechart/paths', params, callback);
    };
});
