angular.module('app.resultsdatabankactions.save', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 20, {
        title: Ltsq("Save")+":",
        id: "databank-action-save-menu"
    });
    
    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/save/savePopup.html", 'saveBtnPopupCtrl', 'SQUANT');
    
});
