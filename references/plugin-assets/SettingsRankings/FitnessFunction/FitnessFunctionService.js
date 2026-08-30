angular.module('app.settings').service('FitnessFunctionService', function(BackendService, AppService, SQConstants) {
    var instance = this;
    
    this.methods = InitializationData().settings.Rankings.fitnessMethods;
});