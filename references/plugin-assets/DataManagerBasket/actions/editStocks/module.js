angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBasket', 100, {
        title: Ltsq('Edit stocks'),
        key: 'edit-stocks',
        source: 'baskets',
        class: 'baskets',
        id: "datamanager-basket-of-stocks-edit"
    });
});