angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBasket', 10, {
        title: Ltsq('Add new'),
        key: 'add',
        source: 'baskets',
        class: 'baskets',
        id: "datamanager-basket-of-stocks-add"
    });
});