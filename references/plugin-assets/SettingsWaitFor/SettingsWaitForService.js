angular.module('app.settings').service('SettingsWaitForService', function($q, BackendService, AppService, L) {

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var waitForObj = AppService.getCurrentTaskTabSettings("WaitFor");
        
        addNode('File', instance.config.file, waitForObj, xmlDoc);
        addNode('Action', instance.config.action, waitForObj, xmlDoc);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var waitForObj = AppService.getCurrentTaskTabSettings("WaitFor");
        if(!waitForObj) return;

        instance.config.file = getNodeValue(waitForObj, 'File', '');
        instance.config.action = getNodeValue(waitForObj, 'Action', 'user');
    }
	
    var instance = this;

    this.config = {
        file: '',
        action: 'user'
    }
});