angular.module('app.settings').config(function(sqPluginProvider, SQEventsProvider) {
    
    SQEventsProvider.add('FILTERING_CONDITIONS_CHANGED', 'filtering-conditions-changed');

    sqPluginProvider.plugin("SimpleTaskSettings", 3, {
        taskType: 'Filtering',
        templateUrl: '../../../plugins/TaskFiltering/simpleSettings/simpleSettings.html',
        controller: 'SimpleFilteringSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            var L = injector.get("L");
            var settingsPlugins = sqPluginProvider.getPlugins('SettingsTab');

            var groups = [
                {
                    title: L.tsq('Filtering options'),
                    settings: [
                        { name : L.tsq("Filtering"), value : getTaskInfoPanelSettings(xmlConfig, getItem(settingsPlugins, 'configElemName', "Filtering")) }
                    ]
                }
            ];
            
            return groups;
        }
    });
    
});