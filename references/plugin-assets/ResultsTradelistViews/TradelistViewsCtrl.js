angular.module('app.tradelist.views').controller('TradelistViewsCtrl', function ($rootScope, $scope, $timeout, TradelistViewsService, SQEvents, SQConstants, L) {
    console.log("Tradelist views ctrl initialized");

    //- Event Handlers --------------------------------------------------------------

    function onEvent(event, data) {
        if (event == SQEvents.get('TRADELIST_VIEWS_LOADED')) {
            if($scope.manageViewsDialog.reloadViews){
                $scope.manageViewsDialog.reloadViews();
            }
        }
        else if (event == SQEvents.get('TRADELIST_VIEW_CHANGED')) {
            if($scope.manageViewsDialog.selectView){
                $scope.manageViewsDialog.selectView(data);
            }
        }
        else return;

        try { $scope.$digest(); } catch (er) { }
    }

    $scope.$on('$destroy', function () {
        SQEvents.removeListener(listenerId);
    });

    //- Initialization --------------------------------------------------------------

    $scope.manageViewsDialog = {
        getColumns : function(callback){
            var columnsAll = [];
            
            var columns = angular.copy(SQConstants.getColumns().tradelistColumns);
            for (var i = 0; i < columns.length; i++) {
                var column = columns[i];
                column.value = column.class;

                columnsAll.push(column);
            }
            
            callback(columnsAll);
        },
        getViews : function(callback){
            var settings = SQConstants.getSettings();
            callback(angular.copy(SQConstants.getConstants().tradelistViews), settings.tradelistView ? settings.tradelistView : 'Default');
        },
        addView : function(viewName, viewXML, callback){
            SQConstants.getSettings().tradelistView = viewName;
            TradelistViewsService.addView(viewXML).then(function (result) {
                if (result.data.success) {
                    if(result.data.view){     //AW Cloud special handling - there are no websocket updates
                        SQConstants.getConstants()["tradelistViews"].push(result.data.view);
                        SQEvents.notifyListeners(SQEvents.get("TRADELIST_VIEWS_LOADED"), SQConstants.getConstants()["tradelistViews"]);
                    }

                    $rootScope.showSuccess(L.tsq("New view saved."));
                    callback();
                }
                else {
                    $rootScope.showError(result.data.error);
                }
            });
        },
        updateView : function(viewName, viewXML, callback){
            TradelistViewsService.updateView(viewXML).then(function (result) {
                if (result.data.success) {
                    if(result.data.tradelistViews){     //AW Cloud special handling - there are no websocket updates
                        SQConstants.getConstants()["tradelistViews"] = result.data.tradelistViews;
                        SQEvents.notifyListeners(SQEvents.get("TRADELIST_VIEWS_LOADED"), result.data.tradelistViews);
                    }

                    $rootScope.showSuccess(L.tsq("View updated."));
                    callback();
                }
                else {
                    $rootScope.showError(result.data.error);
                }
            });
        },
        onViewRename : function(oldName, newName){
            SQConstants.getSettings().tradelistView = newName;
        },
        removeView : function(viewName, callback){
            TradelistViewsService.removeView(viewName).then(function (result) {
                if (!result.success) {
                    $rootScope.showError(L.tsq("Cannot delete view. %s", [result.error]));
                }
                else {
                    if(result.tradelistViews){     //AW Cloud special handling - there are no websocket updates
                        SQConstants.getConstants()["tradelistViews"] = result.tradelistViews;
                        SQEvents.notifyListeners(SQEvents.get("TRADELIST_VIEWS_LOADED"), result.tradelistViews);
                    }
                    
                    $rootScope.showSuccess(L.tsq("Tradelist view removed"));
                    callback();
                }
            });
        }
    };

    var listenerId = "TradelistViewsCtrl";
    SQEvents.addListener(listenerId, [
        SQEvents.get('TRADELIST_VIEWS_LOADED'),
        SQEvents.get("TRADELIST_VIEW_CHANGED"),
        SQEvents.get("STRATEGY_SELECTED")
    ], onEvent);

});