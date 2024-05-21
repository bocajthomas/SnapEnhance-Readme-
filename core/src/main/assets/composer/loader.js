const config = callExport("getConfig");

if (config.composerLogs) {
    ["log", "error", "warn", "info", "debug"].forEach(method => {
        console[method] = (...args) => callExport("log", method, Array.from(args).join(" "));
    })

    console.stacktrace = () => new Error().stack;
    console.info("loader.js loaded");
}

// Composer imports

const jsx = require('composer_core/src/JSX').jsx;
const assetCatalog = require("composer_core/src/AssetCatalog")
const style = require("composer_core/src/Style");
const colors = require("coreui/src/styles/semanticColors");

function dumpObject(obj, indent = 0) {
    if (typeof obj !== "object") return console.log(obj);
    let prefix = ""
    for (let i = 0; i < indent; i++) {
        prefix += "    ";
    }
    for (let key of Object.keys(obj)) {
        try {
            console.log(prefix, key, typeof obj[key], obj[key]);
            if (key == "renderer") continue
            if (typeof obj[key] === "object" && indent < 10) dumpObject(obj[key], indent + 1);
        } catch (e) {
        }
    }
}

function proxyProperty(module, functionName, handler) {
    if (!module || !module[functionName]) {
        console.warn("Function not found", functionName);
        return;
    }
    module[functionName] = new Proxy(module[functionName], {
        apply: (a, b, c) => handler(a, b, c),
        construct: (a, b, c) => handler(a, b, c)
    });
}

function interceptComponent(moduleName, className, functions) {
    proxyProperty(require(moduleName), className, (target, args, newTarget) => {
        let initProxy = functions["<init>"]
        let component;

        if (initProxy) {
            initProxy(args, (newArgs) => {
                component = Reflect.construct(target, newArgs || args, newTarget);
            });
        } else {
            component = Reflect.construct(target, args, newTarget);
        }

        for (let funcName of Object.keys(functions)) {
            if (funcName == "<init>" || !component[funcName]) continue
            proxyProperty(component, funcName, (target, thisArg, argumentsList) => {
                let result;
                try {
                    functions[funcName](component, argumentsList, (newArgs) => {
                        result = Reflect.apply(target, thisArg, newArgs || argumentsList);
                    });
                } catch (e) {
                    console.error("Error in", funcName, e);
                }
                return result;
            });
        }

        return component;
    })
}

if (config.bypassCameraRollLimit) {
    interceptComponent(
        'memories_ui/src/clickhandlers/MultiSelectClickHandler',
        'MultiSelectClickHandler',
        {
            "<init>": (args, superCall) => {
                args[1].selectionLimit = 9999999;
                superCall();
            }
        }
    )
}

if (config.operaDownloadButton) {
    const downloadIcon = assetCatalog.loadCatalog("share_sheet/res").downloadIcon

    interceptComponent(
        'context_chrome_header/src/ChromeHeaderRenderer',
        'ChromeHeaderRenderer',
        {
            onRenderBaseHeader: (component, args, render) => {
                render()
                jsx.beginRender(jsx.makeNodePrototype("image"))
                jsx.setAttributeStyle("style", new style.Style({
                    height: 32,
                    marginTop: 4,
                    marginLeft: 8,
                    marginRight: 12,
                    objectFit: "contain",
                    tint: "white"
                }))
                jsx.setAttribute("src", downloadIcon)
                jsx.setAttributeFunction("onTap", () => callExport("downloadLastOperaMedia", false))
                jsx.setAttributeFunction("onLongPress", () => callExport("downloadLastOperaMedia", true))
                jsx.endRender()
            }
        }
    )
}

if (config.showFirstCreatedUsername) {
    interceptComponent(
        'common_profile/src/identity/ProfileIdentityView',
        'ProfileIdentityView',
        {
            onRender: (component, _, render) => {
                if (component.viewModel) {
                    let userInfo = callExport("getFriendInfoByUsername", component.viewModel.username);
                    if (userInfo) {
                        let userInfoJson = JSON.parse(userInfo);
                        let firstCreatedUsername = userInfoJson.username.split("|")[0];
                        if (firstCreatedUsername != component.viewModel.username) {
                            component.viewModel.username += " (" + firstCreatedUsername + ")";
                        }
                    }
                }
                render();
            }
        }
    )
}
