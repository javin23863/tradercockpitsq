angular.module('app').service('EditParametersService', function($rootScope, SQEvents, BackendService,AppService, SQConstants) {

    this.loadParameters = function(callback){
        BackendService.sendRequest("resultsDatabankActions/getStrategyParameters", instance.config, callback, 'POST');
    }

    this.saveParameters = function(callback){
        BackendService.sendRequest("resultsDatabankActions/setStrategyParameters", instance.config, callback, 'POST');
    }

    this.saveSort = sortValue => {
        AppService.saveSetting("editStrategyParamsSort", sortValue);
    }

    this.getSort = () => {
        let sort = SQConstants.getSettings()["editStrategyParamsSort"];
        return (sort!="0" && sort!="1")?"0":sort;
    }

    this.config = {
        project : '',
        databank : '',
        strategy : '',
        symmetricVariables : SQConstants.getSettings()["editStrategyParamsSymmetry"] == "true"
    }

    var instance = this;

});