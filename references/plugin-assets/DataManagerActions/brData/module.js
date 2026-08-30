angular.module('app.data').config(function (sqPluginProvider) {
        sqPluginProvider.plugin('DataManagerActionTools', 1, {
            key: 'brData',
            title: Ltsq('Find BMF data'),
            source: 'data',
            class: 'datamanager-find-br-data',
            id: "datamanager-find-br-data",
            group: "data-source",
        });
});