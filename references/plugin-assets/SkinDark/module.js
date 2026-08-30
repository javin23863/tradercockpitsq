//product:any

angular.module('app.skins.dark', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'Skin',
        20,
        {
            name: 'Dark skin',
            previewImgPath: "../../../plugins/SkinDark/preview.png",
            filePaths: [
                '../../../plugins/SkinDark/dark.skin.css'
            ]
        }
    );
    
});