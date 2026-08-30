angular.module('app').service('CodeEditorService', function(BackendService) {
    this.getEditorAutocomplete = function(callOnSuccess){
		BackendService.sendRequest('codeeditor/editorAutocomplete', null, callOnSuccess);
	}
	
    this.getContent = function(file, callOnSuccess) {
        BackendService.sendRequest('codeeditor/getContent', {'file': file}, callOnSuccess);
    }   
    
    this.save = function(tab, callOnSuccess) {
        BackendService.sendRequest('codeeditor/save', tab, callOnSuccess, 'POST');
    }   
    
    this.saveAs = function(tab, callOnSuccess) {
        BackendService.sendRequest('codeeditor/saveAs', tab, callOnSuccess, 'POST');
    }   
    
    this.compile = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/compile', data, callOnSuccess, 'POST');
    }   

    this.reloadApp = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/reloadApp', data, callOnSuccess, 'POST');
    }   
    
    this.fixImports = function(data, callback) {
        BackendService.sendRequest('codeeditor/fixImports', data, callback, 'POST', true);
    }   

    this.compileAll = function(callback) {
        BackendService.sendRequest('codeeditor/compileAll', null, callback, 'GET');
    } 
    
    this.stopCompilation = function() {
        BackendService.sendRequest('codeeditor/stopCompilation');
    }    
    
    this.listTemplates = function(file, callOnSuccess) {
        BackendService.sendRequest('codeeditor/listTemplates', {'file': file}, callOnSuccess);
    }  

    this.listIndicators = function(callOnSuccess) {
        BackendService.sendRequest('codeeditor/listIndicators', null, callOnSuccess);
    }  
 
    this.listCodeTypes = function(callOnSuccess) {
        BackendService.sendRequest('codeeditor/listCodeTypes', null, callOnSuccess);
    } 
    
    this.findInFiles = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/findInFiles', data, callOnSuccess);
    }  

    this.createNew = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/createNew', data, callOnSuccess);
    } 
    
    this.createNewFile = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/createNewFile', data, callOnSuccess);
    }  
    
    this.createNewDir = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/createNewDir', data, callOnSuccess);
    } 
    
    this.clone = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/clone', data, callOnSuccess);
    }
    
    this.rename = function(data, callOnSuccess) {
        BackendService.sendRequest('codeeditor/rename', data, callOnSuccess);
    }  
    
    this.delete = function(file, callOnSuccess) {
        BackendService.sendRequest('codeeditor/delete', {file : file}, callOnSuccess);
    }
    
    this.reload = function(file, callOnSuccess) {
        BackendService.sendRequest('codeeditor/reload', {file : file}, callOnSuccess);
    }

    this.createNewTemplate = function(file, callOnSuccess) {
        BackendService.sendRequest('codeeditor/createNewTemplate', {file : file}, callOnSuccess);
    }   

    this.saveLastOpenedFiles = function(files) {
        BackendService.sendRequest('codeeditor/saveLastOpenedFiles', {files : files});
    }   

    this.loadLastOpenedFiles = function(callOnSuccess) {
        BackendService.sendRequest('codeeditor/loadLastOpenedFiles', null, callOnSuccess);
    }   

    this.compilePlugin = function(file, callOnSuccess) {
        BackendService.sendRequest('codeeditor/compilePlugin', {file : file}, callOnSuccess);
    }       

    this.showInNavigator = null;
    
    this.mainTabs = null;

    this.openedTabs = [];
});
    