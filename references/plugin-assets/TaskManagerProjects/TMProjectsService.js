angular.module('app.tmprojects').service('TMProjectsService', function($rootScope, $q, BackendService) {

    this.loadProjects = function(callOnSuccess) {
        BackendService.sendRequest('taskmanager/listProjects', null, callOnSuccess);
    }

    this.addProject = function(projectName, callOnSuccess) {
        BackendService.sendRequest('taskmanager/createProject', {projectName: projectName}, callOnSuccess);
    }

    this.removeProject = function(projectName, callOnSuccess) {
       BackendService.sendRequest('taskmanager/removeProject', {projectName: projectName}, callOnSuccess);
    }

    this.renameProject = function(oldName, newName, callOnSuccess) {
       BackendService.sendRequest('taskmanager/renameProject', {oldName: oldName, newName: newName}, callOnSuccess);
    }

    this.cloneProject = function(projectName, callOnSuccess) {
       BackendService.sendRequest('taskmanager/cloneProject', {projectName: projectName}, callOnSuccess);
    }

    this.openProject = function(file, loadAsIs, callOnSuccess) {
        BackendService.sendRequest('taskmanager/openProject', {file: file, loadAsIs: loadAsIs}, callOnSuccess);
    }

    this.saveProject = function(projectName, file, callOnSuccess) {
        BackendService.sendRequest('taskmanager/saveProject', {projectName: projectName, file: file}, callOnSuccess);
    }

    this.modifySymbolsList = function(projectName, callOnSuccess) {
        BackendService.sendRequest('taskmanager/modifySymbolsList', {projectName: projectName}, callOnSuccess);
    }

    this.modifySymbols = function(data, callOnSuccess) {
        BackendService.sendRequest('taskmanager/modifySymbols', data, callOnSuccess);
    }

    this.getXMLConfig = function(data, callOnSuccess) {
        BackendService.sendRequest('/project/getXMLConfig', data, callOnSuccess)
    }

    this.checkResources = function(data, callOnSuccess) {
        BackendService.sendRequest('/project/checkResources', data, callOnSuccess)
    }

    var instance = this;

    this.projects = [];
    this.lastProjectStats = [];

});