//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.settings', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {
	
    SQEventsProvider.add('SETTINGS_TAB_ACTIVE', 'settings-tab-active'); 
    SQEventsProvider.add('SETTINGS_TAB_RELOAD', 'settings-tab-reload'); 
    SQEventsProvider.add('SETTINGS_TAB_SHOW', 'settings-tab-show'); 
    SQEventsProvider.add('SETTINGS_CLOSE', 'settings-close'); 
    SQEventsProvider.add('SETTINGS_VIEW_TOGGLE', 'settings-view-toggle');

    SQEventsProvider.add('SETTINGS_TASK_CHANGED', 'settings-task-changed');
    SQEventsProvider.add('SIMPLE_SETTINGS_UPDATE', 'simple-settings-update');
    
    SQEventsProvider.add('TASK_INFO_UPDATE', 'task-info-update');

    SQEventsProvider.add('PROJECT_DISABLE_START', 'project-disable-start');     //disables start button, promise to reenable the button is expected as an argument 

	sqPluginProvider.plugin("MainPanel", 10, {
        templateUrl: '../../../plugins/ProjectSettings/views/settings.html',
        controller: 'SettingsCtrl',
        class: { 'sqn-settings' : true },
        name: 'settings'
    });
});