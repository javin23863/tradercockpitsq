angular.module('app.resultstabs.wf').controller('ManageViewsCtrlWF', function ($scope, $rootScope, $timeout, $q, SQEvents, ManageViewsServiceWF, SQConstants, L) {
    console.log("ManageViewsCtrlWF controller initialized");
    
    $scope.type = "wfresults";

    function init(callback) {
        ManageViewsServiceWF.loadViews(function(views) {
            loadToArray($scope.views, views);
            
            $scope.onViewChange();

            viewsDeferred.resolve();
            if(callback) callback();
        }); 
    }

    $scope.onManageViews = function(){
        showPopup("#manageViews" + $scope.type);
    }

    $scope.onViewChange = function() {
        if($scope.manageViewsDialog.selectView){
            $scope.manageViewsDialog.selectView($scope.selectedView);
        }
        $scope.instance.viewChanged(); 
    }

    $scope.instance.getSelectedViewName = function() {
        return $scope.selectedView;  
    }
 
    $scope.instance.getSelectedView = function() {
        return getItem($scope.views, 'name', $scope.selectedView);  
    }

    //- Initialization --------------------------------------------------------------

    $scope.manageViewsDialog = {
        getColumns : function(callback){
            var columnTypes = SQConstants.getConstants().databankColumnTypes; 
            var columns = SQConstants.getColumnsByType([columnTypes.general]);  

            callback(angular.copy(columns));
        },
        getViews : function(callback){
            viewsDeferred.promise.then(function(){
                callback($scope.views, $scope.selectedView);
            });
        },
        addView : function(viewName, viewXML, callback) {
            $scope.selectedView = viewName;
            
            ManageViewsServiceWF.addView(viewXML).then(function(result){
                if(result.data.success){
                    $rootScope.showSuccess(L.tsq("New view saved."));
                    init(callback);
                }
                else {
                    $rootScope.showError(result.data.error);
                }
            });
        }, 
        onViewRename : function(oldName, newName){
            $scope.selectedView = newName;
        },
        updateView : function(viewName, viewXML, callback) {
            $scope.selectedView = viewName;

            ManageViewsServiceWF.updateView(viewXML).then(function(result){
                if(result.data.success){
                    $rootScope.showSuccess(L.tsq("View updated."));
                    init(callback);
                }
                else {
                    $rootScope.showError(result.data.error);
                }
            });
        },
        removeView : function(viewName, callback) {
            $scope.selectedView = 'Default';

            ManageViewsServiceWF.removeView(viewName).then(function(result){
                if(!result.success){
                    $rootScope.showError(L.tsq("Cannot delete view.") +" "+ result.error);
                }
                else {
                    init(callback);
                }
            });
        }
    };

    $scope.selectedView = SQConstants.getSettings().wfResultsView || 'Default';
    $scope.views = [];

    var viewsDeferred = $q.defer();
    init($scope.instance.initCallback);

});