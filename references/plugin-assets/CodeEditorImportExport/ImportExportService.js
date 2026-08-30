angular.module('app').service('ImportExportService', function($rootScope, SQConstants, BackendService, L) {

    this.import = function(files, callback){
        BackendService.getPromise("/importexport/import", { files : arrayToStr(files) }).then(function(result){
            if(result.data.error){
                hidePopup("#CEOverwriteConfirmPopup");
                $rootScope.showError(result.data.error);
            }
            else {
                if(callback) callback(result);
            }
        });
    }

    this.export = function(files, targetPath, callback){
        BackendService.sendRequest("/importexport/export", { files : arrayToStr(files), targetPath : targetPath }, callback, "POST");
    }

    this.listFiles = function(filter, callback){
        BackendService.sendRequest("/importexport/list", { filter : filter}, callback);
    }

    this.onOverwrite = function(){
        BackendService.sendRequest("/importexport/overwrite", {}, closePopup);
    }

    this.onOverwriteAlways = function(){
        BackendService.sendRequest("/importexport/overwriteAlways", {}, closePopup);
    }

    this.onSkip = function(){
        BackendService.sendRequest("/importexport/skip", {}, closePopup);
    }

    this.onSkipAlways = function(){
        BackendService.sendRequest("/importexport/skipAlways", {}, closePopup);
    }

    this.onCancel = function(){
        BackendService.sendRequest("/importexport/cancel", {}, closePopup);
    }

    this.IDsToPaths = function(ids, treeObj){
        var paths = [];
        var appPath = SQConstants.getSettings().appPath.replace(/\\/g, "/");

        if(appPath.lastIndexOf("/") != appPath.length - 1){
            appPath += "/";
        }

        for(var i=0; i<ids.length; i++){
            var id = ids[i];
            var filePath = treeObj.getUserData(id, "file").replace(/\\/g, "/");
            
            paths.push(filePath.replace(appPath, ""));
        }

        return paths;
    }

    function closePopup(){
        hidePopup("#CEOverwriteConfirmPopup");
    }

    function arrayToStr(array){
        var str = "";

        for(var i=0; i<array.length; i++){
            str += (i > 0 ? "," : "") + array[i];
        }

        return str;
    }

    this.pathKey = "CEImportExport";
    this.fileExtension = {
        name : 'sxp',
        description : L.tsq('SQ Extension files')
    };

});