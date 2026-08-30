angular.module('app.databank.views').controller('DatabankViewsCtrl', function ($rootScope, $scope, $timeout, DatabankViewsService, SQEvents, SQConstants, AppService, L) {
    console.log("Databank views ctrl initialized");

    //- Event Handlers --------------------------------------------------------------

    function onDatabankViewsEvent(event, data) {
        if (event == SQEvents.get('DB_VIEW_CHANGED') || event == SQEvents.get('DB_VIEW_EDIT')) {
            if($scope.manageViewsDialog.selectView){
                $scope.manageViewsDialog.selectView(data);
            }
            selectedView = data;
        }
        else if (event == SQEvents.get('DATABANK_CHANGED')) {
            if (data){
                if($scope.manageViewsDialog.selectView) {
                    $scope.manageViewsDialog.selectView(data.view);
                }
                selectedView = data.view;
            }
            else return;
        }
        else if (event == SQEvents.get('DATABANK_VIEWS_LOADED')) {
            if($scope.manageViewsDialog.reloadViews){
                $scope.manageViewsDialog.reloadViews();
            }
        }
        else return;

        try { $scope.$digest(); } catch (err) { }
    }

    //- Initialization --------------------------------------------------------------

    $scope.manageViewsDialog = {
        getColumns : function(callback){
            var columnTypes = SQConstants.getConstants().databankColumnTypes; 
            var columns = SQConstants.getColumnsByType([columnTypes.general]);  

            callback(angular.copy(columns));
        },
        getViews : function(callback){
            callback(angular.copy(SQConstants.getConstants().databankViews), selectedView);
        },
        addView : function(viewName, viewXML, callback){
            DatabankViewsService.addView(viewXML, function () {
                $rootScope.showSuccess(L.tsq("New view saved."));
                SQEvents.notifyListeners(SQEvents.get("DATABANK_SET_VIEW"), { project : AppService.getProject(), viewName : viewName });
                callback();
            });
        },
        onViewRename : function(oldName, newName){
            SQEvents.notifyListeners(SQEvents.get("DATABANK_SET_VIEW"), { project : AppService.getProject(), viewName : newName, viewRenamed : true });
        },
        updateView : function(viewName, viewXML, callback){
            DatabankViewsService.updateView(viewXML, function () {
                $rootScope.showSuccess(L.tsq("View updated."));
                callback();
            });
        },
        removeView : function(viewName, callback){
            DatabankViewsService.removeView(viewName, function () {
                $rootScope.showSuccess(L.tsq("View deleted"));
            });
        }
    };

    var selectedView = 'Default';

    SQEvents.addListener('DatabankViewsCtrl', [
        SQEvents.get('DB_VIEW_EDIT'),
        SQEvents.get('DB_VIEW_CHANGED'),
        SQEvents.get('DATABANK_CHANGED'),
        SQEvents.get("DATABANK_VIEWS_LOADED")
    ], onDatabankViewsEvent);


});