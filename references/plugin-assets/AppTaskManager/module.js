//product:SQUANT
angular.module('app.taskManager', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {
    
    SQEventsProvider.add("PROJECT_CHANGED", 'project-changed');

    sqPluginProvider.plugin("NavigationPlugin", 20, {
        title: Ltsq('Custom Projects'),
        appCode: 'TASKMANAGER',
        url: '../TASKMANAGER/index.html',
        iconClass: 'fa-tasks',
        section: 1,
        iframeId: 'task-manager-frame',
        linkId: 'task-manager-link',
        onlyInPro: true
    });

});