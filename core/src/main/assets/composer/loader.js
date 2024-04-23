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
