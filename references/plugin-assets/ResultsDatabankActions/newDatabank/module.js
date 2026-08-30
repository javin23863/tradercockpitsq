//product:BUILDER
angular.module('app.resultsdatabankactions.newDatabank', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/newDatabank/newDatabankPopup.html", 'NewDatabankPopupCtrl', 'SQUANT');
    
});
