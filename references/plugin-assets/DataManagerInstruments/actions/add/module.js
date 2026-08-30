angular.module('app.instruments').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionInstrument', 10, {
        key: 'add',
        title: Ltsq('Add instrument'),
        source: 'instruments',
        class: 'add',
        id: "datamanager-instruments-add"
    });
});