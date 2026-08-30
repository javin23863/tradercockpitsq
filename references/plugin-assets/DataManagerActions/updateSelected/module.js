angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionTools', 6, {
        key: 'updateSelected',
        title: Ltsq('Update selected'),
        source: 'data',
        class: 'update-selected',
        id: "datamanager-update-selected",
        group: "data-source",
    });
});