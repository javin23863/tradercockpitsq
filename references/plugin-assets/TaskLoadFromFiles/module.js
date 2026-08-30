angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'LoadFromFiles',
        templateUrl: '../../../plugins/TaskLoadFromFiles/simpleSettings/simpleSettings.html',
        controller: 'SimpleLoadFromFilesSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});