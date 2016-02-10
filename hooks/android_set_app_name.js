module.exports = function (context) {
    console.log('hook.js>> start');
    var fs = context.requireCordovaModule('fs'),
        path = context.requireCordovaModule('path');

    var platformRoot = path.join(context.opts.projectRoot, 'platforms/android');


    var manifestFile = path.join(platformRoot, 'AndroidManifest.xml');

    if (fs.existsSync(manifestFile)) {

        fs.readFile(manifestFile, 'utf8', function (err, data) {
            if (err) {
                console.log('hook.js>> Error' + err);
                throw new Error('Unable to find AndroidManifest.xml: ' + err);

            }

            var appClass = 'com.aaw.beaconsmanager.MainApplication';

            if (data.indexOf(appClass) == -1) {

                var result = data.replace(/<application/g, '<application android:name="' + appClass + '"');

                fs.writeFile(manifestFile, result, 'utf8', function (err) {
                    if (err) {
                        console.log('hook.js>> Error' + err);
                        throw new Error('Unable to write into AndroidManifest.xml: ' + err);
                    }
                })
            }
        });
    }

    console.log('hook.js>> end');
};