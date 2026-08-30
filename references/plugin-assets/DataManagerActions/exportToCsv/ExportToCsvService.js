angular.module('app.data').service('ExportToCsvService', function(SQEvents, SQConstants, BackendService) {

    this.export = function(data, callOnSuccess) {
        BackendService.sendRequest('/data/exportToCsv', data, callOnSuccess);
    }

    this.loadSettings = function(callOnSuccess) {
        BackendService.sendRequest('/data/exportToCsvLoadSettings', null, callOnSuccess);
    }

    this.saveFileFormat = function(data, callOnSuccess) {
        BackendService.sendRequest('/data/exportToCsvSaveFileFormat', data, callOnSuccess);
    }

    this.saveAsFileFormat = function(data, callOnSuccess) {
        BackendService.sendRequest('/data/exportToCsvSaveAsFileFormat', data, callOnSuccess);
    }

    this.deleteFileFormat = function(fileFormat, callOnSuccess) {
        BackendService.sendRequest('/data/exportToCsvDeleteFileFormat', {fileFormat: fileFormat}, callOnSuccess);
    }

    this.action = function(symbol, action, removeFile) {
        return BackendService.getPromise('/data/exportToCsvAction', {symbol: symbol, action: action, removeFile : removeFile});
    }
    
    function onEvent(event, data) {
        if (event == SQEvents.get('SESSIONS_CHANGED')) {
            loadToArray(instance.sessions, data);
        }
    }

    var instance = this;
    
    this.sessions = SQConstants.getConstants().sessions;

    SQEvents.addListener('ExportToCsvService', [SQEvents.get('SESSIONS_CHANGED')], onEvent);

});