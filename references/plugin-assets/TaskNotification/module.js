angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 5, {
        taskType: 'Notification',
        templateUrl: '../../../plugins/TaskNotification/simpleSettings/simpleSettings.html',
        controller: 'SimpleNotificationSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');

            var groups = [
                {
                    title: L.tsq('Notification options'),
                    settings: [
                        { name : L.tsq("Notification"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Notification")) }
                    ]
                }
            ];
            
            return groups;
        }
    });
    
});