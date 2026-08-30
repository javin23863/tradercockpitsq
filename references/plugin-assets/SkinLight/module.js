//product:any

angular.module('app.skins.light', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'Skin',
        10,
        {
            name: 'Light skin',
            previewImgPath: "../../../plugins/SkinLight/preview.png",
            filePaths: [
                '../../../plugins/SkinLight/light.skin.css'
            ]
        }
    );
    
});