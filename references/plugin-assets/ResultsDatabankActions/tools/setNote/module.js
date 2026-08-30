angular.module('app.resultsdatabankactions.tools.setNote', ['sqplugin']).config(function(sqPluginProvider) {

    sqPluginProvider.plugin("ResultsDatabankAction", 200, {
        title: Ltsq("Tools")+":"+Ltsq("Set note"),
        controller: 'SetNoteCtrl',
        class: 'btn btn-normal btn-default',
        id: "databank-action-tools-set-note",
        icon: "fa fa-comment-o",
    });

    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/tools/setNote/setNote.html", 'SetNotePopupCtrl', 'SQUANT');

});