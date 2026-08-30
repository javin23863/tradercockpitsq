angular.module('app.datasource.dukascopy', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin(
        "DataSourceDukascopy",
        10, {
            title: Ltsq('Dukascopy data') + ':',
            class: "dukascopy",
            source: 'data',
            id: "datamanager-datasource-dukascopy-menu"
        }
    );
});