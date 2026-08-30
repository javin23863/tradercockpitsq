angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionTools', 5, {
        key: 'updateall',
        title: Ltsq('Update all'),
        source: 'data',
        class: 'updateall',
        id: "datamanager-updateall",
        group: "data-source",
    });
});