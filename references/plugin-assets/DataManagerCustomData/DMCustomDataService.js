angular.module('app.customdata').service('DMCustomDataService', function ($rootScope, $q, BackendService) {

        this.add = function(data, callback){
            BackendService.sendRequest('/customdata/add', data, callback);
        }

        this.edit = function(data, callback){
            BackendService.sendRequest('/customdata/edit', data, callback);
        }

        this.clearData = function (data) {
            BackendService.sendRequest('/customdata/clear', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            }, 'POST');
        }

        this.removeData = function (data) {
            BackendService.sendRequest('/customdata/remove', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            }, 'POST');
        }

          // import --------------------------------------------------------------------------------------------------------------
        var importInfo;

        function importGetInfo(){
            var deferred = $q.defer();

            BackendService.getPromise('/customdata/importGetInfo', {}, 'POST').then(function(result){
                if(result.success){
                    importInfo = result.data.importInfo;
                }
                else {
                    $rootScope.showError(result.data.error);
                }

                deferred.resolve(importInfo);
            });

            return deferred.promise;
        }

        this.importGetInfo = function(){
            if(!importInfo){
                return importGetInfo();
            }
            else return importInfo;
        }

        this.import = function(config){
            return BackendService.getPromise('/customdata/import', config);
        }

        this.importAction = function(data, action) {
            BackendService.sendRequest('/customdata/importAction', {data: data, action: action});
        }

        this.importGetOverview = function(params){
            return BackendService.getPromise('/customdata/importGetOverview', params);
        }

        this.importSaveNewDataFormat = function(params){
            return BackendService.getPromise('/customdata/importSaveNewDataFormat', params);
        }

        this.importUpdateDataFormat = function(params, callback){
            BackendService.sendRequest('/customdata/importUpdateDataFormat', params, callback);
        }
        
        this.importDeleteDataFormat = function(formatName, callback){
            BackendService.sendRequest('/customdata/importDeleteDataFormat', { name : formatName }, callback);
        }

        this.recognizeFromFile = function(filePath, callback){
            BackendService.sendRequest('/customdata/recognizeFromFile', { filePath : filePath }, callback);
        }

        this.save = function (data) {
            BackendService.sendRequest('/customdata/save', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }

        this.load = function (data) {
            BackendService.sendRequest('/customdata/load', data, function (result) {
                if (result.success) {
                    $rootScope.showSuccess(result.success);
                }
            });
        }
});