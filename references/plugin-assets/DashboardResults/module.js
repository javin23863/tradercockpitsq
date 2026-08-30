//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.dashboardResults', ['sqplugin']).config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("MainPanel", 20, {
        templateUrl: '../../../plugins/DashboardResults/dashboardResults.html',
        controller: 'DashboardResultsCtrl',
        class: { 'sqn-results' : true },
        name: 'results'
    });
});