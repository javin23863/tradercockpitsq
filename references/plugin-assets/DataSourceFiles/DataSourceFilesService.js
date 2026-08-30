angular.module('app.datasource.files').service('DataSourceFilesService', function($rootScope, $q, BackendService) {
    
        // add ----------------------------------------------------------------------------------------------------------------
        this.add = function(dataDetails){
            BackendService.sendRequest('/dataSourceFiles/add', dataDetails, function(result){
                if(result.success){
                    $rootScope.showSuccess(result.success);
                }
            });
        }

        // import --------------------------------------------------------------------------------------------------------------
        var importInfo;

        function importGetInfo(){
            var deferred = $q.defer();

            BackendService.getPromise('/dataSourceFiles/importGetInfo', {}, 'POST').then(function(result){
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
            return BackendService.getPromise('/dataSourceFiles/import', config);
        }

        this.importAction = function(symbol, action) {
            BackendService.sendRequest('/dataSourceFiles/importAction', {symbol: symbol, action: action});
        }

        this.importGetOverview = function(params){
            return BackendService.getPromise('/dataSourceFiles/importGetOverview', params);
        }

        this.importSaveNewDataFormat = function(params){
            return BackendService.getPromise('/dataSourceFiles/importSaveNewDataFormat', params);
        }

        this.importUpdateDataFormat = function(params, callback){
            BackendService.sendRequest('/dataSourceFiles/importUpdateDataFormat', params, callback);
        }
        
        this.importDeleteDataFormat = function(formatName, callback){
            BackendService.sendRequest('/dataSourceFiles/importDeleteDataFormat', { name : formatName }, callback);
        }

        this.massImport = function(params, callback){
            BackendService.sendRequest('/dataSourceFiles/massImport', params, callback);
        }

        this.appImport = function(params, callback){
            BackendService.sendRequest('/dataSourceFiles/appImport', params, callback);
        }

        this.cancelAppImport = function(params, callback){
            BackendService.sendRequest('/dataSourceFiles/cancelAppImport', params, callback);
        }
});