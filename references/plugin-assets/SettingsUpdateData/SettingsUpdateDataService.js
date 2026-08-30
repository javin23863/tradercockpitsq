angular.module('app.settings').service('SettingsUpdateDataService', function($q, BackendService, AppService, L, DatabankService, SQEvents) {

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var updateDataObj = AppService.getCurrentTaskTabSettings("UpdateData");

        addNode('Type', instance.config.type, updateDataObj, xmlDoc);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var updateDataObj = AppService.getCurrentTaskTabSettings("UpdateData");
        if(!updateDataObj) return;

        instance.config.type = getNodeValue(updateDataObj, 'Type', 'project');
    }

    var instance = this;

    this.config = {
        type: 'project'
    }
});