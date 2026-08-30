//product:SQUANT
angular.module('app.projectresources', []).config(function(sqPluginProvider, SQEventsProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/ProjectResources/resolveResourcesPopup.html", 'ResolveResourcesPopupCtrl', 'SQUANT');
    sqPluginProvider.addPopupWindow("../../../plugins/ProjectResources/resolveCustomResourcesPopup.html", 'ResolveCustomResourcesPopupCtrl', 'SQUANT');
    sqPluginProvider.addPopupWindow("../../../plugins/ProjectResources/addedResourcesPopup.html", 'AddedResourcesPopupCtrl', 'SQUANT');

});