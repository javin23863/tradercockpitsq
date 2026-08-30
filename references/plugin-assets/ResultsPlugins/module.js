angular.module('app.resultstabs.resultsplugins', ['sqplugin']).run(function(sqPlugin, ResultsPluginsService) {
    // Dynamic loading of user ResultsPlugins from /user/extend/ResultsPlugins
    // These plugins will be registered as normal ResultsTab plugins
    loadUserPlugins(sqPlugin, ResultsPluginsService);
});

function loadUserPlugins(sqPlugin, ResultsPluginsService) {
    console.log("Loading results plugins...");

    ResultsPluginsService.listPlugins(function(response) {
            var userPlugins = response.plugins;
            
            userPlugins.forEach(function(pluginData, index) {
                var pluginName = pluginData.name;
                var url = pluginData.url;
                
                // Skip if name or url is missing
                if (!pluginName || !url) {
                    console.warn("Plugin missing required fields (name or url), skipping:", pluginData);
                    return;
                }
                
                var dataItem = 'custom_plugin_' + pluginName.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9_]/g, '');

                var plugin = {
                    title: pluginName,
                    dataItem: dataItem,
                    templateUrl: '../../../plugins/ResultsPlugins/pluginIframe.html',
                    controller: 'PluginIframeCtrl',
                    pluginName: pluginName, // Passed to $scope.tab
                    url: url, // URL for iframe src
                    hideAfterInit: false,
                    isCustom: true,
                    isDefault: pluginData.isDefault === true,
                    hasMenu: true
                };

                // Add plugin to ResultsTab
                console.log("Adding plugin to ResultsTab:", plugin);
                sqPlugin.addPlugin("ResultsTab", 1000 + index, plugin);
            });
            
            console.log("Dynamically registered " + userPlugins.length + " user plugins:", userPlugins);
    });
}

