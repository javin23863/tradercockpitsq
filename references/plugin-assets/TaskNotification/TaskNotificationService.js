angular.module('app.settings').service('TaskNotificationService', function ($rootScope, AppService, SettingsNotificationService) {
   
    this.loadSettings = function() {
        SettingsNotificationService.loadSettings();
    }

    this.saveSettings = function() {
       SettingsNotificationService.saveSettings();
    }

    this.config = SettingsNotificationService.config;

    var instance = this;

});