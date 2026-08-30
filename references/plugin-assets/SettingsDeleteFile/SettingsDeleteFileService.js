angular.module('app.settings').service('SettingsDeleteFileService', function($q, BackendService, AppService, L) {

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var deleteFileObj = AppService.getCurrentTaskTabSettings("DeleteFile");
        
        addNode('File', instance.config.file, deleteFileObj, xmlDoc);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var deleteFileObj = AppService.getCurrentTaskTabSettings("DeleteFile");
        if(!deleteFileObj) return;

        instance.config.file = getNodeValue(deleteFileObj, 'File', '');
    }
	
    var instance = this;

    this.config = {
        file: ''
    }
});