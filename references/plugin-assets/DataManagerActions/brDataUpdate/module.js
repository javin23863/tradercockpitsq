angular.module('app.data').config(function (sqPluginProvider) {
        sqPluginProvider.plugin('DataManagerActionTools', 2, {
            key: 'brDataUpdate',
            title: Ltsq('Update BMF data'),
            source: 'data',
            class: 'datamanager-update-br-data',
            id: "datamanager-update-br-data",
            group: "data-source",
        });
});