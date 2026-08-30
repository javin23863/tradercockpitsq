angular.module('app.dashboard', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("AppPanel", 30, {
        dataItem: 'dashboard',
        templateUrl: '../../../plugins/DashboardPanel/dashboard.html',
        controller: 'DashboardCtrl',
    });
});