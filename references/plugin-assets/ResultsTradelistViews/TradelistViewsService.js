angular.module('app.tradelist.views').service('TradelistViewsService', function($rootScope, SQEvents, $q, SQConstants, BackendService) {

    this.removeView = function(name){
        var deferred = $q.defer();
        var args = {name: name};

        BackendService.sendRequest('/tradelistviews/removeView', args, function(result){
            deferred.resolve(result);
        });

        return deferred.promise;
    }

    this.addView = function(viewXML){
        var args = {
            viewXML: formatXML(viewXML)
        };

        return BackendService.getPromise('/tradelistviews/addView', args, 'POST');
    }

    this.updateView = function(viewXML){
        var args = {
            viewXML: formatXML(viewXML)
        };

        return BackendService.getPromise('/tradelistviews/updateView', args, 'POST');
    }

    this.changeView = function(viewName){
        var args = {
            viewName: viewName
        };

        BackendService.sendRequest('/tradelistviews/changeView', args, function(result){
            if(result.success){
                SQConstants.getSettings().tradelistView = viewName;
                SQEvents.notifyListeners(SQEvents.get("TRADELIST_VIEW_CHANGED"), viewName);
            }
        });
    }

    var instance = this;

});