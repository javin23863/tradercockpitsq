angular.module('app.settingsPanel', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("AppPanel", 60, {
        dataItem: 'settings',
        templateUrl: '../../../plugins/SettingsPanel/settings.html',
        controller: 'SettingsPanelCtrl',
    });
});