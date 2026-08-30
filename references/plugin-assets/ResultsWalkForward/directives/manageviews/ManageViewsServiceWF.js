angular.module('app.resultstabs.wf').service('ManageViewsServiceWF', function($rootScope, SQEvents, $q, BackendService) {

    this.loadViews =function(callback) {
        BackendService.sendRequest('walkforward/viewsGetViews', null, function(result) {
            if(callback) {
                callback(result.views);
            }
        });
    }

    this.removeView = function(name){
        var deferred = $q.defer();

        BackendService.sendRequest('walkforward/viewsRemoveView', {"name": name}, function(result){
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    this.addView = function(viewXML){
        return BackendService.getPromise('walkforward/viewsAddView', {"viewXML": formatXML(viewXML)});
    }

    this.updateView = function(viewXML){
        return BackendService.getPromise('walkforward/viewsUpdateView', {"viewXML": formatXML(viewXML)});
    }

});