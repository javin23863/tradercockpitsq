angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'ApplyMassConfig',
        templateUrl: '../../../plugins/TaskApplyMassConfig/simpleSettings/simpleSettings.html',
        controller: 'SimpleApplyMassConfigCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});