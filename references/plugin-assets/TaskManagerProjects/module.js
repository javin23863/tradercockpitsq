angular.module('app.tmprojects', ['sqplugin']).config(function(sqPluginProvider) {

    sqPluginProvider.plugin("AppPanel", 10, {
        dataItem: 'tmprojects',
        templateUrl: '../../../plugins/TaskManagerProjects/projects.html',
        controller: 'TMProjectsCtrl',
        product: 'TASKMANAGER'
    });
});