const config = callExport("getConfig");

if (config.composerLogs) {
    ["log", "error", "warn", "info", "debug"].forEach(method => {
        console[method] = (...args) => callExport("log", method, Array.from(args).join(" "));
    })

    console.stacktrace = () => new Error().stack;
    console.info("loader.js loaded");
}

if (config.bypassCameraRollLimit) {
    (module => {
        module.MultiSelectClickHandler = new Proxy(module.MultiSelectClickHandler, {
            construct: function(target, args, newTarget) {
                args[1].selectionLimit = 9999999;
                return Reflect.construct(target, args, newTarget);
            },
        });
    })(require('memories_ui/src/clickhandlers/MultiSelectClickHandler'))
}

(module => {
    function onComponentPreRender(component, viewModel) {
        const componentName = component.constructor.name;

        if (componentName == "ProfileIdentityView" && config.showFirstCreatedUsername) {
            let userInfo = callExport("getFriendInfoByUsername", viewModel.username);

            if (userInfo) {
                let userInfoJson = JSON.parse(userInfo);
                let firstCreatedUsername = userInfoJson.username.split("|")[0];
                if (firstCreatedUsername != viewModel.username) {
                    viewModel.username += " (" + firstCreatedUsername + ")";
                }
            }
        }

        return false
    }
    
    function onComponentPostRender(component, viewModel) {
    }

    module.Component = new Proxy(module.Component, {
        construct: function(target, args, newTarget) {
            let component = Reflect.construct(target, args, newTarget);
            component.onRender = new Proxy(component.onRender, {
                apply: function(target, thisArg, argumentsList) {
                    if (onComponentPreRender(component, thisArg.viewModel || {})) return;
                    let result = Reflect.apply(target, thisArg, argumentsList);
                    onComponentPostRender(component, thisArg.viewModel || {});
                    return result;
                }
            });
            return component;
        }
    })
})(require('composer_core/src/Component'))
