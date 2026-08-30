angular.module('app.resultstabs.optimprof').service('OptManageViewsService', function($rootScope, SQEvents, $q, BackendService) {

    this.loadViews =function(callback) {
        BackendService.sendRequest('optimprof/viewsGetViews', null, function(result) {
            if(callback) {
                callback(result.views);
            }
        });
    }

    this.removeView = function(name){
        var deferred = $q.defer();

        BackendService.sendRequest('optimprof/viewsRemoveView', {"name": name}, function(result){
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    this.addView = function(viewXML){
        return BackendService.getPromise('optimprof/viewsAddView', {"viewXML": formatXML(viewXML)});
    }

    this.updateView = function(viewXML){
        return BackendService.getPromise('optimprof/viewsUpdateView', {"viewXML": formatXML(viewXML)});
    }

});