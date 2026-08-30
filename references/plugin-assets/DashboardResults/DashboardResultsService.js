angular.module('app.dashboardResults').service('DashboardResultsService', function(BackendService, SQConstants) {
    
    this.getSettings = function(data, callOnSuccess) {
        BackendService.sendRequest('dashboardResults/getSettings', data, callOnSuccess);
    } 

    this.projectChanged = function(projectName) {
        BackendService.sendRequest('/project/projectChanged', {projectName: projectName}, null);
    } 

    function printResults() {
        var data = {
            sampleType: instance.sampleType,
            projectName: window.appConfig.project
        }

        BackendService.sendRequest('dashboardResults/print', data);
    }

    this.setSampleType = function(sampleType) {
        instance.sampleType = sampleType;

        printResults();
    }

    var instance = this;

    var sampleTypes = SQConstants.getConstants().sampleTypes;
    this.sampleType = sampleTypes.full;
});