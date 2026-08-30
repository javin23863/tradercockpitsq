angular.module('app.resultstabs.robustnesstests').service('RTManageViewsService', function($rootScope, SQEvents, $q, BackendService) {

    this.loadViews =function(type, callback) {
        BackendService.sendRequest('rtresults/viewsGetViews', {"type": type}, function(result) {
            if(callback) {
                callback(result.views);
            }
        });
    }

    this.removeView = function(name, type){
        var deferred = $q.defer();

        BackendService.sendRequest('rtresults/viewsRemoveView', {"name": name, "type": type}, function(result){
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    this.addView = function(viewXML, type){
        return BackendService.getPromise('rtresults/viewsAddView', {"viewXML": formatXML(viewXML), "type": type});
    }

    this.updateView = function(viewXML, type){
        return BackendService.getPromise('rtresults/viewsUpdateView', {"viewXML": formatXML(viewXML), "type": type});
    }

});