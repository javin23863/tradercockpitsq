angular.module('app.settings').service('SettingsNotificationService', function($q, BackendService, AppService) {

    function init() {
        var types = InitializationData().settings.Notification.types;
        loadToArray(instance.config.types, types);
    }
  
    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var notificationObj = AppService.getCurrentTaskTabSettings("Notification");
        if(!notificationObj) return;

        addNode('Type', instance.config.type, notificationObj, xmlDoc);
        
        var email = valueFilled(instance.config.email) ? instance.config.email : '';
        addNode('Email', email, notificationObj, xmlDoc);
        addNode('Wait', instance.config.wait, notificationObj, xmlDoc);
    }
    
    this.loadSettings = function() {
        var notificationObj = AppService.getCurrentTaskTabSettings("Notification");
       
        instance.config.type = getNodeValue(notificationObj, 'Type', 'email');
        instance.config.email = getNodeValue(notificationObj, 'Email', null);
        instance.config.wait = getNodeBooleanValue(notificationObj, 'Wait', false);
    }

    var instance = this;

    this.config = {
        types: [],
        type: 'email',
        email: null,
        wait: false
    }

    init();
});