angular.module('app.settings').service('SettingsCallExternalScriptService', function($q, BackendService, AppService, L) {

    this.saveSettings = function() {
        var xmlDoc = AppService.xmlDoc;
        var callExternalScriptObj = AppService.getCurrentTaskTabSettings("CallExternalScript");
        
        var fileObj = addNode('File', instance.config.file, callExternalScriptObj, xmlDoc);
        fileObj.setAttribute("params", instance.config.params);

        var waitForObj = addNode('WaitFor', instance.config.waitfor, callExternalScriptObj, xmlDoc);
        waitForObj.setAttribute("hours", instance.config.hours);
        waitForObj.setAttribute("minutes", instance.config.minutes);
    }
    
    this.loadSettings = function() {
        var xmlDoc = AppService.xmlDoc;

        var callExternalScriptObj = AppService.getCurrentTaskTabSettings("CallExternalScript");
        if(!callExternalScriptObj) return;

        instance.config.file = getNodeValue(callExternalScriptObj, 'File', '');
        var fileObj = getChildElement(callExternalScriptObj, "File");
        instance.config.params = getAttrStringValue(fileObj, 'params', '');

        instance.config.waitfor = getNodeValue(callExternalScriptObj, 'WaitFor', 'end');

        var waitForObj = getChildElement(callExternalScriptObj, "WaitFor");
        instance.config.hours = getAttrIntValue(waitForObj, 'hours', 0);
        instance.config.minutes = getAttrIntValue(waitForObj, 'minutes', 0);
    }
	
    var instance = this;

    this.config = {
        file: '',
        params: '',
        waitfor: 'end',
        hours: 0,
        minutes: 0
    }
});