angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'DeleteFile',
        templateUrl: '../../../plugins/TaskDeleteFile/simpleSettings/simpleSettings.html',
        controller: 'SimpleDeleteFileSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});