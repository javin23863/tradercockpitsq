angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'UpdateData',
        templateUrl: '../../../plugins/TaskUpdateData/simpleSettings/simpleSettings.html',
        controller: 'SimpleUpdateDataSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});